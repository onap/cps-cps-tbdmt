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

import javax.validation.Valid;
import org.onap.cps.tbdmt.model.ExecutionRequest;
import org.onap.cps.tbdmt.service.ExecutionBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionController {

    @Autowired
    private ExecutionBusinessLogic executionBusinessLogic;

    /**
     * Execute a template by model and templateId.
     *
     * @param templateId Id to find the template
     * @param model schema set to find the template
     * @return result of the execution
     */
    @PostMapping(path = "/execute/{model}/{templateId}")
    public ResponseEntity<String> executeTemplate(@Valid @PathVariable final String model,
        @Valid @PathVariable final String templateId,
        @Valid @RequestBody final ExecutionRequest executionRequest) {
        final String result = executionBusinessLogic.executeTemplate(model,
            templateId, executionRequest);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
