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

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import org.onap.cps.tbdmt.db.TemplateRepository;
import org.onap.cps.tbdmt.exception.TemplateNotFoundException;
import org.onap.cps.tbdmt.model.Template;
import org.onap.cps.tbdmt.model.TemplateId;
import org.onap.cps.tbdmt.model.TemplateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateBusinessLogic {

    private static final String TEMPLATE_NOT_FOUND_ERROR = "Template not found for given id: %s";

    @Autowired
    private TemplateRepository templateRepository;

    /**
     * Create Template.
     *
     * @param templateRequest request object
     * @return template
     */
    public Template createTemplate(final TemplateRequest templateRequest) {
        final Template template = new Template(templateRequest.getId(),
            templateRequest.getModel(),
            templateRequest.getXpathTemplate());
        return templateRepository.save(template);
    }

    /**
     * Get All Templates.
     *
     * @return templates
     */
    public Collection<Template> getAllTemplates() {
        final Collection<Template> templates = new HashSet<>();
        templateRepository.findAll().forEach(templates::add);
        return templates;
    }

    /**
     * Get Template by Id.
     *
     * @param templateId template id to find the template
     * @return template
     */
    public Template getTemplate(final TemplateId templateId) {
        final Optional<Template> template = templateRepository.findById(templateId);
        if (template.isPresent()) {
            return template.get();
        } else {
            final String errorMessage = String.format(TEMPLATE_NOT_FOUND_ERROR, templateId.getId());
            throw new TemplateNotFoundException(errorMessage);
        }
    }

    /**
     * Delete Template.
     *
     * @param templateId template id to find the template
     */
    public void deleteTemplate(final TemplateId templateId) {
        if (templateRepository.existsById(templateId)) {
            templateRepository.deleteById(templateId);
        } else {
            final String errorMessage = String.format(TEMPLATE_NOT_FOUND_ERROR, templateId.getId());
            throw new TemplateNotFoundException(errorMessage);
        }
    }
}
