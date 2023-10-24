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

package org.onap.cps.tbdmt.exception;

import java.util.ArrayList;
import java.util.List;
import org.onap.cps.tbdmt.model.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class TemplateExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handle TemplateNotFoundException.
     *
     * @param templateNotFoundException Exception
     * @param webRequest web request
     * @return response entity
     */
    @ExceptionHandler(TemplateNotFoundException.class)
    public final ResponseEntity<Object> handleTemplateNotFoundException(
        final TemplateNotFoundException templateNotFoundException, final WebRequest webRequest) {
        final List<String> details = new ArrayList<>();
        details.add(templateNotFoundException.getLocalizedMessage());
        final ErrorResponse ErrorResponse = new ErrorResponse("Template Not found", details);
        return new ResponseEntity<>(ErrorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle ExecuteException.
     *
     * @param executeException Exception
     * @param webRequest web request
     * @return response entity
     */
    @ExceptionHandler(ExecuteException.class)
    public final ResponseEntity<Object> handleExecutionException(final ExecuteException executeException,
        final WebRequest webRequest) {
        final List<String> details = new ArrayList<>();
        details.add(executeException.getLocalizedMessage());
        final ErrorResponse ErrorResponse = new ErrorResponse("Error while executing template", details);
        return new ResponseEntity<>(ErrorResponse, HttpStatus.OK);
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        final MethodArgumentNotValidException methodArgumentNotValidException, final HttpHeaders httpHeaders,
        final HttpStatus httpStatus, final WebRequest webRequest) {
        final List<String> details = new ArrayList<>();
        for (final ObjectError objectError : methodArgumentNotValidException.getBindingResult().getAllErrors()) {
            details.add(objectError.getDefaultMessage());
        }
        final ErrorResponse ErrorResponse = new ErrorResponse("Validation Failed", details);
        return new ResponseEntity<>(ErrorResponse, HttpStatus.BAD_REQUEST);
    }
}
