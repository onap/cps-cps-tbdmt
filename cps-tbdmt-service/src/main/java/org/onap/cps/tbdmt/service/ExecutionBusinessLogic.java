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

import com.hubspot.jinjava.Jinjava;
import java.util.Map;
import java.util.Optional;
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
     * @param id id
     * @param executionRequest inputs to be applied to the templates
     * @return result response from the execution of template
     */
    public String executeTemplate(final String schemaSet, final String id, final ExecutionRequest executionRequest) {

        final Optional<Template> templateOptional = templateRepository.findById(new TemplateKey(id, schemaSet));
        if (templateOptional.isPresent()) {
            return execute(templateOptional.get(), executionRequest.getInputParameters());
        }
        throw new TemplateNotFoundException("Template does not exist");
    }

    private String execute(final Template template, final Map<String, String> inputParameters) {
        final String anchor = appConfiguration.getSchemaToAnchor().get(template.getModel());
        if (anchor == null) {
            throw new ExecuteException("Anchor not found for the schema");
        }
        final String xpath = generateXpath(template.getXpathTemplate(), inputParameters);
        try {
            return cpsRestClient.fetchNode(anchor, xpath);
        } catch (final CpsClientException e) {
            throw new ExecuteException(e.getLocalizedMessage());
        }
    }

    private String generateXpath(final String xpathTemplate, final Map<String, String> templateParameters) {
        return new Jinjava().render(xpathTemplate, templateParameters);
    }
}
