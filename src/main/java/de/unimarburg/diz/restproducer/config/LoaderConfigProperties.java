/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.loader")
public record LoaderConfigProperties(
    String username, String password, List<EndpointNodeProperties> endpoints) {

  @Override
  public String toString() {
    var passReplaced = (password == null || password.isBlank()) ? "" : "<***secret***>";
    return "LoaderConfigProperties{"
        + "username='"
        + username
        + '\''
        + ", password='"
        + passReplaced
        + '\''
        + ", endpoints="
        + endpoints
        + '}';
  }
}
