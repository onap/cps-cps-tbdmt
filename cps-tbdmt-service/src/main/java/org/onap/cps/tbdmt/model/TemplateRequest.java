/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Wipro Limited.
 *  Modifications Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.tbdmt.model;

import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateRequest implements Serializable {

    private static final long serialVersionUID = 543L;

    @NotEmpty(message = "template id missing")
    private String templateId;

    @NotEmpty(message = "model missing")
    private String model;

    @NotEmpty(message = "template missing")
    private String xpathTemplate;

    @NotEmpty(message = "request type missing")
    private String requestType;

    private Boolean includeDescendants;

    private String multipleQueryTemplateId;

    private String transformParam;
}
