/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@ConfigurationProperties("custom-key")
public record CustomKeyProperties(
    @NonNull String name, @NonNull String path, @Nullable String regEx) {}
