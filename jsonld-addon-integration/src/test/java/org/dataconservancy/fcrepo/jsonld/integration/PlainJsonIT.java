/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.fcrepo.jsonld.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.getUncompactedJsonld;

import java.net.URI;
import java.util.Arrays;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class PlainJsonIT implements FcrepoIT {

    final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

    static final URI SERVER_MANAGED = URI.create("http://fedora.info/definitions/v4/repository#ServerManaged");

    @Ignore
    @Test
    public void jsonGetTest() throws Exception {
        final URI resource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(toInputStream(getUncompactedJsonld(), UTF_8), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .get(resource)
                .accept("application/json")
                .preferRepresentation(emptyList(), Arrays.asList(SERVER_MANAGED))
                .perform()) {

            System.out.println(IOUtils.toString(response.getBody(), UTF_8));
        }
    }
}
