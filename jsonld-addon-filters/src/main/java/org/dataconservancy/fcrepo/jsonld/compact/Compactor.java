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
import static org.dataconservancy.fcrepo.jsonld.ContextUtil.PREDICATE_HAS_CONTEXT;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dataconservancy.fcrepo.jsonld.JsonldNtriplesTranslator;

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

    private final boolean usePersistedContext;

    private final JsonldNtriplesTranslator translator;

    private final int PREDICATE = 1;

    private final int OBJECT = 2;

    private static final List<String> INTERNAL_ATTRS = Arrays.asList("@id", "@type");

    public Compactor(JsonLdOptions options, boolean limitCompaction, boolean usePersistedContext) {
        this.options = options;
        this.limitCompaction = limitCompaction;
        this.usePersistedContext = usePersistedContext;
        this.translator = new JsonldNtriplesTranslator(options, false, false);
    }

    /**
     * Produce a compact representation of the given jsonld content.
     *
     * @param jsonld The jsonld.
     * @param defaultContext Context URI to use when compacting.
     * @return Compacted JSON-LD.
     * @throws Exception
     */
    public String compact(String jsonld, URL defaultContext) {
        try {

            final String contextUri;
            if (usePersistedContext) {
                contextUri = findPersistedContext(jsonld, defaultContext);
            } else {
                contextUri = defaultContext.toExternalForm();
            }

            final Map<String, Object> cxt = getContext(contextUri);

            final String compacted = JsonUtils.toPrettyString(
                    JsonLdProcessor.compact(fromString(jsonld), cxt, options));

            if (limitCompaction) {
                return stripAttrsNotDefinedInContext(compacted, contextUri, cxt.get("@context"));
            }

            return compacted;
        } catch (final JsonLdError | IOException ex) {
            throw new RuntimeException("Error converting JsonLd", ex);
        }
    }

    private String findPersistedContext(String jsonld, URL defaultContext) {
        for (final String triple : translator.translate(jsonld).split("\n")) {
            final String[] spo = triple.split(" ");
            if (spo[PREDICATE].contains(PREDICATE_HAS_CONTEXT)) {
                final String context = spo[OBJECT].substring(1, spo[OBJECT].indexOf('>'));
                LOG.debug("Found persisted context {}", context);
                return context;
            }
        }

        LOG.info("Did not find persistent context, using default");
        return defaultContext.toExternalForm();
    }

    @SuppressWarnings("unchecked")
    private String stripAttrsNotDefinedInContext(String jsonld, String context, Object parsedAttrs)
            throws IOException {
        final Map<String, Object> parsedJson = (Map<String, Object>) fromString(jsonld);
        final Map<String, Object> attrs = (Map<String, Object>) parsedAttrs;
        final List<String> toRemove = new ArrayList<>();

        for (final String key : parsedJson.keySet()) {
            if (!attrs.containsKey(key) && !INTERNAL_ATTRS.contains(key)) {
                if (!key.equals("@context")) {
                    LOG.debug("Dropping json field {} as it is not in context {}", key, context);
                }
                toRemove.add(key);
            }
        }

        // TODO: Handle aliasing at some point
        if (parsedJson.get("@type") instanceof List) {
            final List<String> types = new ArrayList<>();
            for (final String value : (List<String>) parsedJson.get("@type")) {
                if (attrs.containsKey(value)) {
                    types.add(value);
                }
            }

            if (types.size() == 1) {
                parsedJson.put("@type", types.get(0));
            } else {
                parsedJson.put("@type", types);
            }
        }

        parsedJson.keySet().removeAll(toRemove);

        parsedJson.put("@context", context);

        return JsonUtils.toPrettyString(parsedJson);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getContext(String context) throws JsonLdError {
        return (Map<String, Object>) options.getDocumentLoader().loadDocument(context).getDocument();
    }
}
