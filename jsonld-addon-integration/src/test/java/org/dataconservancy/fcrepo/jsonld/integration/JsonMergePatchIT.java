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
import static org.dataconservancy.fcrepo.jsonld.integration.FcrepoIT.assertSuccess;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.io.IOException;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dataconservancy.fcrepo.jsonld.test.JsonMergePatchTests;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionComparatorMode;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchIT extends JsonMergePatchTests implements FcrepoIT {

    final FcrepoClient client = new FcrepoClientBuilder().build();

    final CloseableHttpClient http = HttpClients.createDefault();

    static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void prolematicPatchTest() throws Exception {
        final URI resource;
        try (FcrepoResponse response = client.post(URI.create(fcrepoBaseURI))
                                             .body(this.getClass().getResourceAsStream("/pass_object.nt"),
                                                   "text/turtle")
                                             .perform()) {
            assertSuccess(response);
            resource = response.getLocation();
        }

        try (FcrepoResponse response = client.patch(resource).body(this.getClass().getResourceAsStream(
            "/pass_patch.sparql")).perform()) {
            assertSuccess(response);
        }

    }

    private static void assertJsonEquals(String origString, String patchedString) {

        final ObjectNode orig;
        final ObjectNode patched;
        try {
            orig = (ObjectNode) mapper.readTree(origString);
            patched = (ObjectNode) mapper.readTree(patchedString);
        } catch (final IOException e) {
            throw new RuntimeException("JSON parse error", e);
        }

        orig.remove("@context");
        patched.remove("@context");
        assertReflectionEquals(orig, patched, ReflectionComparatorMode.LENIENT_ORDER);

    }

    private URI add(String json) {
        return attempt(1, () -> {
            try (FcrepoResponse response = client
                .post(URI.create(fcrepoBaseURI))
                .body(toInputStream(json, UTF_8), "application/ld+json")
                .perform()) {
                assertSuccess(response);
                return response.getLocation();
            }
        });
    }

    private String doPatch(URI id, String json) {
        final HttpPatch patch = new HttpPatch(id);
        patch.addHeader("content-type", "application/merge-patch+json");
        patch.setEntity(new StringEntity(json, UTF_8));

        try (CloseableHttpResponse response = http.execute(patch)) {
            if (response.getStatusLine().getStatusCode() > 399) {
                final String message = EntityUtils.toString(response.getEntity());
                throw new RuntimeException("PATCH failed with error" + response.getStatusLine() + ": " + message);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Connection error", e);
        }

        try (FcrepoResponse response = client.get(id)
                                             .accept("application/ld+json")
                                             .perform()) {
            return IOUtils.toString(response.getBody(), UTF_8);
        } catch (IOException | FcrepoOperationFailedException e) {
            throw new RuntimeException("Connection error", e);
        }
    }

    @Override
    protected void assertPatch(String jsomMergePatch, String expectedJson) {
        final String initial = INITIAL.replace(TEST_RESOURCE_ID, "");

        final URI resourceUri = add(initial);

        final String patch = jsomMergePatch.replace(TEST_RESOURCE_ID, resourceUri.toString());
        final String expected = expectedJson.replace(TEST_RESOURCE_ID, resourceUri.toString());

        final String result = doPatch(resourceUri, patch);
        assertJsonEquals(expected, result);
        ;
    }

    @Override
    protected void doPatch(String json) {
        final String initial = INITIAL.replace(TEST_RESOURCE_ID, "");

        final URI resourceUri = add(initial);

        final String patch = json.replace(TEST_RESOURCE_ID, resourceUri.toString());
        doPatch(resourceUri, patch);
    }
}
