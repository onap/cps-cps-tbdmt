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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hubspot.jinjava.Jinjava;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.tbdmt.client.CpsRestClient;
import org.onap.cps.tbdmt.db.TemplateRepository;
import org.onap.cps.tbdmt.exception.CpsClientException;
import org.onap.cps.tbdmt.exception.ExecuteException;
import org.onap.cps.tbdmt.exception.OutputTransformationException;
import org.onap.cps.tbdmt.exception.TemplateNotFoundException;
import org.onap.cps.tbdmt.model.AppConfiguration;
import org.onap.cps.tbdmt.model.ExecutionRequest;
import org.onap.cps.tbdmt.model.Template;
import org.onap.cps.tbdmt.model.TemplateKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExecutionBusinessLogic {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AppConfiguration appConfiguration;

    @Autowired
    private CpsRestClient cpsRestClient;

    /**
     * Execute a template stored in the database.
     *
     * @param schemaSet schema set
     * @param templateId templateId
     * @param executionRequest inputs to be applied to the templates
     * @return result response from the execution of template
     */
    public String executeTemplate(final String schemaSet, final String templateId,
                    final ExecutionRequest executionRequest) {

        final Optional<Template> templateOptional = templateRepository.findById(new TemplateKey(templateId));
        if (templateOptional.isPresent()) {
            if (!StringUtils.isBlank(templateOptional.get().getMultipleQueryTemplateId())) {
                return executeMultipleQuery(templateOptional.get(), executionRequest.getInputParameters(),
                        executionRequest.getPayload());
            } else {
                return execute(templateOptional.get(), executionRequest.getInputParameters(),
                        executionRequest.getPayload(), schemaSet);
            }
        }
        throw new TemplateNotFoundException("Template does not exist");
    }

    private String executeMultipleQuery(final Template template, final Map<String, String> inputParameters,
            final Map<String, Object> payload) throws OutputTransformationException {
        final List<Object> processedQueryOutput = new ArrayList<Object>();
        final String multipleQuerytemplateId = template.getMultipleQueryTemplateId();
        final Optional<Template> multipleQueryTemplate =
                templateRepository.findById(new TemplateKey(multipleQuerytemplateId));
        if (!multipleQueryTemplate.isPresent()) {
            throw new TemplateNotFoundException("Multiple query template does not exist");
        } else {
            if (StringUtils.isBlank(multipleQueryTemplate.get().getTransformParam())) {
                throw new OutputTransformationException("Error executing multiple query: "
                                + "Template must have atleast one transformParameter");
            }
            final List<String> transformParamList = new ArrayList<String>(
                    Arrays.asList(multipleQueryTemplate.get().getTransformParam().split("\\s*,\\s*")));
            final String inputKey = transformParamList.get(transformParamList.size() - 1);
            final String queryParamString = execute(multipleQueryTemplate.get(), inputParameters,
                            payload, template.getModel());
            final List<String> queryParamList = new ArrayList<String>();
            final JsonParser jsonParser = new JsonParser();
            final Gson gson = new Gson();
            try {
                if (jsonParser.parse(queryParamString).isJsonArray()) {
                    final JsonArray array = jsonParser.parse(queryParamString).getAsJsonArray();

                    for (final JsonElement jsonElement : array) {
                        queryParamList.add(gson.fromJson(jsonElement, String.class));
                    }
                } else {
                    queryParamList.add(queryParamString);
                }
                queryParamList.forEach(queryParam -> {
                    final Map<String, String> inputParameter = new HashMap<String, String>();
                    inputParameter.put(inputKey, queryParam);
                    final Object result = execute(template, inputParameter, payload, template.getModel());
                    processedQueryOutput.add(result);
                });
            } catch (final Exception e) {
                throw new OutputTransformationException(e.getLocalizedMessage());
            }
            return processedQueryOutput.toString();
        }
    }

    private String execute(final Template template, final Map<String, String> inputParameters,
            final Map<String, Object> payload, final String schemaSet) {

        final String anchor = "dynamic".equalsIgnoreCase(template.getModel())
            ?  schemaSet : appConfiguration.getSchemaToAnchor().get(template.getModel());
        if (anchor == null) {
            throw new ExecuteException("Anchor not found for the schema");
        }
        final String xpath = generateXpath(template.getXpathTemplate(), inputParameters);
        try {
            if ("put".equalsIgnoreCase(template.getRequestType()) || "patch".equalsIgnoreCase(template.getRequestType())
                    || "post".equalsIgnoreCase(template.getRequestType())
                    || "post-list-node".equalsIgnoreCase(template.getRequestType())) {
                return cpsRestClient.addData(anchor, xpath, template.getRequestType(), payload);
            } else if ("delete".equalsIgnoreCase(template.getRequestType())
                    || "delete-list-node".equalsIgnoreCase(template.getRequestType())) {
                return cpsRestClient.deleteData(anchor, xpath, template.getRequestType());
            } else {
                final String result = cpsRestClient.fetchNode(anchor, xpath, template.getRequestType(),
                        template.getIncludeDescendants());
                if (StringUtils.isBlank(template.getTransformParam())) {
                    return result;
                } else {
                    final List<JsonElement> json = transform(template, result);
                    return removeExtraBracketsIfAny(json, result);
                }
            }
        } catch (final CpsClientException e) {
            throw new ExecuteException(e.getLocalizedMessage());
        }
    }

    private List<JsonElement> transform(final Template template, final String result) {

        final JsonElement transformJsonElement = new Gson().fromJson(result, JsonElement.class);
        List<JsonElement> transformedResult;
        List<JsonElement> temp;
        List<JsonElement> processedOutput = new ArrayList<JsonElement>();
        final List<String> transformParamList =
                new ArrayList<String>(Arrays.asList(template.getTransformParam().split("\\s*,\\s*")));
        try {
            if (transformParamList.size() > 0) {
                processedOutput = find(transformParamList.get(0), transformJsonElement, new ArrayList<JsonElement>());
                transformParamList.remove(0);
                for (final String param : transformParamList) {
                    transformedResult = new ArrayList<JsonElement>();

                    for (final JsonElement json : processedOutput) {
                        temp = find(param, json, new ArrayList<JsonElement>());
                        transformedResult.addAll(temp);
                    }
                    processedOutput.clear();
                    processedOutput.addAll(transformedResult);
                }
            }
        } catch (final Exception e) {
            throw new OutputTransformationException(e.getLocalizedMessage());
        }

        return processedOutput;

    }

    private static List<JsonElement> find(final String param, final JsonElement jsonElement,
                    final List<JsonElement> output) {

        if (jsonElement.isJsonArray()) {
            for (final JsonElement je : jsonElement.getAsJsonArray()) {
                find(param, je, output);
            }
        } else {
            if (jsonElement.isJsonObject()) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has(param)) {
                    output.add(jsonObject.getAsJsonObject().get(param));

                }
            }
        }
        return output;

    }

    private static String removeExtraBracketsIfAny(final List<JsonElement> jsonElementList, final String result) {

        final Gson gson = new Gson();
        final List<JsonElement> updatedResult = new ArrayList<>();
        if (jsonElementList.size() == 1) {
            if (gson.fromJson(result, JsonElement.class).isJsonArray()) {
                return gson.toJson(jsonElementList);
            }
            return gson.toJson(jsonElementList.get(0));
        }
        if (jsonElementList.size() > 1) {
            jsonElementList.forEach(jsonElement -> {
                if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() == 1) {
                    updatedResult.add(jsonElement.getAsJsonArray().get(0));
                } else {
                    updatedResult.add(jsonElement);
                }
            });
            return gson.toJson(updatedResult);
        }
        return gson.toJson(jsonElementList);

    }

    private String generateXpath(final String xpathTemplate, final Map<String, String> templateParameters) {
        return new Jinjava().render(xpathTemplate, templateParameters);
    }
}
