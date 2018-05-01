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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

/**
 * @author apb@jhu.edu
 */
public class JsonldTestUtil {

    final static Map<String, Object> compacted = Collections.unmodifiableMap(getCompacted());

    public static String getUncompactedJsonld() {
        try {
            return IOUtils.toString(JsonldTestUtil.class.getResourceAsStream(
                    "/uncompacted.json"), UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException("Could not read test JSON-LD", e);
        }
    }

    public static void assertCompact(String jsonld) {
        final Map<String, Object> testJson = redact(new JSONObject(jsonld).toMap());

        ReflectionAssert.assertReflectionEquals(compacted, testJson, ReflectionComparatorMode.LENIENT_ORDER);
    }

    private static Map<String, Object> getCompacted() {
        try {
            return redact(new JSONObject(IOUtils.toString(JsonldTestUtil.class.getResourceAsStream(
                    "/compact.json"), UTF_8)).toMap());
        } catch (final IOException e) {
            throw new RuntimeException("Could not read compacted JSON-LD");
        }
    }

    private static Map<String, Object> redact(Map<String, Object> input) {
        input.remove("@context");
        input.remove("@id");
        input.remove("id");
        return input;
    }
}
