/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import com.jayway.jsonpath.JsonPath;
import de.unimarburg.diz.restproducer.config.EndpointNode;
import de.unimarburg.diz.restproducer.config.EndpointNodeProperties;
import de.unimarburg.diz.restproducer.config.LoaderConfigProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

public class LoaderUtil {

  private static final Logger log = LoggerFactory.getLogger(LoaderUtil.class);

  public static List<EndpointNode> getAllNodesAsList(EndpointNode root) {
    if (root == null) {
      throw new IllegalArgumentException("tree is invalid - no null elements allowed.");
    }
    var result = new ArrayList<EndpointNode>();

    if (root.getNextSibling() != null) {
      result.addAll(getAllNodesAsList(root.getNextSibling()));
    }
    if (root.getFirstChild() != null) {
      result.addAll(getAllNodesAsList(root.getFirstChild()));
    }

    result.add(root);

    return result;
  }

  /**
   * @param jsonPathExpression filter
   * @param json json as text
   * @return values matched by given json path expression
   */
  public static @NonNull Collection<String> getNextNodePropValue(
      @NonNull String jsonPathExpression, @NonNull String json) {
    if (!StringUtils.hasText(jsonPathExpression))
      throw new IllegalArgumentException("jsonPathExpression must have a value");
    if (!StringUtils.hasText(json)) return new ArrayList<>();

    return JsonPath.parse(json).read(jsonPathExpression);
  }

  public void getSiblings(EndpointNode node, List<EndpointNode> siblings) {
    var result = new LinkedList<EndpointNode>();
    siblings.add(node);
    if (node.getNextSibling() != null) {
      getSiblings(node.getNextSibling(), result);
    }
  }

  /**
   * Build n-array-tree object structure from configuration
   *
   * @param config LoaderConfigProperties
   * @return n-array-tree from configuration
   */
  public static EndpointNode getNArrayTree(LoaderConfigProperties config) {
    return LoaderUtil.getNArrayTree(
        config.endpoints(), config.endpoints().stream().findFirst().orElse(null));
  }

  /**
   * Build n-array-tree object structure from configuration
   *
   * @param orderedEntriesList list of configuration entries
   * @param currentEntry current root element (initial parameter will be the root)
   * @return root of n-array-tree
   * @implNote checks for duplicate entrypoint names
   */
  protected static EndpointNode getNArrayTree(
      List<EndpointNodeProperties> orderedEntriesList, EndpointNodeProperties currentEntry) {
    var checkSet = new HashSet<String>();

    var duplicateCount = getDuplicateEndpointNameCount(orderedEntriesList, checkSet);

    if (duplicateCount == 0) {
      return getNArrayTreeInternal(orderedEntriesList, currentEntry);
    } else {
      throw new IllegalArgumentException("duplicate entrypoint configuration!");
    }
  }

  protected static long getDuplicateEndpointNameCount(
      List<EndpointNodeProperties> orderedEntriesList, HashSet<String> checkSet) {
    return orderedEntriesList.stream()
        .map(
            entry -> {
              var addResult = checkSet.add(entry.endpointName());
              if (addResult) {
                return null;
              } else {
                return entry;
              }
            })
        .filter(Objects::nonNull)
        .peek(
            duplicate ->
                log.error(
                    "Configuration error: duplicate endpoint naming is not allowed! please check entry '%s'"
                        .formatted(duplicate)))
        .count();
  }

  /**
   * Build n-array-tree object structure from configuration
   *
   * @param orderedEntriesList list of configuration entries
   * @param currentEntry current root element (initial parameter will be the root)
   * @return root of n-array-tree
   * @implNote recursion call
   */
  protected static EndpointNode getNArrayTreeInternal(
      List<EndpointNodeProperties> orderedEntriesList, EndpointNodeProperties currentEntry) {
    if (currentEntry == null) {
      throw new IllegalArgumentException("current entry must not be null!");
    }

    var currentNode = new EndpointNode(currentEntry);

    if (StringUtils.hasText(currentEntry.nextSiblingEndpoint())) {
      var nextSiblingElement =
          orderedEntriesList.stream()
              .filter(e -> currentEntry.nextSiblingEndpoint().equals(e.endpointName()))
              .findFirst();
      nextSiblingElement.ifPresentOrElse(
          nextSiblingEntry ->
              currentNode.setNextSibling(
                  getNArrayTreeInternal(orderedEntriesList, nextSiblingEntry)),
          () -> {
            throw new IllegalArgumentException(
                "Endpoint '%s' has invalid sibling name. '%s' is missing - please remove or set an existing entry!"
                    .formatted(currentEntry.endpointName(), currentEntry.nextSiblingEndpoint()));
          });
    }
    if (StringUtils.hasText(currentEntry.nextChildEndpointName())) {
      var nextChildElement =
          orderedEntriesList.stream()
              .filter(e -> currentEntry.nextChildEndpointName().equals(e.endpointName()))
              .findFirst();
      nextChildElement.ifPresentOrElse(
          childProperties ->
              currentNode.setFirstChild(getNArrayTreeInternal(orderedEntriesList, childProperties)),
          () -> {
            throw new IllegalArgumentException(
                "Endpoint '%s' has invalid child name. '%s' is missing - please remove or set an existing entry!"
                    .formatted(currentEntry.endpointName(), currentEntry.nextChildEndpointName()));
          });
    }
    return currentNode;
  }

  /**
   * URL variables may contain only
   *
   * <ul>
   *   <li>Lowercase alphabetical
   *   <li>Numerical
   *   <li>Hyphen
   *   <li>Underscore
   * </ul>
   *
   * therefore we replace other characters with '_'
   *
   * @param path json filter path to a property
   * @return url compatible variable name
   * @implNote if we get variable duplicates, this could be implemented more sophisticated, but for
   *     first version this should be sufficient
   */
  public static String convertPathToVariableName(String path) {
    return path.replaceAll("\\W", "_").toLowerCase();
  }

  public static List<String> getVariableNames(String endpointAddress) {

    var result = new ArrayList<String>();
    var pattern = Pattern.compile("\\{[\\w^\\}]{1,50}\\}");

    var matcher = pattern.matcher(endpointAddress);
    while (matcher.find()) {
      var parameter = matcher.group();
      result.add(parameter);
    }

    return result;
  }
}
