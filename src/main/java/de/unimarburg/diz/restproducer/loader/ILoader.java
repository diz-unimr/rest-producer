/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import java.util.Map;
import org.springframework.lang.NonNull;

public interface ILoader {

  @NonNull
  String load(String endpointUri, Map<String, String> parameter);
}
