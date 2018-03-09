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
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.extract;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.props;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class JsonldUtil {

    static final Logger LOG = LoggerFactory.getLogger(JsonldUtil.class);

    public static final String COMPACTION_PROP_PRELOAD_URIS = "compaction.preload.uri";

    public static final String COMPACTION_PROP_PRELOAD_FILES = "compaction.preload.file";

    public static void loadContexts(JsonLdOptions options) {
        final Map<String, String> contextLocations = extract(props(), COMPACTION_PROP_PRELOAD_FILES);
        final Map<String, String> contextUris = extract(props(), COMPACTION_PROP_PRELOAD_URIS);

        for (final Map.Entry<String, String> entry : contextUris.entrySet()) {
            try (FileInputStream file = new FileInputStream(contextLocations.get(entry.getKey()))) {

                LOG.info("Loading static context for '{}' from file '{}'", entry.getValue(), contextLocations.get(
                        entry.getKey()));
                addStaticContext(new URL(entry.getValue()), file, options);
            } catch (final MalformedURLException urle) {
                LOG.warn("Bad json-ld context URL for preload: '{}' from configuration property ", entry.getValue(),
                        String
                                .join(".", COMPACTION_PROP_PRELOAD_URIS, entry.getKey()));
            } catch (final FileNotFoundException e) {
                LOG.warn("json-ld context File not found at '{}', from configuration property {}", contextLocations
                        .get(entry.getKey()), String.join(".", COMPACTION_PROP_PRELOAD_FILES, entry.getKey()));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void addStaticContext(URL contextUrl, InputStream context, JsonLdOptions options) {
        try (InputStream cxt = context) {
            addStaticContext(contextUrl, IOUtils.toString(cxt, UTF_8), options);
        } catch (final IOException e) {
            throw new RuntimeException("Could read input stream of static jsonld context", e);
        }
    }

    public static void addStaticContext(URL contextUrl, String context, JsonLdOptions options) {
        try {
            options.getDocumentLoader().addInjectedDoc(contextUrl.toExternalForm(), context);
        } catch (final JsonLdError e) {
            throw new RuntimeException("Could not add static jsonld context", e);
        }
    }

}
