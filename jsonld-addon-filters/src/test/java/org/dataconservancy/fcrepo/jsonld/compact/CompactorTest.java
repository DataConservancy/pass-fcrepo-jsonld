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

import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.getUncompactedJsonld;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.isCompact;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldUtil.addStaticContext;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class CompactorTest {

    @Test
    public void compactorTest() throws Exception {

        final URL CONTEXT_URL = new URL("http://example.org/compactorTest/farm.jsonld");

        final JsonLdOptions options = new JsonLdOptions();

        addStaticContext(CONTEXT_URL, this.getClass().getResourceAsStream("/context.jsonld"), options);

        final Compactor compactor = new Compactor();
        compactor.setOptions(options);

        final String JSON = getUncompactedJsonld();

        assertTrue(isCompact(compactor.compact(JSON, CONTEXT_URL)));
    }
}
