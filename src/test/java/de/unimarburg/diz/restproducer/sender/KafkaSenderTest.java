/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.unimarburg.diz.restproducer.config.KafkaProducerConfig;
import de.unimarburg.diz.restproducer.data.Message;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:9092"})
@Profile("test")
class KafkaSenderTest {

  @Autowired KafkaProducerConfig kafkaProducerConfig;

  KafkaSender kafkaSender;

  @Mock KafkaTemplate<String, String> kafkaTemplate;

  @Mock SendResult<String, String> successful;

  @BeforeEach
  void setUpBeforeClass() {
    MockitoAnnotations.openMocks(this);
    kafkaSender = new KafkaSender(kafkaTemplate);
    when(successful.getRecordMetadata()).thenReturn(Mockito.mock(RecordMetadata.class));
    when(successful.getRecordMetadata().offset()).thenReturn(42L);
    when(kafkaTemplate.send(any(ProducerRecord.class)))
        .thenReturn(CompletableFuture.completedFuture(successful));
  }

  @Test
  void send_successful() {
    assertThat(kafkaSender.send(new Message("test", "12", "foo"))).isTrue();
  }

  @Test
  void send_error() {

    when(kafkaTemplate.send(any(ProducerRecord.class)))
        .thenReturn(CompletableFuture.completedFuture(new Exception("fail!!")));

    assertThat(kafkaSender.send(new Message("test", "12", "foo"))).isFalse();
  }
}
