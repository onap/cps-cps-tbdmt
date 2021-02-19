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

package org.onap.cps.tbdmt.rest;

import java.util.Collection;
import javax.validation.Valid;
import org.onap.cps.tbdmt.exception.TemplateNotFoundException;
import org.onap.cps.tbdmt.model.Template;
import org.onap.cps.tbdmt.model.TemplateId;
import org.onap.cps.tbdmt.model.TemplateRequest;
import org.onap.cps.tbdmt.service.TemplateBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemplateController {

    @Autowired
    private TemplateBusinessLogic templateBusinessLogic;

    @PostMapping(path = "/templates")
    public ResponseEntity<Template> createTemplate(@Valid @RequestBody final TemplateRequest request) {
        return new ResponseEntity<>(templateBusinessLogic.createTemplate(request),
            HttpStatus.CREATED);
    }

    /**
     * Get All Templates.
     *
     * @return templates
     */
    @GetMapping(path = "/templates")
    public ResponseEntity<Collection<Template>> getAllTemplates() {
        final Collection<Template> templates = templateBusinessLogic.getAllTemplates();
        if (templates.isEmpty()) {
            throw new TemplateNotFoundException("Template repository is empty");
        }
        return new ResponseEntity<>(templates, HttpStatus.OK);
    }

    /**
     * Get Template by Id.
     *
     * @param id id to find the template
     * @param model schema set to find the template
     * @return template
     */
    @GetMapping(path = "/templates/{model}/{id}")
    public ResponseEntity<Template> getTemplate(@PathVariable final String id,
        @PathVariable final String model) {
        return new ResponseEntity<>(
            templateBusinessLogic.getTemplate(new TemplateId(id, model)),
            HttpStatus.OK);

    }

    @DeleteMapping(path = "/templates/{model}/{id}")
    public void deleteTemplate(@PathVariable final String id, @PathVariable final String model) {
        templateBusinessLogic.deleteTemplate(new TemplateId(id, model));
    }
}
