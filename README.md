<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2022 Wipro Limited..
  ================================================================================
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  SPDX-License-Identifier: Apache-2.0
  ============LICENSE_END=========================================================
-->

# ONAP CPS TBDMT(Template Based Data Model Transformer)

This service shall be used to map the erstwhile Config-DB-like REST APIs to appropriate CPS API calls. The purpose of this service is to abstract the details of (possibly multiple, and complex) XPath queries from the users of CPS.
See [wiki for CPS-TBDMT](https://wiki.onap.org/display/DW/R9+TBDMT+Enhancements)

## Building Java Archive only

Following command builds Java executable jar to `target/cps-tbdmt-x.y.z-SNAPSHOT` JAR file
without generating any docker images:

```bash
mvn clean install
```

## Building Java Archive and local Docker image

Following command builds the JAR file and also generates the Docker image:

```bash
mvn clean install -Pdocker -Ddocker.repository.push=
```

## Running via Docker Compose

`docker-compose.yml` file is provided to be run with `docker-compose` tool and local image previously built.

Execute following command from project root folder to start all services:

```bash
docker-compose up
```
