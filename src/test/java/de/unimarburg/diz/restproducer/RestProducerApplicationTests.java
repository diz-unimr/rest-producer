/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer;

import de.unimarburg.diz.restproducer.config.KafkaProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = KafkaProducerConfig.class)
class RestProducerApplicationTests {

  @Test
  void contextLoads() {}
}
