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

package org.dataconservancy.fcrepo.jsonld;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.addStaticContext;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class JsonldNtriplesTranslatorTest {

    static JsonLdOptions options;

    @BeforeClass
    public static void setUp() throws Exception {

        options = new JsonLdOptions();

        addStaticContext(new URL("http://example.org/farm.jsonld"), JsonldNtriplesTranslatorTest.class
                .getResourceAsStream("/context.jsonld"),
                options);

        addStaticContext(new URL("http://example.org/farm-aliased.jsonld"), JsonMergePatchTranslatorTest.class
                .getResourceAsStream("/context-aliased.jsonld"),
                options);
    }

    /* Verifies that the null relative URI is OK */
    @Test
    public void nullRelativeIdTest() throws Exception {

        final JsonldNtriplesTranslator t = new JsonldNtriplesTranslator(options, false);

        final String JSON = IOUtils.toString(this.getClass().getResourceAsStream("/null-relative.json"), UTF_8);

        final Model desiredTriples = ModelFactory.createDefaultModel();
        desiredTriples.read(this.getClass().getResourceAsStream("/null-relative.ttl"), null, "TTL");

        final Model actualTriples = ModelFactory.createDefaultModel();
        actualTriples.read(new StringReader(t.translate(JSON)), null, "TTL");

        assertTrue(desiredTriples.isIsomorphicWith(actualTriples));

    }

    /* JSON-LD with URI context */
    @Test
    public void linkedContextTest() throws Exception {

        final JsonldNtriplesTranslator t = new JsonldNtriplesTranslator(options, false);

        final String JSON = IOUtils.toString(this.getClass().getResourceAsStream("/uri-context.json"), UTF_8);

        final Model desiredTriples = ModelFactory.createDefaultModel();
        desiredTriples.read(this.getClass().getResourceAsStream("/file.nt"), null, "N-Triples");

        final Model actualTriples = ModelFactory.createDefaultModel();
        actualTriples.read(new StringReader(t.translate(JSON)), null, "N-Triples");
    }

    @Test
    public void validationUnexpectedFieldTest() throws Exception {
        final JsonldNtriplesTranslator validating = new JsonldNtriplesTranslator(options, true);
        final JsonldNtriplesTranslator nonValidating = new JsonldNtriplesTranslator(options, false);

        final String unexpected = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"unexpectedProperty\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        try {
            validating.translate(unexpected);
            fail("Should have thrown a valiation error");
        } catch (final BadRequestException e) {
            // expected
        }

        // Should not fail
        nonValidating.translate(unexpected);
    }

    @Test
    public void validationAliasTest() throws Exception {
        final JsonldNtriplesTranslator validating = new JsonldNtriplesTranslator(options, true);

        final String badAlias = "{ " +
                "\"id\": \"test:123\", " +
                "\"type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        final String goodAlias = "{ " +
                "\"id\": \"test:123\", " +
                "\"type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm-aliased.jsonld\"" +
                "}";

        try {
            validating.translate(badAlias);
            fail("Should have thrown a valiation error");
        } catch (final BadRequestException e) {
            // expected
        }

        // Should not fail
        validating.translate(goodAlias);
    }

    @Test
    public void noIdTest() {
        final JsonldNtriplesTranslator validating = new JsonldNtriplesTranslator(options, true);
        final JsonldNtriplesTranslator nonValidating = new JsonldNtriplesTranslator(options, false);

        final String noId = "{ " +
                "\"@type\": \"Cow\", " +
                "\"unexpectedProperty\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        try {
            validating.translate(noId);
            fail("Should have thrown a valiation error");
        } catch (final BadRequestException e) {
            // expected
        }

        // Should not fail
        nonValidating.translate(noId);
    }
}
