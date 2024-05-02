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
import de.unimarburg.diz.restproducer.data.Message;
import de.unimarburg.diz.restproducer.loader.DummyValues;
import de.unimarburg.diz.restproducer.loader.RestLoader;
import de.unimarburg.diz.restproducer.sender.KafkaSender;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class EtlManagerTest {

  @Nested
  @ExtendWith(SpringExtension.class)
  @SpringBootTest
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
    void execute() {
      assertThat(etlManager).isNotNull();
      assertThat(etlManager.execute()).as("2 entries at jobs á 2 samples").isEqualTo(6);
      verify(kafkaSender, times(6)).send(any(Message.class));
    }
  }

  @SpringBootTest
  @ActiveProfiles("test")
  @Disabled("WIP")
  @ExtendWith(SpringExtension.class)
  @Nested
  class EtlManagerThreeLevelTest {

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

      var job3Map = Map.of("jobs____job_id", "12");
      when(restloader.load(
              ArgumentMatchers.startsWith("https://target/job"),
              argThat(new MapContainsAtLeastEntriesMatcher(job2Map))))
          .thenReturn(DummyValues.jobExample2);

      when(kafkaSender.send(any(Message.class))).thenReturn(true);
    }

    @Test
    void execute() {
      assertThat(etlManager).isNotNull();
      assertThat(etlManager.execute()).as("2 entries at jobs á 2 samples").isEqualTo(6);
      verify(kafkaSender, times(6)).send(any(Message.class));
    }
  }

  private class MapContainsAtLeastEntriesMatcher implements ArgumentMatcher<Map<String, String>> {

    public MapContainsAtLeastEntriesMatcher(Map<String, String> left) {
      this.left = left;
    }

    private Map<String, String> left;

    @Override
    public boolean matches(Map argument) {

      return argument.entrySet().containsAll(left.entrySet());
    }
  }
}
