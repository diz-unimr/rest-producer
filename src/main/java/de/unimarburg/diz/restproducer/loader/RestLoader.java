/* GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Service
public class RestLoader implements ILoader {
  private static final Logger log = LoggerFactory.getLogger(RestLoader.class);

  RestTemplate restTemplate;

  @Autowired
  public RestLoader(@Qualifier("restTemplateBasicAuth") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  @NonNull
  public String load(@NonNull String endpoint, @NonNull Map<String, String> parameter)
      throws MalformedURLException, UnsupportedEncodingException {

    // todo: retry
    var result =
        RestClient.builder(restTemplate)
            .build()
            .get()
            .uri(endpoint, parameter)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String.class);

    if (parameter.isEmpty() && !StringUtils.hasText(result.getBody())) {
      log.warn(
          "check your configuration rest call got no result and you have provided no parameters.");
    }

    return Objects.requireNonNull(result.getBody());
  }
}
