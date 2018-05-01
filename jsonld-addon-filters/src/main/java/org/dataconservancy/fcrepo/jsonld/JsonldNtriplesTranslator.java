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

import static com.github.jsonldjava.utils.JsonUtils.fromString;
import static org.dataconservancy.fcrepo.jsonld.ContextUtil.getContext;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.RDFDatasetUtils;
import com.github.jsonldjava.core.RemoteDocument;
import com.rits.cloning.Cloner;

/**
 * @author apb@jhu.edu
 */
public class JsonldNtriplesTranslator {

    List<String> internalPrefixes = Arrays.asList("@id", "@type", "@context");

    static final String NULL_RELATIVE = "null::" + UUID.randomUUID() + "::";

    private final Cloner cloner = new Cloner();

    private final JsonLdOptions options;

    private final boolean strict;

    ObjectMapper mapper = new ObjectMapper();

    public JsonldNtriplesTranslator(JsonLdOptions options, boolean strict) {
        this.options = cloner.deepClone(options);
        this.options.format = "application/nquads";
        this.options.setBase(NULL_RELATIVE);
        this.strict = strict;
    }

    public String translate(String jsonld) {

        URI.create(NULL_RELATIVE);
        try {
            if (strict) {
                verify(jsonld);
            }

            return ((String) JsonLdProcessor.toRDF(fromString(jsonld), RDFDatasetUtils::toNQuads, options))
                    .replaceAll(NULL_RELATIVE, "");
        } catch (JsonLdError | IOException e) {
            throw new BadRequestException("Could not parse jsonld: " + e.getMessage(), e);
        }
    }

    void verify(String jsonld) {
        final ObjectNode parsedJsonld;
        try {
            parsedJsonld = (ObjectNode) mapper.readTree(jsonld);
        } catch (final IOException e) {
            throw new BadRequestException("Could not parse request", e);
        }

        final Set<String> terms = new HashSet<>(getContext(parsedJsonld, options).getPrefixes(false).keySet());

        addAliases(terms, parsedJsonld, options);

        for (final String fieldName : (Iterable<String>) () -> parsedJsonld.fieldNames()) {
            if (!terms.contains(fieldName) && !internalPrefixes.contains(fieldName)) {
                throw new BadRequestException("Unknown attribute " + fieldName);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addAliases(Set<String> keys, ObjectNode parsedJson, JsonLdOptions options) {
        if (parsedJson.get("@context").isTextual()) {
            final RemoteDocument doc = options.getDocumentLoader().loadDocument(parsedJson.get("@context")
                    .textValue());
            final Map<String, Object> cxt = (Map<String, Object>) ((Map<String, Object>) doc.getDocument()).get(
                    "@context");
            cxt.entrySet().stream()
                    .filter(e -> internalPrefixes.contains(e.getValue()))
                    .map(e -> e.getKey())
                    .forEach(keys::add);

        } else if (parsedJson.get("@context").isObject()) {
            final ObjectNode cxt = (ObjectNode) parsedJson.get("@context");
            for (final String fieldName : (Iterable<String>) () -> cxt.fieldNames()) {
                if (internalPrefixes.contains(cxt.get(fieldName).textValue())) {
                    keys.add(fieldName);
                }
            }
        } else {
            throw new BadRequestException("Could not parse context");
        }
    }
}
