/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.sender;

import de.unimarburg.diz.restproducer.config.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KafkaSender {

  @Autowired
  public KafkaSender(AppConfiguration appConfiguration) {}

  public boolean send(String topic, String message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
