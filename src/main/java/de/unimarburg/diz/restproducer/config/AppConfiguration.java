/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(value = {LoaderConfigProperties.class, AppConfigProperties.class})
public class AppConfiguration {
  private static final Logger log = LoggerFactory.getLogger(AppConfiguration.class);
  private final LoaderConfigProperties loaderConfigProperties;

  public LoaderConfigProperties getLoaderConfigProperties() {
    return loaderConfigProperties;
  }

  public AppConfigProperties getAppConfigProperties() {
    return appConfigProperties;
  }

  private final AppConfigProperties appConfigProperties;

  public AppConfiguration(
      LoaderConfigProperties loaderConfigProperties, AppConfigProperties appConfigProperties) {

    this.loaderConfigProperties = loaderConfigProperties;
    this.appConfigProperties = appConfigProperties;
    log.info(this.toString());
  }

  @Override
  public String toString() {
    return "AppConfiguration{"
        + "loaderConfigProperties="
        + loaderConfigProperties
        + ", appConfigProperties="
        + appConfigProperties
        + '}';
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    var cfg = getLoaderConfigProperties();
    if (getLoaderConfigProperties().username() != null
        && !cfg.username().isBlank()
        && cfg.password() != null
        && !cfg.password().isBlank()) {
      builder.basicAuthentication(cfg.username(), cfg.password());
    }
    return new RestTemplate();
  }
}
