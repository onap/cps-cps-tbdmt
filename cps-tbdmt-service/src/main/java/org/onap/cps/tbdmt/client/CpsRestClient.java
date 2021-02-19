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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CpsRestClient {

    private static final String NODES_API_PATH = "%s/anchors/%s/nodes?cps-path=%s";

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
    public String fetchNode(final String anchor, final String xpath) throws CpsClientException {
        final String url = appConfiguration.getXnfProxyUrl();

        final String uri = String.format(NODES_API_PATH, url, anchor, xpath);

        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
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

}