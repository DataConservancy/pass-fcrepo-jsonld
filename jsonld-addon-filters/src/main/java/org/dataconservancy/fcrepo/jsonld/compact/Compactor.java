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

import static com.github.jsonldjava.utils.JsonUtils.fromString;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * Compacts jsonld according to a given context.
 *
 * @author apb@jhu.edu
 */
class Compactor {

    Logger LOG = LoggerFactory.getLogger(Compactor.class);

    private final JsonLdOptions options;

    private final boolean limitCompaction;

    private static final List<String> INTERNAL_ATTRS = Arrays.asList("@id", "@type");

    public Compactor(JsonLdOptions options, boolean limitCompaction) {
        this.options = options;
        this.limitCompaction = limitCompaction;
    }

    /**
     * Produce a compact representation of the given jsonld content.
     *
     * @param jsonld The jsonld.
     * @param context Context URI to use when compacting.
     * @return Compacted JSON-LD.
     * @throws Exception
     */
    public String compact(String jsonld, URL context) {
        try {
            final Map<String, Object> cxt = getContext(context);

            System.out.println(jsonld);
            System.out.println(fromString(jsonld));

            final String compacted = JsonUtils.toPrettyString(
                    JsonLdProcessor.compact(fromString(jsonld), cxt, options));

            if (limitCompaction) {
                return stripAttrsNotDefinedInContext(compacted, context, cxt.get("@context"));
            }

            return compacted;
        } catch (final JsonLdError | IOException ex) {
            throw new RuntimeException("Error converting JsonLd", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String stripAttrsNotDefinedInContext(String jsonld, URL context, Object parsedAttrs)
            throws IOException {
        final Map<String, Object> parsedJson = (Map<String, Object>) fromString(jsonld);
        final Map<String, Object> attrs = (Map<String, Object>) parsedAttrs;
        final List<String> toRemove = new ArrayList<>();

        for (final String key : parsedJson.keySet()) {
            if (!attrs.containsKey(key) && !INTERNAL_ATTRS.contains(key)) {
                if (!key.equals("@context")) {
                    LOG.info("Dropping json field {} as it is not in context {}", key, context);
                }
                toRemove.add(key);
            }
        }

        parsedJson.keySet().removeAll(toRemove);

        parsedJson.put("@context", context.toExternalForm());

        return JsonUtils.toPrettyString(parsedJson);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getContext(URL context) throws JsonLdError {
        return (Map<String, Object>) options.getDocumentLoader().loadDocument(context.toExternalForm()).getDocument();
    }
}
