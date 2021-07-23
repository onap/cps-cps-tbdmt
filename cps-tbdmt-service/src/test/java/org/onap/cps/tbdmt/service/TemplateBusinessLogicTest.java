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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tbdmt.db.TemplateRepository;
import org.onap.cps.tbdmt.exception.TemplateNotFoundException;
import org.onap.cps.tbdmt.model.Template;
import org.onap.cps.tbdmt.model.TemplateKey;
import org.onap.cps.tbdmt.model.TemplateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class TemplateBusinessLogicTest {

    @TestConfiguration
    static class TemplateBusinessLogicTestContextConfiguration {

        @Bean
        public TemplateBusinessLogic templateBusinessLogic() {
            return new TemplateBusinessLogic();
        }
    }

    @Autowired
    private TemplateBusinessLogic templateBusinessLogic;

    @MockBean
    private TemplateRepository templateRepository;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Template template;
    private TemplateKey templateKey;

    @Before
    public void setup() {
        template = new Template("getNbr", "ran-network", "sample", "get", true, "sample", "getRIC");
        final TemplateKey templateKey = new TemplateKey("getNbr");
    }

    @Test
    public void testCreateTemplate() throws Exception {
        final TemplateRequest templateRequest = new TemplateRequest("getNbr", "ran-network", "sample", "get",
                        true, "sample", "getRIC");
        Mockito.when(templateRepository.save(ArgumentMatchers.any())).thenReturn(template);
        assertEquals(template, templateBusinessLogic.createTemplate(templateRequest));
    }

    @Test
    public void testGetAllTemplates() throws Exception {
        final Collection<Template> templates = new HashSet<>();
        templates.add(template);
        Mockito.when(templateRepository.findAll()).thenReturn(templates);
        assertEquals(templates, templateBusinessLogic.getAllTemplates());
    }

    @Test
    public void testGetTemplate() throws Exception {
        Mockito.when(templateRepository.findById(templateKey)).thenReturn(Optional.of(template));
        assertEquals(template, templateBusinessLogic.getTemplate(templateKey));

        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.empty());
        exception.expect(TemplateNotFoundException.class);
        exception.expectMessage("Template not found for given id: getNbr");
        templateBusinessLogic.getTemplate(new TemplateKey("getNbr"));
    }

    @Test
    public void testDeleteTemplate() throws Exception {
        Mockito.when(templateRepository.existsById(templateKey)).thenReturn(true);
        templateBusinessLogic.deleteTemplate(templateKey);
        verify(templateRepository, times(1)).deleteById(templateKey);

        Mockito.when(templateRepository.existsById(ArgumentMatchers.any())).thenReturn(false);
        exception.expect(TemplateNotFoundException.class);
        exception.expectMessage("Template not found for given id: getNbr");
        templateBusinessLogic.deleteTemplate(new TemplateKey("getNbr"));
    }
}
