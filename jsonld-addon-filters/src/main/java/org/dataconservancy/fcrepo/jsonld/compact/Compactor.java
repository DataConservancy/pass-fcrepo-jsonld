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

    private JsonLdOptions options = new JsonLdOptions();

    public void setOptions(JsonLdOptions options) {
        this.options = options;
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
            return JsonUtils.toString(
                    JsonLdProcessor.compact(fromString(jsonld), getContext(context), options));
        } catch (final JsonLdError | IOException ex) {
            throw new RuntimeException("Error converting JsonLd", ex);
        }
    }

    private Object getContext(URL context) throws JsonLdError {
        return options.getDocumentLoader().loadDocument(context.toExternalForm()).getDocument();
    }
}
