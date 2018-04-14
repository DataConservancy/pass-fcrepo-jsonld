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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author apb@jhu.edu
 */
public class ContextUtil {

    static final ObjectMapper mapper = new ObjectMapper();

    public static String stripContext(String json) {

        try {

            final ObjectNode parsed = (ObjectNode) mapper.readTree(json);
            parsed.remove("@context");

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ContextReady replaceContextFrom(String json) throws Exception {
        try {
            return new ContextReady((ObjectNode) mapper.readTree(json));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ContextReady {

        private final ObjectNode doc;

        public ContextReady(ObjectNode doc) {
            this.doc = doc;
        }

        public String withContext(String context) {

            try {

                doc.replace("@context", new TextNode(context));
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
