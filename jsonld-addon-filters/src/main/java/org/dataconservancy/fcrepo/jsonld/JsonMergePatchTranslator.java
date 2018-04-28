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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.dataconservancy.fcrepo.jsonld.ContextUtil.getContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchTranslator {

    final ObjectMapper mapper = new ObjectMapper();

    final JsonLdOptions options;

    final JsonldNtriplesTranslator translator;

    List<String> excluded = asList("@context");

    public JsonMergePatchTranslator(JsonLdOptions options, boolean strict) {
        this.options = options;
        translator = new JsonldNtriplesTranslator(options, strict);
    }

    public String toSparql(String jsonld, URI defaultContext) throws BadRequestException {

        final SparqlBuilder builder = new SparqlBuilder();

        final ObjectNode parsedMergePatch;
        try {
            parsedMergePatch = (ObjectNode) mapper.readTree(jsonld);
        } catch (final IOException e) {
            throw new BadRequestException("Could not parse request", e);
        }

        if (parsedMergePatch.get("@context") == null) {
            if (defaultContext == null) {
                throw new BadRequestException("No context provided");
            }
            parsedMergePatch.set("@context", new TextNode(defaultContext.toString()));
        }

        final String jsonldWithContext;
        try {
            jsonldWithContext = mapper.writer().writeValueAsString(parsedMergePatch);
        } catch (final IOException e) {
            throw new BadRequestException("Could not process request", e);
        }

        final Map<String, String> attrs = getContext(parsedMergePatch, options).getPrefixes(false);
        attrs.put("@type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

        for (final String name : (Iterable<String>) () -> parsedMergePatch.fieldNames()) {
            builder.deleteWithPredicate(attrs.get(name));
        }

        builder.addStatements(translator.translate(jsonldWithContext));

        return builder.build();
    }

    class SparqlBuilder {

        StringBuilder additions = new StringBuilder("INSERT { \n");

        StringBuilder subtractions = new StringBuilder("DELETE { \n");

        public void addStatements(String ntStatements) {
            additions.append(ntStatements);
        }

        public void deleteWithPredicate(String predicate) {
            if (predicate != null) {
                subtractions.append(format("?s <%s> ?o .\n", predicate));
            }
        }

        public String build() {
            subtractions.append("}\n");
            additions.append("}\n");

            return subtractions.toString() + additions.toString() + "WHERE {?s ?p ?o}";
        }
    }
}
