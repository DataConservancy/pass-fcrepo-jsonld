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
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchIT implements FcrepoIT {

    final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

    final CloseableHttpClient http = HttpClients.createDefault();

    static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void identityTest() throws Exception {

        final String originalResource = "{ " +
                "\"@type\": \"Cow\", " +
                "\"@id\": \"\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        final URI id = add(originalResource);

        final String result = doPatch(id, String.format("{ " +
                "\"@id\": \"%s\", " + "\"@context\": \"http://example.org/farm\"" + "}", id));
        assertJsonEquals(originalResource, result);

    }

    private static void assertJsonEquals(String origString, String patchedString) throws Exception {
        final ObjectNode orig = (ObjectNode) mapper.readTree(origString);
        final ObjectNode patched = (ObjectNode) mapper.readTree(patchedString);

        orig.remove("@context");
        orig.remove("@id");
        orig.remove("id");
        patched.remove("@context");
        patched.remove("@id");
        patched.remove("id");

        assertReflectionEquals(orig, patched, ReflectionComparatorMode.LENIENT_ORDER);

    }

    private URI add(String json) {
        return attempt(1, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(toInputStream(json, UTF_8), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });
    }

    private String doPatch(URI id, String json) throws Exception {
        final HttpPatch patch = new HttpPatch(id);
        patch.addHeader("content-type", "application/merge-patch+json");
        patch.setEntity(new StringEntity(json, UTF_8));

        try (FcrepoResponse response = client.get(id)
                .accept("application/ld+json")
                .perform()) {
            final ObjectNode orig = (ObjectNode) mapper.readTree(IOUtils.toString(response.getBody(), UTF_8));
            orig.remove("@context");
            orig.remove("@id");
            orig.remove("id");
        }

        try (CloseableHttpResponse response = http.execute(patch)) {
            assertTrue(response.getStatusLine().getStatusCode() < 399);
        }

        try (FcrepoResponse response = client.get(id)
                .accept("application/ld+json")
                .perform()) {
            return IOUtils.toString(response.getBody(), UTF_8);
        }
    }
}
