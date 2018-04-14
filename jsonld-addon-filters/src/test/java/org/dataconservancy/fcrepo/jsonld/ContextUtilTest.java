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
import static org.dataconservancy.fcrepo.jsonld.ContextUtil.replaceContextFrom;
import static org.dataconservancy.fcrepo.jsonld.ContextUtil.stripContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class ContextUtilTest {

    @Test
    public void stripContextTest() throws Exception {
        final String in = IOUtils.toString(this.getClass().getResourceAsStream("/compact.json"), UTF_8);

        final String stripped = stripContext(in);

        final Map<String, Object> inputJsonMap = new JSONObject(in).toMap();
        final Map<String, Object> strippedJsonMap = new JSONObject(stripped).toMap();

        // Input should have context, stripped should not
        assertTrue(inputJsonMap.containsKey("@context"));
        assertFalse(strippedJsonMap.containsKey("@context"));

        // They should differ only on context
        inputJsonMap.remove("@context");
        assertEquals(inputJsonMap, strippedJsonMap);
    }

    @Test
    public void replaceContextTest() throws Exception {
        final String in = IOUtils.toString(this.getClass().getResourceAsStream("/compact.json"), UTF_8);

        final String CONTEXT = "http://example.org/context.jsonld";

        final String replaced = replaceContextFrom(in).withContext(CONTEXT);

        final Map<String, Object> inputJsonMap = new JSONObject(in).toMap();
        final Map<String, Object> replacedJsonMap = new JSONObject(replaced).toMap();

        assertTrue(replaced.contains(CONTEXT));

        // They should differ only on context
        inputJsonMap.remove("@context");
        replacedJsonMap.remove("@context");
        assertEquals(inputJsonMap, replacedJsonMap);
    }
}
