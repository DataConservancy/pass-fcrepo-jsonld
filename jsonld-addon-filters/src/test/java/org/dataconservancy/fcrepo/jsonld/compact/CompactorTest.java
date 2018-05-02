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

package org.dataconservancy.fcrepo.jsonld.compact;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.addStaticContext;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.assertCompact;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.getUncompactedJsonld;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class CompactorTest {

    static JsonLdOptions options = new JsonLdOptions();

    static URL CONTEXT_URL;

    @BeforeClass
    public static void loadContext() throws Exception {
        CONTEXT_URL = new URL("http://example.org/compactorTest/farm.jsonld");
        addStaticContext(CONTEXT_URL, CompactorTest.class.getResourceAsStream("/context.jsonld"), options);
    }

    @Test
    public void compactJsonldTest() throws Exception {

        final Compactor compactor = new Compactor(options, true, false);

        final String JSON = getUncompactedJsonld();

        assertCompact(compactor.compact(JSON, CONTEXT_URL));
    }

    @Test
    public void dropUnknownAttrTest() throws Exception {
        final Compactor willDrop = new Compactor(options, true, false);
        final Compactor willNotDrop = new Compactor(options, false, false);

        final String hasDataNotInContext = IOUtils.toString(JsonldTestUtil.class.getResourceAsStream(
                "/uncompacted-with-unwanted-data.json"), UTF_8);

        try {
            assertCompact(willNotDrop.compact(hasDataNotInContext, CONTEXT_URL));
            fail("Should have failed compaction comparison");
        } catch (final AssertionError e) {
            System.out.println("OK");
        }

        assertCompact(willDrop.compact(hasDataNotInContext, CONTEXT_URL));
    }

    @Test
    public void persistedContextTest() throws Exception {
        final URL MY_CONTEXT = new URL("http://example.org/farm");
        final JsonLdOptions myOptions = new JsonLdOptions();

        addStaticContext(MY_CONTEXT, CompactorTest.class.getResourceAsStream("/context.jsonld"), myOptions);
        addStaticContext(CONTEXT_URL, CompactorTest.class.getResourceAsStream("/context.jsonld"), myOptions);

        final Compactor withPersist = new Compactor(myOptions, true, true);

        final String jsonWithPersistedContext = IOUtils.toString(JsonldTestUtil.class.getResourceAsStream(
                "/compact-uri-with-persisted-context.json"), UTF_8);

        final String results = withPersist.compact(jsonWithPersistedContext, CONTEXT_URL);
        assertCompact(results);
        assertTrue(results.contains(MY_CONTEXT.toExternalForm()));
        assertFalse(results.contains(CONTEXT_URL.toExternalForm()));
    }
}
