/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.manager;

import com.jayway.jsonpath.JsonPath;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class EtlManager {

  private final RestLoader loader;
  private final KafkaSender sender;
  private final AppConfiguration config;
  private static final Logger log = LoggerFactory.getLogger(EtlManager.class);

  @Autowired
  public EtlManager(RestLoader loader, KafkaSender sender, AppConfiguration config) {
    this.loader = loader;
    this.sender = sender;
    this.config = config;
  }

  /**
   * // todo store offset // todo scheduled execution
   *
   * @return
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
                  var currentEndpointVariables = LoaderUtil.getVariableNames(endpointAddress);
                  var noParameterStored = parameterListForCurrentEndpoint.isEmpty();
                  // TODO: CHECK: noParameterStored is true  && currentEndpointVariables not empty
                  // -> error!!!

                  Stream<String> messagesOfEndpoint;
                  if (noParameterStored) {
                    // call endpoint without parameter (usually root access point)
                    final String endpointDataRecieved =
                        callEndpoint(endpointAddress, new HashMap<>());
                    messagesOfEndpoint = Stream.of(endpointDataRecieved);
                    addCurrentParamsForChild(
                        endpointNode, globalParameterMap, new ArrayList<>(), endpointDataRecieved);

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

                                  addCurrentParamsForChild(
                                      endpointNode,
                                      globalParameterMap,
                                      endpointParameter,
                                      calledEndpoint);

                                  return calledEndpoint;
                                });
                  }

                  var msgForKafka =
                      messagesOfEndpoint
                          .map(
                              rdata ->
                                  extractMessages(
                                      rdata,
                                      endpointNode.getNodeData().extractionTarget(),
                                      endpointNode.getNodeData().idProperty(),
                                      endpointNode.getKey()))
                          .flatMap(Collection::stream)
                          .toList();

                  return msgForKafka;
                })
            .flatMap(Collection::stream);
    var tempMssaes = messages.toList();
    var sendCount =
        tempMssaes.stream()
            .peek(m -> log.debug("sending message id '{}'...,", m.key()))
            .map(sender::send)
            .filter(a -> a.booleanValue())
            .peek(result -> log.debug("message successfully sent: '{}'", result))
            .count();
    log.info("total messages sent: {}", sendCount);
    return sendCount;
  }

  protected void addCurrentParamsForChild(
      EndpointNode endpointNode,
      HashMap<String, List<List<StringPair>>> globalParameterMap,
      List<StringPair> endpointParameter,
      String dataJson) {

    var childEntryMap =
        globalParameterMap.getOrDefault(
            endpointNode.getNodeData().nextChildEndpointName(), new ArrayList<>());
    globalParameterMap.putIfAbsent(
        endpointNode.getNodeData().nextChildEndpointName(), childEntryMap);
    if (endpointNode.getNodeData().nextNodeReference() == null) {
      return;
    }

    final String convertedPathToVariableName =
        LoaderUtil.convertPathToVariableName(endpointNode.getNodeData().nextNodeReference());

    var childEndpointParamValues =
        LoaderUtil.getNextNodePropValue(endpointNode.getNodeData().nextNodeReference(), dataJson);

    childEndpointParamValues.forEach(
        childparam -> {
          final ArrayList<StringPair> oneCallparams = new ArrayList<>();
          childEntryMap.add(oneCallparams);

          // param from endpoint data
          oneCallparams.add(new StringPair(convertedPathToVariableName, childparam));
          oneCallparams.addAll(endpointParameter);
        });
  }

  private String callEndpoint(String address, Map<String, String> parameter) {

    try {
      final String loadedJson = loader.load(address, parameter);

      return loadedJson;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Message> extractMessages(
      @NonNull String loadedJson,
      @NonNull String extractFromProperty,
      @NonNull String idProperty,
      @NonNull String targetTopic) {

    return JsonPath.parse(loadedJson)
        .map(
            extractFromProperty,
            (currentValue, configuration) -> {
              final String messageKey = JsonPath.parse(currentValue).read(idProperty).toString();
              return new Message(targetTopic, messageKey, currentValue.toString());
            })
        .read(extractFromProperty);
  }

  private List<EndpointNode> getEndpointNodes() {
    var tree = LoaderUtil.getNArrayTree(config.getLoaderConfigProperties());
    var levelOrderedElements = LoaderUtil.getAllNodesAsList(tree).reversed();
    return levelOrderedElements;
  }

  // fast and simple - no need for additional libs
  public record StringPair(String value1, String value2) {}
}
