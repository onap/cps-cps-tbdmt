/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 Wipro Limited.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.tbdmt.client;

import java.util.Arrays;
import java.util.Map;
import org.onap.cps.tbdmt.exception.CpsClientException;
import org.onap.cps.tbdmt.model.AppConfiguration;
import org.onap.cps.tbdmt.model.CpsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CpsRestClient {

    private static final String NODES_API_PATH = "/anchors/{anchor}/node";

    private static final String QUERY_API_PATH = "/anchors/{anchor}/nodes/query";

    private static final String POST_API_PATH = "/anchors/{anchor}/nodes";

    private static final String LIST_NODE_API_PATH = "/anchors/{anchor}/list-nodes";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppConfiguration appConfiguration;

    /**
     * Fetch node from the CPS using xpath.
     *
     * @param anchor anchor
     * @param xpath xpath query
     * @return result Response string from CPS
     */
    public String fetchNode(final String anchor, final String xpath,
        final String requestType, final Boolean includeDescendants) throws CpsClientException {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        final CpsConfiguration cpsConfiguration = "cpsCore".equals(appConfiguration.getCpsClient())
            ? appConfiguration.getCpsCoreConfiguration() : appConfiguration.getNcmpConfiguration();
        String path = NODES_API_PATH;
        switch (requestType) {
            case "query-cps-path":
                queryParams.add("cps-path", xpath);
                path = QUERY_API_PATH;
                break;
            case "query":
                queryParams.add("xpath", xpath);
                path = QUERY_API_PATH;
                break;
            default:
                queryParams.add("xpath", xpath);
        }
        queryParams.add("include-descendants", includeDescendants.toString());
        final String uri = buildCpsUrl(cpsConfiguration.getUrl(), path, anchor, queryParams);

        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        headers.setBasicAuth(cpsConfiguration.getUsername(), cpsConfiguration.getPassword());
        final HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        } catch (final Exception e) {
            throw new CpsClientException(e.getLocalizedMessage());
        }

        final int statusCode = responseEntity.getStatusCodeValue();

        if (statusCode == 200) {
            return responseEntity.getBody();
        } else {
            throw new CpsClientException(
                String.format("Response code from CPS other than 200: %d", statusCode));
        }
    }

    /**
     * Post data to CPS using xpath.
     *
     * @param anchor anchor
     * @param xpath xpath query
     * @param requestType http request type
     * @param payload request body
     * @return result Response string from CPS
     */
    public String addData(final String anchor, final String xpath, final String requestType,
            final Map<String, Object> payload) throws CpsClientException {

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("xpath", xpath);
        final CpsConfiguration cpsConfiguration = "cpsCore".equals(appConfiguration.getCpsClient())
            ? appConfiguration.getCpsCoreConfiguration() : appConfiguration.getNcmpConfiguration();

        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(cpsConfiguration.getUsername(), cpsConfiguration.getPassword());
        final HttpEntity<String> entity = new HttpEntity<>(new com.google.gson.Gson().toJson(payload), headers);

        String uri = buildCpsUrl(cpsConfiguration.getUrl(), POST_API_PATH, anchor, queryParams);
        try {
            switch (requestType) {
                case "post":
                    uri = buildCpsUrl(cpsConfiguration.getUrl(), POST_API_PATH, anchor, new LinkedMultiValueMap<>());
                    return restTemplate.postForEntity(uri, entity, String.class).getBody();
                case "patch":
                    final HttpComponentsClientHttpRequestFactory requestFactory =
                            new HttpComponentsClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(10000);
                    requestFactory.setReadTimeout(10000);
                    restTemplate.setRequestFactory(requestFactory);
                    return restTemplate.patchForObject(uri, entity, String.class);
                case "post-list-node":
                    uri = buildCpsUrl(cpsConfiguration.getUrl(), LIST_NODE_API_PATH, anchor, queryParams);
                    return restTemplate.postForEntity(uri, entity, String.class).getBody();
                default:
                    return restTemplate.exchange(uri, HttpMethod.PUT, entity, String.class).getBody();
            }
        } catch (final Exception e) {
            throw new CpsClientException(e.getLocalizedMessage());
        }
    }

    /**
     * Delete data from the CPS using xpath.
     *
     * @param anchor anchor
     * @param xpath xpath query
     * @return result Response string from CPS
     */
    public String deleteData(final String anchor, final String xpath,
              final String requestType) throws CpsClientException {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("xpath", xpath);
        final CpsConfiguration cpsConfiguration = "cpsCore".equals(appConfiguration.getCpsClient())
            ? appConfiguration.getCpsCoreConfiguration() : appConfiguration.getNcmpConfiguration();

        String uri = buildCpsUrl(cpsConfiguration.getUrl(), POST_API_PATH, anchor, queryParams);

        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(cpsConfiguration.getUsername(), cpsConfiguration.getPassword());
        final HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = null;
        try {
            if ("delete-list-node".equalsIgnoreCase(requestType)) {
                uri = buildCpsUrl(cpsConfiguration.getUrl(), LIST_NODE_API_PATH, anchor, queryParams);
                responseEntity = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
            } else {
                responseEntity = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
            }
        } catch (final Exception e) {
            throw new CpsClientException(e.getLocalizedMessage());
        }

        final int statusCode = responseEntity.getStatusCodeValue();

        if (statusCode == 204) {
            return "Success";
        } else {
            throw new CpsClientException(
                String.format("Response code from CPS other than 200: %d", statusCode));
        }
    }

    private String buildCpsUrl(final String baseUrl, final String path, final String anchor,
        final MultiValueMap<String, String> queryParams) {

        return UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .path(path)
            .queryParams(queryParams)
            .buildAndExpand(anchor)
            .toUriString();
    }

}
