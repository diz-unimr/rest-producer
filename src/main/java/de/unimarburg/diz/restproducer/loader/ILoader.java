/* GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Map;
import org.springframework.lang.NonNull;

public interface ILoader {

  @NonNull
  String load(@NonNull String endpoint, @NonNull Map<String, String> parameter)
      throws MalformedURLException, UnsupportedEncodingException;
}
