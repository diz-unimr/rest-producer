/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.unimarburg.diz.restproducer.config.AppConfiguration;
import de.unimarburg.diz.restproducer.config.KafkaProducerConfig;
import java.util.Collection;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

class LoaderUtilTest {

  @ParameterizedTest
  @CsvSource({
    "jobs[*].job_id,jobs____job_id",
    "sample.id,sample_id",
    "$..book[?(@.price <= $['expensive'])],___book_____price_______expensive____"
  })
  void convertPathToVariableName(String value, String expected) {

    assertThat(LoaderUtil.convertPathToVariableName(value)).isEqualTo(expected);
  }

  @Test
  void getVariableNames() {
    var result =
        LoaderUtil.getVariableNames(
            "https://target/jobs/{jobs____job_id}/samples/{samples____id}/results/cnv-filtered-list");
    assertThat(result).containsExactly("{jobs____job_id}", "{samples____id}");
  }

  @Nested
  @SpringBootTest(
      classes = {
        KafkaProducerConfig.class,
        AppConfiguration.class,
        RestTemplate.class,
        RestTemplateAutoConfiguration.class
      })
  @TestPropertySource(
      properties = {
        "app.loader.username=test",
        "app.loader.password=test",
        "app.loader.endpoints[0].endpoint-name=job",
        "app.loader.endpoints[1].endpoint-name=job"
      })
  class duplicateEndpointNameTest {
    @Autowired AppConfiguration config;

    @Test
    void getNArrayTree_Invalid() {
      assertThrows(
          IllegalArgumentException.class,
          () -> LoaderUtil.getNArrayTree(config.getLoaderConfigProperties()));
    }
  }

  @Nested
  @SpringBootTest(
      classes = {AppConfiguration.class, RestTemplate.class, RestTemplateAutoConfiguration.class})
  @ActiveProfiles("test")
  class validBehaviour {
    @Autowired AppConfiguration config;

    @Test
    void getNArrayTree() {
      var nArrayTree = LoaderUtil.getNArrayTree(config.getLoaderConfigProperties());

      assertThat(nArrayTree.getFirstChild()).isNotNull();
      assertThat(nArrayTree.getNextSibling()).isNull();
    }

    @Test
    void getAllNodes() {
      var nArrayTree = LoaderUtil.getNArrayTree(config.getLoaderConfigProperties());
      var tree = LoaderUtil.getAllNodesAsList(nArrayTree);
      assertThat(tree.size()).isEqualTo(4);
    }

    @Test
    void findNestedArrayParameter() throws JSONException {

      // sample is array
      final String nextNodeRefProperty = "samples[*].id";

      Collection<String> values =
          LoaderUtil.getNextNodePropValue(nextNodeRefProperty, DummyValues.jobExample1);

      assertThat(values).containsAll(List.of("111", "112"));
      assertThat(values).hasSize(2);
    }

    @Test
    void findArrayParameter() throws JSONException {

      // sample is array
      final String nextNodeRefProperty = "jobs[*].job_id";

      Collection<String> values =
          LoaderUtil.getNextNodePropValue(nextNodeRefProperty, DummyValues.jobsExample1);

      assertThat(values).containsAll(List.of("11", "12"));
      assertThat(values).hasSize(2);
    }
  }
}
