/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.manager;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import de.unimarburg.diz.restproducer.config.AppConfiguration;
import de.unimarburg.diz.restproducer.config.EndpointNode;
import de.unimarburg.diz.restproducer.data.Message;
import de.unimarburg.diz.restproducer.loader.LoaderUtil;
import de.unimarburg.diz.restproducer.loader.RestLoader;
import de.unimarburg.diz.restproducer.sender.KafkaSender;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EtlManager {

  private final RestLoader loader;
  private final KafkaSender sender;
  private final AppConfiguration config;
  private static final Logger log = LoggerFactory.getLogger(EtlManager.class);
  protected Configuration suppressExceptionConfiguration =
      Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);

  @Autowired
  public EtlManager(RestLoader loader, KafkaSender sender, AppConfiguration config) {
    this.loader = loader;
    this.sender = sender;
    this.config = config;
  }

  /**
   * // todo store offset // todo scheduled execution
   *
   * @return number of produced messages
   */
  public long execute() {

    /*
      1. prep config level order
      2. begin at root and descend
      3. call all current level endpoints and produce data into target topic
    */

    // 1
    final var levelOrderedElements = getEndpointNodes();

    // fixme: we need also a global parameter map to supply hierarchy calls and array iterations
    /*
    key: endpoint name
    value: List_1 length is number of parent parameter values
          List_2 length is number of parameter needed for current endpoint call
          Pair <name,param_value>
     */
    var globalParameterMap = new HashMap<String, List<List<StringPair>>>();

    Stream<Message> messages =
        levelOrderedElements.stream()
            .map(
                endpointNode -> {
                  var parameterListForCurrentEndpoint =
                      globalParameterMap.getOrDefault(endpointNode.getKey(), new ArrayList<>());

                  final String endpointAddress = endpointNode.getNodeData().endpointAddress();

                  // THIS PARAMETER DEFINED AT URL
                  // var currentEndpointVariables = LoaderUtil.getVariableNames(endpointAddress);
                  var noParameterStored = parameterListForCurrentEndpoint.isEmpty();
                  // TODO: CHECK: noParameterStored is true  && currentEndpointVariables not empty
                  // -> error!!!

                  Stream<String> messagesOfEndpoint;
                  if (noParameterStored) {
                    // call endpoint without parameter (usually root access point)
                    final String endpointDataReceived =
                        callEndpoint(endpointAddress, new HashMap<>());
                    messagesOfEndpoint = Stream.of(endpointDataReceived);
                    addParamsForChildAndSiblings(
                        endpointNode, globalParameterMap, new ArrayList<>(), endpointDataReceived);

                  } else {
                    // iterate over call list
                    // extract parameter
                    messagesOfEndpoint =
                        parameterListForCurrentEndpoint.stream()
                            .map(
                                endpointParameter -> {
                                  var singleCallParams =
                                      endpointParameter.stream()
                                          .collect(
                                              Collectors.toMap(
                                                  StringPair::value1, StringPair::value2));

                                  final String calledEndpoint =
                                      callEndpoint(endpointAddress, singleCallParams);

                                  addParamsForChildAndSiblings(
                                      endpointNode,
                                      globalParameterMap,
                                      endpointParameter,
                                      calledEndpoint);

                                  return calledEndpoint;
                                });
                  }

                  return messagesOfEndpoint
                      .filter(Objects::nonNull)
                      .map(
                          rdata ->
                              extractMessages(
                                  rdata,
                                  endpointNode.getNodeData().extractionTarget(),
                                  endpointNode.getNodeData().idProperty(),
                                  endpointNode.getKey()))
                      .filter(Objects::nonNull)
                      .flatMap(Collection::stream)
                      .toList();
                })
            .flatMap(Collection::stream);
    var tempMssaes = messages.toList();
    tempMssaes.forEach(
        m -> log.debug("sending message id '{}' - content '{}',", m.key(), m.payload()));
    var sendCount =
        tempMssaes.stream()
            .filter(Objects::nonNull)
            .peek(m -> log.debug("sending message id '{}'", m.key()))
            .map(sender::send)
            .filter(a -> a)
            .count();
    log.info("total messages sent: {}", sendCount);
    return sendCount;
  }

  /**
   * Add parameter values for node children and siblings
   *
   * @param endpointNode current processed node
   * @param globalParameterMap global parameter store
   * @param endpointParameter current parameter used to call endpoint
   * @param dataJson data returned by current endpoint
   */
  protected void addParamsForChildAndSiblings(
      EndpointNode endpointNode,
      HashMap<String, List<List<StringPair>>> globalParameterMap,
      List<StringPair> endpointParameter,
      String dataJson) {

    final boolean isNextNodeDefined = endpointNode.getNodeData().nextNodeReference() != null;
    if (isNextNodeDefined) {
      final String convertedPathToVariableName =
          LoaderUtil.convertPathToVariableName(endpointNode.getNodeData().nextNodeReference());

      var childEndpointParamValues =
          LoaderUtil.getNextNodePropValue(endpointNode.getNodeData().nextNodeReference(), dataJson);

      addParamsForEndpoint(
          endpointNode.getNodeData().nextChildEndpointName(),
          convertedPathToVariableName,
          childEndpointParamValues,
          endpointParameter,
          globalParameterMap);
      if (endpointNode.getFirstChild() != null) {
        addParamsForSibling(
            endpointNode.getFirstChild().getNextSibling(),
            convertedPathToVariableName,
            childEndpointParamValues,
            endpointParameter,
            globalParameterMap);
      }
    }
  }

  /**
   * @param endpointName endpoint for which we want to store parameter
   * @param targetParameterName converted parameter reference {@link
   *     LoaderUtil#convertPathToVariableName(String)}
   * @param values create entries for each value (get from {@link
   *     LoaderUtil#getNextNodePropValue(String, String)})
   * @param previousEndpointParameter parameter and their values
   * @param globalParameterMap global parameter store
   */
  protected void addParamsForEndpoint(
      String endpointName,
      String targetParameterName,
      Collection<String> values,
      List<StringPair> previousEndpointParameter,
      HashMap<String, List<List<StringPair>>> globalParameterMap) {

    if (!StringUtils.hasText(endpointName)) {
      return;
    }
    // init map - even if we have no values
    var parameterMap = globalParameterMap.getOrDefault(endpointName, new ArrayList<>());
    globalParameterMap.putIfAbsent(endpointName, parameterMap);

    if (!StringUtils.hasText(targetParameterName) || values.isEmpty()) {
      return;
    }

    values.forEach(
        paramValue -> {
          final ArrayList<StringPair> nameAndValue = new ArrayList<>();
          parameterMap.add(nameAndValue);

          // param from endpoint data
          if (previousEndpointParameter != null) {
            // may be null
            nameAndValue.addAll(previousEndpointParameter);
          }
          nameAndValue.add(new StringPair(targetParameterName, paramValue));
        });
  }

  /**
   * Add currently extracted parameter values for all siblings (travers node tree in-level)
   *
   * @param endpoint endpoint for which we want to store parameter
   * @param targetParameterName converted parameter reference {@link
   *     LoaderUtil#convertPathToVariableName(String)}
   * @param values create entries for each value (get from {@link
   *     LoaderUtil#getNextNodePropValue(String, String)})
   * @param previousEndpointParameter parameter and their values
   * @param globalParameterMap global parameter store
   * @implNote recursive call
   */
  protected void addParamsForSibling(
      EndpointNode endpoint,
      String targetParameterName,
      Collection<String> values,
      List<StringPair> previousEndpointParameter,
      HashMap<String, List<List<StringPair>>> globalParameterMap) {
    if (globalParameterMap == null)
      throw new IllegalArgumentException("globalParameterMap must not be null");

    if (endpoint == null) {
      return;
    }

    addParamsForSibling(
        endpoint.getNextSibling(),
        targetParameterName,
        values,
        previousEndpointParameter,
        globalParameterMap);

    addParamsForEndpoint(
        endpoint.getKey(),
        targetParameterName,
        values,
        previousEndpointParameter,
        globalParameterMap);
  }

  private String callEndpoint(String address, Map<String, String> parameter) {

    try {
      return loader.load(address, parameter);
    } catch (MalformedURLException | UnsupportedEncodingException e) {
      // todo: improve error handling
      throw new RuntimeException(e);
    }
  }

  public List<Message> extractMessages(
      @NonNull String loadedJson,
      String extractFromProperty,
      @NonNull String idProperty,
      String targetTopic) {

    if (extractFromProperty == null) {
      return new ArrayList<>();
    }
    final ParseContext parseContext = JsonPath.using(suppressExceptionConfiguration);
    try {

      if (extractFromProperty.equals("*")) {
        try {
          final String property = parseContext.parse(loadedJson).read(idProperty).toString();

          return List.of(new Message(targetTopic, property, loadedJson));
        } catch (com.jayway.jsonpath.PathNotFoundException pnfe) {
          log.warn("property %s is missing in json".formatted(idProperty));
          return null;
        }
      }

      return parseContext
          .parse(loadedJson)
          .map(
              extractFromProperty,
              (currentValue, configuration) -> {
                final String messageKey =
                    parseContext.parse(currentValue).read(idProperty).toString();
                return new Message(targetTopic, messageKey, currentValue.toString());
              })
          .read(extractFromProperty);
    } catch (com.jayway.jsonpath.PathNotFoundException pathNotFoundException) {
      log.error("missing property in path");
      return null;
    }
  }

  private List<EndpointNode> getEndpointNodes() {
    var tree = LoaderUtil.getNArrayTree(config.getLoaderConfigProperties());
    return LoaderUtil.getAllNodesAsList(tree).reversed();
  }

  // fast and simple - no need for additional libs
  public record StringPair(String value1, String value2) {}
}
