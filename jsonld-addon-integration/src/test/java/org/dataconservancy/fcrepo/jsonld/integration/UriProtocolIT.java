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
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.getUncompactedJsonld;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class UriProtocolIT implements FcrepoIT {

    static final CloseableHttpClient c = HttpClients.createDefault();

    static final String sslBaseUri = fcrepoBaseURI.replace("http:", "https:");

    @Test
    public void sslPostIT() {

        final URI resourceUri = createResource(true);

        assertTrue("Location does not start with https", resourceUri.getScheme().equals("https"));

    }

    @Test
    public void noSSLPostIT() {

        final URI resourceUri = createResource(false);

        assertTrue("Location should start with http", resourceUri.getScheme().equals("http"));

    }

    @Test
    public void sslGetIT() {
        final URI resourceUri = createResource(false);

        final String body = getBody(resourceUri, true);

        assertTrue("Body didn't contain https baseuri", body.contains(sslBaseUri));
        assertFalse("Body contains non-ssl URIs", body.contains(fcrepoBaseURI));
    }

    @Test
    public void noSSLIT() {
        final URI resourceUri = createResource(false);

        final String body = getBody(resourceUri, false);

        assertFalse("Body contained an unwanted https baseuri", body.contains(sslBaseUri));
        assertTrue("Body doesn't contain the baseuri", body.contains(fcrepoBaseURI));
    }

    private String getBody(URI resource, boolean ssl) {
        return attempt(1, () -> {
            final HttpGet get = new HttpGet(resource);
            get.setHeader("Accept", "application/n-triples");

            if (ssl) {
                get.setHeader("X-Forwarded-Proto", "https");
            }

            try (CloseableHttpResponse r = c.execute(get)) {
                return IOUtils.toString(r.getEntity().getContent(), UTF_8);
            }
        });
    }

    private URI createResource(boolean https) {
        return attempt(1, () -> {

            final HttpPost post = new HttpPost(fcrepoBaseURI);
            post.setEntity(EntityBuilder.create()
                    .setBinary(getUncompactedJsonld().getBytes(UTF_8))
                    .setContentType(ContentType.create("application/ld+json"))
                    .build());

            if (https) {
                post.setHeader("X-Forwarded-Proto", "https");
            }

            try (CloseableHttpResponse response = c.execute(post)) {
                return URI.create(response.getFirstHeader("Location").getValue());
            }
        });
    }
}
