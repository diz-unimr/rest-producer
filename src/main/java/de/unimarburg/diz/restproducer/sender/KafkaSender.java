/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.sender;

import de.unimarburg.diz.restproducer.data.Message;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaSender {

  @Autowired
  public KafkaSender(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  private final KafkaTemplate<String, String> kafkaTemplate;
  protected final Logger log = LoggerFactory.getLogger(KafkaSender.class);

  public boolean send(Message message) {
    final ProducerRecord<String, String> producerRecord =
        new ProducerRecord<>(message.topic(), message.key(), message.payload());

    var sendResultFuture = kafkaTemplate.send(producerRecord);
    AtomicBoolean wasSendSuccessful = new AtomicBoolean(false);
    sendResultFuture.whenComplete(
        (result, e) -> {
          if (result != null) {
            log.debug(
                "Sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
            wasSendSuccessful.set(true);
          } else {

            log.error("Unable to send message=[{}] due to : {}", message, e.getMessage());
            wasSendSuccessful.set(false);
          }
        });

    return wasSendSuccessful.get();
  }
}
