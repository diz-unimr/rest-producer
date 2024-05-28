/* GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import de.unimarburg.diz.restproducer.config.AppConfiguration;
import de.unimarburg.diz.restproducer.config.EndpointNodeProperties;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RestLoaderTest {

  @Autowired
  @Qualifier("restTemplateBasicAuth")
  private RestTemplate restTemplate;

  @Autowired AppConfiguration config;

  private MockRestServiceServer mockServer;
  @Autowired private RestLoader restLoader;

  @BeforeEach
  public void init() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void load_withParameter()
      throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
    assertThat(restLoader).isNotNull();

    final String mockedResponse = "mockedResponse";
    mockServer
        .expect(
            ExpectedCount.once(), requestTo(new URI("http://localhost:9090/jobs/23/samples/42")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mockedResponse));

    var result =
        restLoader.load(
            "http://localhost:9090/jobs/{job_id}/samples/{sample_id}",
            Map.of("job_id", "23", "sample_id", "42"));
    mockServer.verify();
    assertThat(result).isEqualTo(mockedResponse);
  }

  @Test
  void load_withOutParameter()
      throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
    assertThat(restLoader).isNotNull();

    final String mockedResponse = "mockedResponse";
    mockServer
        .expect(ExpectedCount.once(), requestTo(new URI("http://localhost:9090/jobs")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mockedResponse));

    var result = restLoader.load("http://localhost:9090/jobs", new HashMap<>());
    mockServer.verify();
    assertThat(result).isEqualTo(mockedResponse);
  }

  private EndpointNodeProperties getEndpointNodeProperties(
      String nextNodeRefProperty, String endPointAddress, String extractionTarget) {
    return new EndpointNodeProperties(
        "testFindParam",
        endPointAddress,
        "child",
        "idProp",
        nextNodeRefProperty,
        "http://localhost/nextSibling",
        null,
        null,
        null,
        null,
        null,
        extractionTarget,
        null);
  }
}
