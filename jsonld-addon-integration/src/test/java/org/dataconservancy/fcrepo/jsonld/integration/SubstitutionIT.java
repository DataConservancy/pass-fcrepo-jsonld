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

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author apb@jhu.edu
 */
public class SubstitutionIT implements FcrepoIT {

    static final String HOST_FOR_SUBSTITUTION = "subst-host";

    static final String HOST_NOT_FOR_SUBSTITUTION = "some-other";

    @Rule
    public TestName name = new TestName();

    static final CloseableHttpClient c = HttpClients.createDefault();

    static final String fusekiBaseURI = String.format("http://localhost:%s/fuseki/", System.getProperty(
            "fuseki.port", "3030"));

    static final String fusekiURI = fusekiBaseURI + "fcrepo-triple-index";

    static enum Mode {
        REQUEST("x-test-req"),
        RESPONSE("x-test-resp");

        final String pfx;

        Mode(String pfx) {
            this.pfx = pfx;
        }

        String prefix() {
            return pfx;
        }
    }

    public String depositTestData(Mode mode, String host) throws Exception {
        final String subject = mode.prefix() + ":/" + name.getMethodName();
        final HttpPost post = new HttpPost(fusekiURI);
        post.setHeader("Host", host);
        post.setEntity(EntityBuilder.create()
                .setBinary(String.format("INSERT DATA {<%s> <%s:predicate> \"hello\" .}", subject, mode.prefix())
                        .getBytes(UTF_8))
                .setContentType(ContentType.create("application/sparql-update"))
                .build());

        return attempt(30, () -> {
            try (CloseableHttpResponse resp = c.execute(post)) {
                System.out.println(fusekiURI);
                System.out.println(resp.getStatusLine());
                assertTrue(resp.getStatusLine().getStatusCode() < 299);
            }

            return subject;
        });
    }

    public String getQuery(String subject) {
        return String.format("SELECT ?p ?o WHERE {<%s> ?p ?o .}", subject);
    }

    void checkBody(HttpUriRequest request, String shouldContain, String shouldNotContain) throws Exception {
        try (CloseableHttpResponse resp = c.execute(request)) {
            assertTrue(resp.getStatusLine().getStatusCode() < 299);
            final String body = EntityUtils.toString(resp.getEntity());

            if (shouldNotContain != null) {
                assertFalse(body, body.contains(shouldNotContain));
            }

            if (shouldContain != null) {
                assertTrue(body, body.contains(shouldContain));
            }
        }
    }

    @Test
    public void requestQueryParamIT() throws Exception {
        final String subject = depositTestData(Mode.REQUEST, HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req
        get.setHeader("Accept", "application/sparql-results+json");

        checkBody(get, "x-test-repl-req", "x-test-req");
    }

    @Test
    public void requestQueryPostIT() throws Exception {
        final String subject = depositTestData(Mode.REQUEST, HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req

        final HttpPost reqPost = new HttpPost(fusekiURI);
        reqPost.setHeader("HOST", HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req
        reqPost.setEntity(EntityBuilder.create()
                .setBinary(getQuery(subject).getBytes(UTF_8))
                .setContentType(ContentType.create("application/sparql-query"))
                .build());
        reqPost.setHeader("Accept", "application/sparql-results+json");

        checkBody(reqPost, "x-test-repl-req", "x-test-req");
    }

    @Test
    public void requestQueryurlencodedPostIT() throws Exception {

        final String subject = depositTestData(Mode.REQUEST, HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req

        final HttpPost reqPost = new HttpPost(fusekiURI);
        reqPost.setHeader("HOST", HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req
        reqPost.setEntity(EntityBuilder.create()
                .setBinary(("query=" + encode(getQuery(subject), UTF_8.toString())).getBytes(UTF_8))
                .setContentType(ContentType.create("application/x-www-form-urlencoded"))
                .build());
        reqPost.setHeader("Accept", "application/sparql-results+json");

        checkBody(reqPost, "x-test-repl-req", "x-test-req");
    }

    @Test
    public void requestNooSubstitutionTest() throws Exception {
        final String subject = depositTestData(Mode.REQUEST, HOST_NOT_FOR_SUBSTITUTION); // x-test-req -> x-test-req

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_NOT_FOR_SUBSTITUTION); // x-test-req -> x-test-req
        get.setHeader("Accept", "application/sparql-results+json");

        checkBody(get, "x-test-req", "x-test-repl=req");
    }

    @Test
    public void requestFrontBackSubstitutionTest() throws Exception {
        final String subject = depositTestData(Mode.REQUEST, HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_NOT_FOR_SUBSTITUTION); // x-test-req -> x-test-req
        get.setHeader("Accept", "application/sparql-results+json");

        // Should be no matches
        checkBody(get, null, "x-test-repl-req");
        checkBody(get, null, "x-test-req");
    }

    @Test
    public void requestMismatchSubstitutionTest() throws Exception {
        final String subject = depositTestData(Mode.REQUEST, HOST_NOT_FOR_SUBSTITUTION); // x-test-req -> x-test-req

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_FOR_SUBSTITUTION); // x-test-req -> x-test-repl-req
        get.setHeader("Accept", "application/sparql-results+json");

        // Should be no matches
        checkBody(get, null, "x-test-repl-req");
        checkBody(get, null, "x-test-req");
    }

    @Test
    public void responseSubstitutionTest() throws Exception {
        final String subject = depositTestData(Mode.RESPONSE, HOST_FOR_SUBSTITUTION); // x-test-resp

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_FOR_SUBSTITUTION); // x-test-resp
        get.setHeader("Accept", "application/sparql-results+json");

        checkBody(get, "x-test-repl-resp", "x-test-resp"); // x-test-resp -> x-test-resp-repl
    }

    @Test
    public void noResponseSubstitutionTest() throws Exception {
        final String subject = depositTestData(Mode.RESPONSE, HOST_FOR_SUBSTITUTION); // x-test-resp

        final HttpGet get = new HttpGet(fusekiURI + "?query=" + encode(getQuery(subject), UTF_8.toString()));
        get.setHeader("Host", HOST_NOT_FOR_SUBSTITUTION); // x-test-resp
        get.setHeader("Accept", "application/sparql-results+json");

        checkBody(get, "x-test-resp", "x-test-repl-resp"); // x-test-resp
    }

}
