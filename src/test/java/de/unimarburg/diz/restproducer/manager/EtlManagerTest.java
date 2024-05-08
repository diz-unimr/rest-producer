/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.unimarburg.diz.restproducer.config.AppConfiguration;
import de.unimarburg.diz.restproducer.config.KafkaProducerConfig;
import de.unimarburg.diz.restproducer.data.Message;
import de.unimarburg.diz.restproducer.loader.DummyValues;
import de.unimarburg.diz.restproducer.loader.RestLoader;
import de.unimarburg.diz.restproducer.sender.KafkaSender;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

class EtlManagerTest {

  @Nested
  @ExtendWith(SpringExtension.class)
  @SpringBootTest(
      classes = {
        KafkaProducerConfig.class,
        AppConfiguration.class,
        EtlManager.class,
        RestTemplate.class,
        RestTemplateAutoConfiguration.class,
        RestLoader.class,
        KafkaSender.class
      })
  class basicProcessingTest {

    @Autowired AppConfiguration appConfiguration;
    @Autowired EtlManager etlManager;

    @Test
    void extractMessages() {

      assertThat(etlManager).isNotNull();
      var messages =
          etlManager.extractMessages(DummyValues.jobsExample1, "jobs[*]", "id", "testTopic");

      assertThat(messages).isNotNull();
      assertThat(messages).hasSize(2);
    }
  }

  @SpringBootTest
  @ActiveProfiles("one-level-test")
  @ExtendWith(SpringExtension.class)
  @Nested
  class EtlManagerOneLevelTest {

    @Autowired AppConfiguration appConfiguration;

    @Mock RestLoader restloader;

    @Mock KafkaSender kafkaSender;

    EtlManager etlManager;

    @BeforeEach
    void setUpBeforeClass() throws Exception {
      MockitoAnnotations.openMocks(this);

      when(restloader.load(anyString(), anyMap())).thenReturn(DummyValues.jobsExample1);
      when(kafkaSender.send(any(Message.class))).thenReturn(true);
      etlManager = new EtlManager(restloader, kafkaSender, appConfiguration);
    }

    @Test
    void executeOneResult() {

      assertThat(etlManager.execute()).as("two messages should be created and send").isEqualTo(2);

      // two messages -> two send calls
      verify(kafkaSender, times(2)).send(any(Message.class));
    }
  }

  @SpringBootTest
  @ActiveProfiles("two-level-test")
  @ExtendWith(SpringExtension.class)
  @Nested
  class EtlManagerTwoLevelTest {

    @Autowired AppConfiguration appConfiguration;

    @Mock RestLoader restloader;

    @Mock KafkaSender kafkaSender;

    EtlManager etlManager;

    @BeforeEach
    void setUpBeforeClass() throws Exception {
      MockitoAnnotations.openMocks(this);

      etlManager = new EtlManager(restloader, kafkaSender, appConfiguration);

      when(restloader.load(ArgumentMatchers.startsWith("https://target/jobs/"), anyMap()))
          .thenReturn(DummyValues.jobsExample1);

      var job1Map = Map.of("jobs____job_id", "11");
      when(restloader.load(
              ArgumentMatchers.startsWith("https://target/job"),
              argThat(new MapContainsAtLeastEntriesMatcher(job1Map))))
          .thenReturn(DummyValues.jobExample1);

      var job2Map = Map.of("jobs____job_id", "12");
      when(restloader.load(
              ArgumentMatchers.startsWith("https://target/job"),
              argThat(new MapContainsAtLeastEntriesMatcher(job2Map))))
          .thenReturn(DummyValues.jobExample2);

      when(kafkaSender.send(any(Message.class))).thenReturn(true);
    }

    @Test
    void execute2Level() {
      assertThat(etlManager).isNotNull();
      assertThat(etlManager.execute())
          .as(
              "2 entries at jobs + 2 sample at first job, second job has no samples - therefore no additional sample entries.")
          .isEqualTo(4);
      verify(kafkaSender, times(4)).send(any(Message.class));
    }
  }

  @SpringBootTest
  @ActiveProfiles("test")
  @ExtendWith(SpringExtension.class)
  @Nested
  class EtlManagerThreeLevelTest {

    @Autowired AppConfiguration appConfiguration;

    @Mock RestLoader restloader;

    @Mock KafkaSender kafkaSender;

    EtlManager etlManager;

    @BeforeEach
    void setUpBefore() throws Exception {
      MockitoAnnotations.openMocks(this);

      etlManager = new EtlManager(restloader, kafkaSender, appConfiguration);

      // will not be stored
      when(restloader.load(
              ArgumentMatchers.contains("https://target/jobs/?filter_string=COMPLETED_OKAY"),
              anyMap()))
          .thenReturn(DummyValues.jobsExample1);

      // extract one message 11 to job topic
      var job1Map = Map.of("jobs____job_id", "11");
      when(restloader.load(
              ArgumentMatchers.contains("https://target/job/{jobs____job_id}"),
              argThat(new MapContainsAtLeastEntriesMatcher(job1Map))))
          .thenReturn(DummyValues.jobExample1);

      // extract one message id 12 to job topic
      var job2Map = Map.of("jobs____job_id", "12");
      when(restloader.load(
              ArgumentMatchers.contains("https://target/job/{jobs____job_id}"),
              argThat(new MapContainsAtLeastEntriesMatcher(job2Map))))
          .thenReturn(DummyValues.jobExample2);

      var job3Map_A = Map.of("samples____id", "111");
      when(restloader.load(
              ArgumentMatchers.contains(
                  "https://target/jobs/{jobs____job_id}/samples/{samples____id}/results/cnv-filtered-list"),
              argThat(new MapContainsAtLeastEntriesMatcher(job3Map_A))))
          .thenReturn(DummyValues.resultCNVFilteredList);

      var job3Map_B = Map.of("samples____id", "112");
      when(restloader.load(
              ArgumentMatchers.contains(
                  "https://target/jobs/{jobs____job_id}/samples/{samples____id}/results/cnv-filtered-list"),
              argThat(new MapContainsAtLeastEntriesMatcher(job3Map_B))))
          .thenReturn(DummyValues.resultCNVFilteredList2);

      var job3Map_C = Map.of("samples____id", "111");
      when(restloader.load(
              ArgumentMatchers.contains(
                  "https://target/jobs/{jobs____job_id}/samples/{samples____id}/results/variant-filtered-list"),
              argThat(new MapContainsAtLeastEntriesMatcher(job3Map_C))))
          .thenReturn(DummyValues.resultFilteredList1);

      var job3Map_D = Map.of("samples____id", "112");
      when(restloader.load(
              ArgumentMatchers.contains(
                  "https://target/jobs/{jobs____job_id}/samples/{samples____id}/results/variant-filtered-list"),
              argThat(new MapContainsAtLeastEntriesMatcher(job3Map_D))))
          .thenReturn(DummyValues.resultFilteredList2);
      when(kafkaSender.send(any(Message.class))).thenReturn(true);
    }

    @Test
    void execute3Level() {
      assertThat(etlManager).isNotNull();
      // fix
      assertThat(etlManager.execute()).as("2 entries at jobs á 2 samples").isEqualTo(6);
      verify(kafkaSender, times(6)).send(any(Message.class));
    }
  }

  private record MapContainsAtLeastEntriesMatcher(Map<String, String> left)
      implements ArgumentMatcher<Map<String, String>> {

    @Override
    public boolean matches(Map argument) {

      return argument.entrySet().containsAll(left.entrySet());
    }
  }
}
