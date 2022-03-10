/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021-2022 Wipro Limited.
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

import java.io.File;
import java.nio.file.Files;
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
import org.onap.cps.tbdmt.model.TemplateKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
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
        final Map<String, Object> payload = new HashMap<>();
        input.put("coverageArea", "Zone 1");
        request = new ExecutionRequest(input, payload);
        final String xpathTemplate = "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
            + "/coverage-area[@coverageArea='{{coverageArea}}']";
        template = new Template("getNbr", "ran-network", xpathTemplate, "get", true, "", "");
        queryTemplate = new Template("getNbr", "ran-network", xpathTemplate, "query", true, "", "");
    }

    @Test
    public void testExecuteTemplate() throws Exception {
        final String resultString = "[{\"key\": \"value\"}]";
        Mockito.when(cpsRestClient
            .fetchNode("ran-network", "/ran-coverage-area/pLMNIdList[@mcc='310' and @mnc='410']"
                + "/coverage-area[@coverageArea='Zone 1']", "get", true))
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
                + "/coverage-area[@coverageArea='Zone 1']", "get", true))
            .thenThrow(new CpsClientException(exceptionMessage));
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template));
        exception.expect(ExecuteException.class);
        exception.expectMessage(exceptionMessage);
        executionBusinessLogic.executeTemplate("ran-network", "getNbr", request);

        final Template template1 = new Template("getNbr", "ran-net", "sample", "get", true, "", "");
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(template1));
        exception.expect(ExecuteException.class);
        exception.expectMessage("Anchor not found for the schema");
        executionBusinessLogic.executeTemplate("ran-net", "getNbr", request);

    }

    @Test
    public void testExecuteTemplateNoAnchor() {
        final Template template = new Template("getNbr", "ran-net", "sample", "get", true, "", "");
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
                + "/coverage-area[@coverageArea='Zone 1']", "query", true))
            .thenReturn(resultString);
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.of(queryTemplate));
        assertEquals(resultString,
            executionBusinessLogic.executeTemplate("ran-network", "getNbr", request));

    }

    @Test
    public void testOutputTransform() {
        final Map<String, String> input = new HashMap<>();
        input.put("idNearRTRIC", "11");
        final String transformParam = "GNBDUFunction, NRCellDU, attributes, cellLocalId";
        final Template template = new Template("get-nrcelldu-id", "ran-network", "/NearRTRIC/[@idNearRTRIC='11']",
                "get", true, null, transformParam);
        final String transformedResult = "[15299,15277]";
        try {
            final String result = readFromFile("sample_transform_query_data.json");
            Mockito.when(cpsRestClient.fetchNode("ran-network", "/NearRTRIC/[@idNearRTRIC='11']", "get", true))
                    .thenReturn(result);
            Mockito.when(templateRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(template));
            assertEquals(transformedResult,
                    executionBusinessLogic.executeTemplate("ran-network", "get-nrcelldu-id", request));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleQuery() {
        final Map<String, String> input = new HashMap<>();
        input.put("idNearRTRIC", "11");
        final String transformParam1 = "branch, name";
        final String transformParam2 = "name";
        final Template template1 =
                new Template("get-tree", "ran-network", "/test-tree", "get", true, null, transformParam1);
        final Template template2 = new Template("get-branch", "ran-network", "/test-tree/branch[@name='{{name}}']/nest",
                "get", true, "get-tree", transformParam2);
        final String transformedResult = "[\"Big\", \"Small\"]";

        try {
            final String result1 = readFromFile("sample_multiple_query_data_1.json");
            final String result2 = readFromFile("sample_multiple_query_data_2.json");
            final String result3 = readFromFile("sample_multiple_query_data_3.json");
            Mockito.when(cpsRestClient.fetchNode("ran-network", "/test-tree", "get", true)).thenReturn(result1);
            Mockito.when(templateRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(template1));

            Mockito.when(cpsRestClient.fetchNode("ran-network", "/test-tree/branch[@name='Right']/nest", "get", true))
                    .thenReturn(result2);
            Mockito.when(cpsRestClient.fetchNode("ran-network", "/test-tree/branch[@name='Left']/nest", "get", true))
                    .thenReturn(result3);
            final TemplateKey key = new TemplateKey("get-branch");
            Mockito.when(templateRepository.findById(key)).thenReturn(Optional.of(template2));

            assertEquals(transformedResult,
                    executionBusinessLogic.executeTemplate("ran-network", "get-branch", request));
        } catch (final CpsClientException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteDataRequest() {
        final Map<String, String> input = new HashMap<>();
        input.put("idNearRTRIC", "11");
        final String transformParam = "GNBDUFunction, NRCellDU, attributes, cellLocalId";
        final Template template = new Template("delete-snssai", "ran-network",
                  "/NearRTRIC/[@idNearRTRIC='11']/attributes/"
                + "pLMNInfoList[@mcc='370' and '@mnc='410']/sNSSAIList[@sNssai='111-1111']",
                  "delete-list-node", true, null, null);
        try {
            final String result = "Success";
            Mockito.when(cpsRestClient.deleteData("ran-network", "/NearRTRIC/[@idNearRTRIC='11']/attributes/"
              + "pLMNInfoList[@mcc='370' and '@mnc='410']/sNSSAIList[@sNssai='111-1111']", "delete-list-node"))
                    .thenReturn(result);
            Mockito.when(templateRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(template));
            assertEquals("Success",
                    executionBusinessLogic.executeTemplate("ran-network", "delete-snssai", request));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteDataFailRequest() throws Exception {
        final Template template = new Template("deleteNbr", "ran-network", "sample", "delete-list-node", true, "", "");
        Mockito.when(cpsRestClient.deleteData("ran-network", "sample", "delete-list-node"))
               .thenThrow(new ExecuteException("Response code from CPS other than 200: 401"));
        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
               .thenReturn(Optional.of(template));
        exception.expect(ExecuteException.class);
        executionBusinessLogic.executeTemplate("ran-net", "deleteNbr", request);
    }

    @Test
    public void testRemovExtraBracketsIfAny() {
        final Map<String, String> input = new HashMap<>();
        input.put("idNearRTRIC", "11");
        final String transformParam1 = "branch, nest, birds";
        final Template template1 =
                new Template("get-tree", "ran-network", "/test-tree", "get", true, null, transformParam1);
        final String transformParam2 = "GNBDUFunction, NRCellDU, attributes, nRSectorCarrierRef";
        final Template template2 = new Template("get-nrcelldu-data", "ran-network", "/NearRTRIC/[@idNearRTRIC='11']",
                "get", true, null, transformParam2);
        final String transformedResult1 = "[[\"Owl\",\"Raven\",\"Crow\"],[\"Robin\",\"Sparrow\",\"Finch\"]]";
        final String transformedResult2 = "[\"OUSales\",\"OUSales\"]";

        try {
            final String result1 = readFromFile("sample_multiple_query_data_1.json");
            Mockito.when(cpsRestClient.fetchNode("ran-network", "/test-tree", "get", true)).thenReturn(result1);
            Mockito.when(templateRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(template1));
            final String result2 = readFromFile("sample_transform_query_data.json");
            Mockito.when(cpsRestClient.fetchNode("ran-network", "/NearRTRIC/[@idNearRTRIC='11']", "get", true))
                    .thenReturn(result2);
            final TemplateKey key = new TemplateKey("get-nrcelldu-data");
            Mockito.when(templateRepository.findById(key)).thenReturn(Optional.of(template2));

            assertEquals(transformedResult1,
                    executionBusinessLogic.executeTemplate("ran-network", "get-tree", request));
            assertEquals(transformedResult2,
                    executionBusinessLogic.executeTemplate("ran-network", "get-nrcelldu-data", request));
        } catch (final CpsClientException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a file from classpath.
     *
     * @param fileName name of the file to be read
     * @return result contents of the file
     */
    private String readFromFile(final String fileName) {
        String content = new String();
        try {
            final File resource = new ClassPathResource(fileName).getFile();
            content = new String(Files.readAllBytes(resource.toPath()));
        } catch (final Exception e) {
            e.printStackTrace();
            content = null;
        }
        return content;
    }

}
