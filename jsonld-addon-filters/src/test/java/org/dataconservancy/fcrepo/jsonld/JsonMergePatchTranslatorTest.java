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

import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.addStaticContext;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchTranslatorTest {

    static JsonLdOptions options = new JsonLdOptions();

    static JsonMergePatchTranslator toTest;

    static JsonldNtriplesTranslator nt;

    static final String initial = "{ " +
            "\"@id\": \"test:123\", " +
            "\"@type\": \"Cow\", " +
            "\"healthy\": true, " +
            "\"milkVolume\": 100.6, " +
            "\"barn\": \"test:/barn\", " +
            "\"calves\": [\"test:/1\", \"test:2\"], " +
            "\"@context\": \"http://example.org/farm.jsonld\"" +
            "}";

    @BeforeClass
    public static void loadContext() throws Exception {

        options = new JsonLdOptions();

        addStaticContext(new URL("http://example.org/farm.jsonld"), JsonMergePatchTranslatorTest.class
                .getResourceAsStream("/context.jsonld"),
                options);

        toTest = new JsonMergePatchTranslator(options, false);
        nt = new JsonldNtriplesTranslator(options, false);
    }

    @Test
    public void identityTest() throws Exception {

        final Model rdf = toModel(initial);

        final Model expected = ModelFactory.createDefaultModel().add(rdf);

        UpdateAction.parseExecute(toTest.toSparql(initial, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void noOpTest() throws Exception {
        final Model rdf = toModel(initial);

        final Model expected = ModelFactory.createDefaultModel().add(rdf);

        final String noop = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        UpdateAction.parseExecute(toTest.toSparql(noop, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void deleteAttributeTest() throws Exception {
        final Model rdf = toModel(initial);

        final String deleteHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"healthy\": null, " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        final Model expected = toModel("{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}");

        UpdateAction.parseExecute(toTest.toSparql(deleteHealthy, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void addAttributeTest() throws Exception {
        final Model rdf = toModel(initial);

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"name\": \"bessie\", " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        final Model expected = toModel("{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"name\": \"bessie\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}");

        UpdateAction.parseExecute(toTest.toSparql(addHealthy, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void addToListTest() throws Exception {
        final Model rdf = toModel(initial);

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"calves\": [\"test:/1\", \"test:2\", \"test:3\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        final Model expected = toModel("{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\", \"test:3\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}");

        UpdateAction.parseExecute(toTest.toSparql(addHealthy, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void removeFromListTest() throws Exception {
        final Model rdf = toModel(initial);

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"calves\": [\"test:/1\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}";

        final Model expected = toModel("{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\"], " +
                "\"@context\": \"http://example.org/farm.jsonld\"" +
                "}");

        UpdateAction.parseExecute(toTest.toSparql(addHealthy, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void defaultContextTest() throws Exception {
        final String identitylNoContext = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"] " +
                "}";

        final Model rdf = toModel(initial);

        final Model expected = ModelFactory.createDefaultModel().add(rdf);

        UpdateAction.parseExecute(toTest.toSparql(identitylNoContext, URI.create("http://example.org/farm.jsonld")),
                rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void noContextProvidedTest() throws Exception {
        final String noContext = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\", \"test:2\"] " +
                "}";

        final Model rdf = ModelFactory.createDefaultModel();

        try {
            UpdateAction.parseExecute(toTest.toSparql(noContext, null),
                    rdf);
            fail("Should have thrown an exception");
        } catch (final BadRequestException e) {
            // good;
        }
    }

    private Model toModel(String jsonld) {
        return ModelFactory.createDefaultModel().read(new StringReader(nt.translate(jsonld)), null,
                "NTriples");
    }
}
