/* GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Endpoint data structure may be arranged as a n-array-tree.
 *
 * <p>each not may have a reference to next sibling and a reference to a child node.
 *
 * @param endpointName internal reference name for this endpoint - can be used at {@link
 *     #nextSiblingEndpoint} or {@link #nextChildEndpointName}
 * @param endpointAddress request target
 * @param nextChildEndpointName internal reference name ({@link #endpointName}) for next child node
 *     (optional)
 * @param idProperty identifier property at request result
 * @param nextNodeReference result property where identifier for underlying requests may be found
 *     (optional)
 * @param nextSiblingEndpoint internal reference name ({@link #endpointName}) for endpoint at same
 *     level - can be processed parallel to current entry (optional)
 * @param pagination <c>true</c> if pagination parameter should be used (optional)
 * @param pageParamName request parameter name for page - e.g. 'page' (optional)
 * @param pageSizeParamName request parameter name for page size - e.g. 'page_size' (optional)
 * @param pageSizeValue number of record per page (optional default value is 20)
 * @param pageStartValue page number to start (optional: default value is 1)
 * @param extractionTarget result property content is used to produce kafka messages, if value is
 *     missing endpoint used to traverse in deeper hierarchy level
 */
@ConfigurationProperties("app.loader.endpoints")
public record EndpointNodeProperties(
    @NonNull String endpointName,
    @NonNull String endpointAddress,
    @Nullable String nextChildEndpointName,
    @NonNull String idProperty,
    @Nullable String nextNodeReference,
    @Nullable String nextSiblingEndpoint,
    @Nullable Boolean pagination,
    @Nullable String pageParamName,
    @Nullable String pageSizeParamName,
    @Nullable Integer pageSizeValue,
    @Nullable Integer pageStartValue,
    @Nullable String extractionTarget,
    @Nullable CustomKeyProperties customKey) {}
