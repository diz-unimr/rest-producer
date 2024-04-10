/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.config;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class EndpointNode {

  protected EndpointNodeProperties nodeData;

  private EndpointNode nextSibling;
  private EndpointNode firstChild;

  public EndpointNode(EndpointNodeProperties nodeData) {
    if (nodeData == null
        || !StringUtils.hasText(nodeData.endpointName())
        || !StringUtils.hasText(nodeData.endpointAddress()))
      throw new IllegalArgumentException(
          "endpoint data missing - name and address must be provided!");
    this.nodeData = nodeData;
  }

  @NonNull
  public String getKey() {
    return nodeData.endpointName();
  }

  @NonNull
  public EndpointNodeProperties getNodeData() {
    return nodeData;
  }

  @Nullable
  public EndpointNode getNextSibling() {
    return nextSibling;
  }

  @Nullable
  public EndpointNode getFirstChild() {
    return firstChild;
  }

  public void setNextSibling(EndpointNode nextSibling) {
    this.nextSibling = nextSibling;
  }

  public void setFirstChild(EndpointNode firstChild) {
    this.firstChild = firstChild;
  }
}
