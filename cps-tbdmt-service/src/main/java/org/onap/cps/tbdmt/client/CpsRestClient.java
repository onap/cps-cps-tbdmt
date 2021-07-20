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
import org.onap.cps.tbdmt.exception.CpsClientException;
import org.onap.cps.tbdmt.model.AppConfiguration;
import org.onap.cps.tbdmt.model.CpsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CpsRestClient {

    private static final String NODES_API_PATH = "/anchors/{anchor}/node";

    private static final String QUERY_API_PATH = "/anchors/{anchor}/nodes/query";

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
        queryParams.add("xpath", xpath);
        queryParams.add("include-descendants", includeDescendants.toString());

        final CpsConfiguration cpsConfiguration = getCpsConfig(appConfiguration);

        String uri = buildCpsUrl(cpsConfiguration.getUrl(), NODES_API_PATH, anchor, queryParams);
        if ("query".equals(requestType)) {
            uri = buildCpsUrl(cpsConfiguration.getUrl(), QUERY_API_PATH, anchor, queryParams);
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        final String username = cpsConfiguration.getUsername();
        final String password = cpsConfiguration.getPassword();
        headers.setBasicAuth(username, password);
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

    private CpsConfiguration getCpsConfig(final AppConfiguration appConfiguration) {
        if (appConfiguration.getCpsClient().equals("cpsCore")) {
            return appConfiguration.getCpsCore();
        }
        return appConfiguration.getNcmp();
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
