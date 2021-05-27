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
import static org.dataconservancy.fcrepo.jsonld.test.JsonldTestUtil.assertCompact;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class CompactionIT implements FcrepoIT {

    CloseableHttpClient client = HttpClients.createDefault();

    @Test
    public void compactionTest() throws Exception {
        final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

        final URI jsonldResource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(this.getClass().getResourceAsStream("/compact-uri.json"), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .get(jsonldResource)
                .accept("application/ld+json")
                .perform()) {

            final String body = IOUtils.toString(response.getBody(), UTF_8);
            assertCompact(body);

            assertNotNull(response.getHeaderValue("X-CREATED"));
            assertNotNull(response.getHeaderValue("X-MODIFIED"));
        }
    }

    @Test
    public void compactionTestWithAcceptCompact() throws Exception {
        final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

        final URI jsonldResource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(this.getClass().getResourceAsStream("/compact-uri.json"), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .get(jsonldResource)
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"")
                .perform()) {

            final String body = IOUtils.toString(response.getBody(), UTF_8);
            assertCompact(body);

            assertNotNull(response.getHeaderValue("X-CREATED"));
            assertNotNull(response.getHeaderValue("X-MODIFIED"));
        }
    }

    @Test
    public void compactionTesWithAcceptCompactAndPrefersOmitFedora() throws Exception {
        final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

        final URI jsonldResource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(this.getClass().getResourceAsStream("/compact-uri.json"), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .get(jsonldResource)
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").preferRepresentation(Arrays.asList(), Arrays.asList(URI.create("http://fedora.info/definitions/v4/repository#ServerManaged")))
                .perform()) {

            final String body = IOUtils.toString(response.getBody(), UTF_8);
            assertCompact(body);

            assertNotNull(response.getHeaderValue("X-CREATED"));
            assertNotNull(response.getHeaderValue("X-MODIFIED"));
        }
    }

    @Test
    public void compactionPostTest() throws Exception {
        final HttpPost post = new HttpPost(URI.create(fcrepoBaseURI));
        post.setEntity(new InputStreamEntity(this.getClass().getResourceAsStream("/compact-uri.json"), ContentType
                .create("application/ld+json")));
        post.setHeader("Accept", "application/ld+json");
        post.setHeader("Prefer", "return=representation");

        assertCompact(client.execute(post, r -> {
            assertSuccess(r);
            return EntityUtils.toString(r.getEntity());
        }));

    }

    @Test
    public void httpHeadTest() throws Exception {
        final FcrepoClient client = new FcrepoClientBuilder().throwExceptionOnFailure().build();

        final URI jsonldResource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(this.getClass().getResourceAsStream("/compact-uri.json"), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .head(jsonldResource)
                .perform()) {
            assertFalse(response.getLinkHeaders("type").isEmpty());
        }
    }

    static void assertSuccess(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() > 299) {
            try {
                final String message = EntityUtils.toString(response.getEntity());
                fail("Http request failed: " + response.getStatusLine() + "; " + message);
            } catch (final IOException e) {
                fail("Http request failed: " + response.getStatusLine());
            }
        }
    }
}
