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

package org.dataconservancy.fcrepo.jsonld.test;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public abstract class JsonMergePatchTests {

    static JsonLdOptions options = new JsonLdOptions();

    protected static final String TEST_RESOURCE_ID = "test:123";

    protected static final String INITIAL = "{ " +
            "\"@id\": \"test:123\", " +
            "\"@type\": \"Cow\", " +
            "\"healthy\": true, " +
            "\"milkVolume\": 100.6, " +
            "\"barn\": \"test:/barn\", " +
            "\"birthDate\": \"1980-03-20T21:25:43.511Z\", " +
            "\"calves\": [\"test:/1\", \"test:2\"], " +
            "\"@context\": \"http://example.org/farm\"" +
            "}";

    protected abstract void assertPatch(String jsomMergePatch, String expectedJson);

    protected abstract void doPatch(String jsonMergePatch);

    @Test
    public void identityTest() throws Exception {

        assertPatch(INITIAL, INITIAL);
    }

    @Test
    public void noOpTest() throws Exception {

        final String noop = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        assertPatch(noop, INITIAL);
    }

    @Test
    public void deleteAttributeTest() throws Exception {

        final String deleteHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"healthy\": null, " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        final String expected = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"birthDate\": \"1980-03-20T21:25:43.511Z\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        assertPatch(deleteHealthy, expected);
    }

    @Test
    public void addAttributeTest() throws Exception {

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"name\": \"bessie\", " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        final String expected = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"name\": \"bessie\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"birthDate\": \"1980-03-20T21:25:43.511Z\", " +
                "\"calves\": [\"test:/1\", \"test:2\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        assertPatch(addHealthy, expected);
    }

    @Test
    public void addToListTest() throws Exception {

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"calves\": [\"test:/1\", \"test:2\", \"test:3\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        final String expected = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"birthDate\": \"1980-03-20T21:25:43.511Z\", " +
                "\"calves\": [\"test:/1\", \"test:2\", \"test:3\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        assertPatch(addHealthy, expected);
    }

    @Test
    public void removeFromListTest() throws Exception {

        final String addHealthy = "{ " +
                "\"@id\": \"test:123\", " +
                "\"calves\": [\"test:/1\"], " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        final String expected = "{ " +
                "\"@id\": \"test:123\", " +
                "\"@type\": \"Cow\", " +
                "\"healthy\": true, " +
                "\"milkVolume\": 100.6, " +
                "\"barn\": \"test:/barn\", " +
                "\"calves\": [\"test:/1\"], " +
                "\"birthDate\": \"1980-03-20T21:25:43.511Z\", " +
                "\"@context\": \"http://example.org/farm\"" +
                "}";

        assertPatch(addHealthy, expected);
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

        try {
            doPatch(noContext);
            fail("Should have thrown an exception");
        } catch (final Exception e) {
            // good;
        }
    }
}
