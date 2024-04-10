/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import de.unimarburg.diz.restproducer.config.LoaderConfigProperties;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Service
public class RestLoader implements ILoader {

  protected HttpHeaders basicAuthHeader = null;
  LoaderConfigProperties config;
  RestTemplate restTemplate;

  @Autowired
  public RestLoader(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  @NonNull
  public String load(String endpointUri, Map<String, String> parameter) {
    // todo: retry
    var result =
        RestClient.builder(restTemplate)
            .build()
            .get()
            .uri(endpointUri, parameter)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String.class);
    return Objects.requireNonNull(result.getBody());
  }
}
