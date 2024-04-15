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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    var globalParameterMap = new HashMap<String, HashMap<String, Collection<String>>>();

    var messageStream =
        levelOrderedElements.stream()
            .map(
                endpointNode -> {

                  // inti global entry if needed
                  HashMap<String, Collection<String>> endpointEntryMap =
                      globalParameterMap.getOrDefault(endpointNode.getKey(), new HashMap<>());
                  if (!globalParameterMap.containsKey(endpointNode.getKey())) {
                    globalParameterMap.put(endpointNode.getKey(), endpointEntryMap);
                  }
                  var singleCallParams = new HashMap<String, String>();
                  var currentEndpointVariables =
                      LoaderUtil.getVariableNames(endpointNode.getNodeData().endpointAddress());
                  if (!currentEndpointVariables.containsAll(endpointEntryMap.keySet())) {
                    // may be need to discard not needed parameters
                    log.error("variable missing!");

                  } else {
                    // fixme: get all parameter needed for call
                    // iterate all call per endpoint
                    // currentEndpointVariables.forEach(p->singleCallParams.put(p,));

                  }
                  try {
                    // all relevant parameter from current and upper level nodes

                    final String loadedJson =
                        loader.load(endpointNode.getNodeData().endpointAddress(), singleCallParams);

                    // todo: populate global map if children or siblings are defined
                    final boolean isChildNodeDefined =
                        StringUtils.hasText(endpointNode.getNodeData().nextChildEndpointName());
                    if (isChildNodeDefined) {
                      var nextRefValues =
                          LoaderUtil.getNextNodePropValue(
                              endpointNode.getNodeData().nextNodeReference(), loadedJson);

                      // variableName,list of values found (can be one or multiple in case of an
                      // array)
                      final String convertedPathToVariableName =
                          LoaderUtil.convertPathToVariableName(
                              endpointNode.getNodeData().nextNodeReference());

                      if (!globalParameterMap.containsKey(
                          endpointNode.getNodeData().nextChildEndpointName())) {
                        // create entry if missing
                        globalParameterMap.put(
                            endpointNode.getNodeData().nextChildEndpointName(), new HashMap<>());
                      }
                      var childEndpointEntry =
                          globalParameterMap.get(
                              endpointNode.getNodeData().nextChildEndpointName());

                      // these will be needed as query parameter at next level
                      childEndpointEntry.put(convertedPathToVariableName, nextRefValues);
                      // fixme: take current called parameter and add new identified

                    }
                    // this has to be send to kafka if it is indented to be stored

                    if (StringUtils.hasText(endpointNode.getNodeData().extractionTarget())) {
                      return extractMessages(
                          loadedJson,
                          endpointNode.getNodeData().extractionTarget(),
                          endpointNode.getNodeData().idProperty(),
                          endpointNode.getKey());
                    }
                    // no storage therefore skip value
                    return null;
                  } catch (MalformedURLException | UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                  }
                });

    var sendCount =
        messageStream
            .flatMap(Collection::stream)
            .peek(m -> log.debug("sending message id '{}'...,", m.key()))
            .map(sender::send)
            .peek(result -> log.debug("message successfully sent: '{}'", result))
            .count();
    log.info("total messages sent: {}", sendCount);
    return sendCount;
  }

  public Collection<Message> extractMessages(
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
}
