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

package org.onap.cps.tbdmt.service;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tbdmt.client.CpsRestClient;
import org.onap.cps.tbdmt.db.TemplateRepository;
import org.onap.cps.tbdmt.exception.CpsClientException;
import org.onap.cps.tbdmt.exception.ExecuteException;
import org.onap.cps.tbdmt.exception.TemplateNotFoundException;
import org.onap.cps.tbdmt.model.AppConfiguration;
import org.onap.cps.tbdmt.model.ExecutionRequest;
import org.onap.cps.tbdmt.model.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(AppConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
public class ExecutionBusinessLogicTest {

    @TestConfiguration
    static class ExecutionBusinessLogicTestContextConfiguration {

        @Bean
        public ExecutionBusinessLogic executionBusinessLogic() {
            return new ExecutionBusinessLogic();
        }
    }

    @Autowired
    private ExecutionBusinessLogic executionBusinessLogic;

    @MockBean
    private TemplateRepository templateRepository;

    @MockBean
    private CpsRestClient cpsRestClient;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ExecutionRequest request;

    private Template template;

    private Template queryTemplate;

    /**
     * Setup variables before test.
     *
     */
    @Before
    public void setup() {
        final Map<String, String> input = new HashMap<>();
        input.put("coverageArea", "Zone 1");
        request = new ExecutionRequest(input);
        final String xpathTemplate = "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
            + "/coverage-area[@coverageArea='{{coverageArea}}']";
        template = new Template("getNbr", "ran-network", xpathTemplate, "get");
        queryTemplate = new Template("getNbr", "ran-network", xpathTemplate, "query");
    }

    @Test
    public void testExecuteTemplate() throws Exception {
        final String resultString = "[{\"key\": \"value\"}]";
        Mockito.when(cpsRestClient
            .fetchNode("ran-network", "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
                + "/coverage-area[@coverageArea='Zone 1']", "get"))
            .thenReturn(resultString);
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template));
        assertEquals(resultString,
            executionBusinessLogic.executeTemplate("ran-network", "getNbr", request));

        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.empty());
        exception.expect(TemplateNotFoundException.class);
        exception.expectMessage("Template does not exist");
        executionBusinessLogic.executeTemplate("ran-network", "getNbr", request);

    }

    @Test
    public void testExecuteTemplateException() throws Exception {
        final String exceptionMessage = "Response from CPS other than 200: 404";
        Mockito.when(cpsRestClient
            .fetchNode("ran-network", "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
                + "/coverage-area[@coverageArea='Zone 1']", "get"))
            .thenThrow(new CpsClientException(exceptionMessage));
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template));
        exception.expect(ExecuteException.class);
        exception.expectMessage(exceptionMessage);
        executionBusinessLogic.executeTemplate("ran-network", "getNbr", request);

        final Template template1 = new Template("getNbr", "ran-net", "sample", "get");
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template1));
        exception.expect(ExecuteException.class);
        exception.expectMessage("Anchor not found for the schema");
        executionBusinessLogic.executeTemplate("ran-net", "getNbr", request);

    }

    @Test
    public void testExecuteTemplateNoAnchor() {
        final Template template = new Template("getNbr", "ran-net", "sample", "get");
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template));
        exception.expect(ExecuteException.class);
        exception.expectMessage("Anchor not found for the schema");
        executionBusinessLogic.executeTemplate("ran-net", "getNbr", request);
    }

    @Test
    public void testExecuteTemplateQueryApi() throws Exception {
        final String resultString = "[{\"key\": \"value\"}]";
        Mockito.when(cpsRestClient
            .fetchNode("ran-network", "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
                + "/coverage-area[@coverageArea='Zone 1']", "query"))
            .thenReturn(resultString);
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(queryTemplate));
        assertEquals(resultString,
            executionBusinessLogic.executeTemplate("ran-network", "getNbr", request));

    }

}
