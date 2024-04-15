/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import de.unimarburg.diz.restproducer.config.LoaderConfigProperties;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Service
public class RestLoader implements ILoader {
  private static final Logger log = LoggerFactory.getLogger(RestLoader.class);
  protected HttpHeaders basicAuthHeader = null;
  LoaderConfigProperties config;
  RestTemplate restTemplate;

  @Autowired
  public RestLoader(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  @NonNull
  public String load(@NonNull String endpoint, @NonNull Map<String, String> parameter)
      throws MalformedURLException, UnsupportedEncodingException {

    // todo: retry
    /* var queryParameter =
            getQueryParameter(endpoint.getNodeData().endpointAddress()), null);
    */
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

    // var paramsForNextEndpoint=
    // LoaderUtil.getNextNodePropValue(endpoint.getNodeData().nextNodeReference(),result.getBody());
    // parameter.put(endpoint.getKey(),paramsForNextEndpoint);
    return Objects.requireNonNull(result.getBody());
  }

  public static HashSet<String> getQueryParameter(String url) throws UnsupportedEncodingException {
    var query_pairs = new HashSet<String>();

    String[] pairs = url.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf('=');
      query_pairs.add(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8));
    }
    return query_pairs;
  }
}
