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

import static org.dataconservancy.fcrepo.jsonld.ContextUtil.PREDICATE_HAS_CONTEXT;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.addStaticContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;

import com.github.jsonldjava.core.JsonLdOptions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.update.UpdateAction;
import org.dataconservancy.fcrepo.jsonld.test.JsonMergePatchTests;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchTranslatorTest extends JsonMergePatchTests {

    static JsonLdOptions options = new JsonLdOptions();

    static JsonMergePatchTranslator toTest;

    static JsonldNtriplesTranslator nt;

    @BeforeClass
    public static void loadContext() throws Exception {

        options = new JsonLdOptions();

        addStaticContext(new URL("http://example.org/farm"), JsonMergePatchTranslatorTest.class
                             .getResourceAsStream("/context.jsonld"),
                         options);

        toTest = new JsonMergePatchTranslator(options, false, false);
        nt = new JsonldNtriplesTranslator(options, false, false);
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

        final Model rdf = toModel(INITIAL);

        final Model expected = ModelFactory.createDefaultModel().add(rdf);

        UpdateAction.parseExecute(toTest.toSparql(identitylNoContext, URI.create("http://example.org/farm")),
                                  rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Test
    public void persistContextTest() throws Exception {
        final JsonMergePatchTranslator translatorWithPersistence = new JsonMergePatchTranslator(options, false, true);

        final String input = "{ " +
                             "\"@id\": \"test:123\", " +
                             "\"@type\": \"Cow\", " +
                             "\"healthy\": true, " +
                             "\"milkVolume\": 100.6, " +
                             "\"barn\": \"test:/barn\", " +
                             "\"calves\": [\"test:/1\"], " +
                             "\"@context\": \"http://example.org/farm\"" +
                             "}";

        final Model existing = ModelFactory.createDefaultModel();
        final Property hasContext = existing.createProperty(PREDICATE_HAS_CONTEXT);
        existing.add(
            existing.createResource("test:123"),
            hasContext,
            existing.createResource("test:obsolete"));

        UpdateAction.parseExecute(translatorWithPersistence.toSparql(input, null), existing);

        assertEquals(1, existing.listStatements(null, hasContext, (RDFNode) null).toList().size());
        assertEquals(1, existing.listStatements(null, hasContext, existing.createResource(
            "http://example.org/farm")).toList().size());
    }

    private Model toModel(String jsonld) {
        return ModelFactory.createDefaultModel().read(new StringReader(nt.translate(jsonld)), null,
                                                      "NTriples");
    }

    @Override
    protected void assertPatch(String jsomMergePatch, String expectedJson) {

        final Model rdf = toModel(INITIAL);

        final Model expected = toModel(expectedJson);

        UpdateAction.parseExecute(toTest.toSparql(jsomMergePatch, null), rdf);

        assertTrue(expected.isIsomorphicWith(rdf));
    }

    @Override
    protected void doPatch(String jsonMergePatch) {
        final Model rdf = toModel(INITIAL);
        UpdateAction.parseExecute(toTest.toSparql(jsonMergePatch, null), rdf);

    }
}
