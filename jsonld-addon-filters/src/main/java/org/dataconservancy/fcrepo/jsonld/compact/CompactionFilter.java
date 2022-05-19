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

import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_MINIMAL_CONTEXT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_PERSIST_CONTEXT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.getValue;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.loadContexts;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import org.dataconservancy.fcrepo.jsonld.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter which compacts responses according to configured context.
 *
 * @author apb@jhu.edu
 */
public class CompactionFilter implements Filter {

    public static final String CONTEXT_COMPACTION_URI_PROP = "compaction.uri";

    private static final String JSONLD_VALUE_FIELD = "@value";

    private URL defaultContext;

    private Compactor compactor;

    Logger LOG = LoggerFactory.getLogger(CompactionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LogUtil.adjustLogLevels();

        LOG.info("Initializing compaction filter");

        final String context = Optional.ofNullable(filterConfig.getInitParameter("context")).orElse(getValue(
            CONTEXT_COMPACTION_URI_PROP));

        if (context != null) {
            LOG.info("Compacting responses with context '{}'", context);
            try {
                defaultContext = new URL(context);
            } catch (final MalformedURLException e) {
                throw new ServletException(String.format("Bad context URL '%s'", context), e);
            }
        } else {
            LOG.info("No default context provided, not compacting");
        }

        final JsonLdOptions options = new JsonLdOptions();

        loadContexts(options);

        boolean limitContexts = false;
        if (getValue(JSONLD_MINIMAL_CONTEXT) != null && !getValue(JSONLD_MINIMAL_CONTEXT).equals("false")) {
            limitContexts = true;
        }

        boolean usePersistedContext = false;
        if (getValue(JSONLD_PERSIST_CONTEXT) != null && !getValue(JSONLD_PERSIST_CONTEXT).equals("false")) {
            usePersistedContext = true;
        }

        compactor = new Compactor(options, limitContexts, usePersistedContext);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;

        LOG.debug("Compaction filter is considering response");

        try {
            LOG.debug("Compaction filter will examine response");
            final CompactionWrapper compactionWrapper = new CompactionWrapper(resp,
                                                                              compactor,
                                                                              defaultContext);
            chain.doFilter(new CompactionRequestWrapper(req), compactionWrapper);

            if (compactionWrapper.compactingOutputStream.compactionEnabled
                && compactionWrapper.compactingOutputStream.captured.size() > 0) {
                ObjectNode rawJson = asObject(new ObjectMapper()
                                                  .readValue(
                                                      compactionWrapper.compactingOutputStream.captured.toByteArray(),
                                                      JsonNode.class));

                String lastModified = null;
                String created = null;

                // Put created and last modified properties into headers.
                // Must check for compact and expanded JSON-LD properties.

                ObjectNode lastModifiedObject = asObject(
                    rawJson.get("http://fedora.info/definitions/v4/repository#lastModified"));
                ObjectNode createdObject = asObject(
                    rawJson.get("http://fedora.info/definitions/v4/repository#created"));

                if (lastModifiedObject == null) {
                    if (rawJson.has("lastModified")) {
                        lastModified = rawJson.get("lastModified").asText();
                    }
                } else {
                    if (lastModifiedObject.has(JSONLD_VALUE_FIELD)) {
                        lastModified = lastModifiedObject.get(JSONLD_VALUE_FIELD).asText();
                    }
                }

                if (createdObject == null) {
                    if (rawJson.has("created")) {
                        created = rawJson.get("created").asText();
                    }
                } else {
                    if (createdObject.has(JSONLD_VALUE_FIELD)) {
                        created = createdObject.get(JSONLD_VALUE_FIELD).asText();
                    }
                }

                if (created != null) {
                    resp.addHeader("X-CREATED", created);
                }

                if (lastModified != null) {
                    resp.addHeader("X-MODIFIED", lastModified);
                }
            }

            compactionWrapper.compactingOutputStream.close();
        } catch (final Exception e) {
            LOG.warn("Internal error", e);
            resp.setStatus(500);
            try (Writer out = resp.getWriter()) {
                out.write("Internal error: " + e.getMessage());
            } catch (Exception x) {
                // nothing
            }
        }
    }

    private class CompactionRequestWrapper extends HttpServletRequestWrapper {

        public CompactionRequestWrapper(HttpServletRequest request) {
            super(request);
            getHeaderNames();
        }

        @Override
        public String getHeader(String name) {
            final String orig = super.getHeader(name);
            if (name.equalsIgnoreCase("accept")) {
                if (orig == null || orig.equals("") || orig.equals("*/*")) {
                    final String accepts = "application/ld+json, */*";
                    LOG.debug("Transforming original accept header {} into  {}", orig, accepts);
                    return accepts;
                }
                return orig;
            } else if (name.equalsIgnoreCase("prefer")) {
                // Ignore requests to modify representation since this filter overriding them.
                if (orig != null && orig.startsWith("return=representation;")) {
                    final String prefers = "return=representation";
                    LOG.debug("Transforming original prefer header {} into  {}", orig, prefers);
                    return prefers;
                }
                return orig;
            } else {
                return orig;
            }
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.equalsIgnoreCase("accept")) {
                return Collections.enumeration(Arrays.asList(getHeader("accept")));
            } else if (name.equalsIgnoreCase("prefer")) {
                return Collections.enumeration(Arrays.asList(getHeader("prefer")));
            } else {
                return super.getHeaders(name);
            }
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final Enumeration<String> origNames = super.getHeaderNames() == null ?
                Collections.enumeration(Collections.emptyList()) : super.getHeaderNames();
            final List<String> names = Collections.list(origNames);
            if (!names.contains("accept")) {
                names.add("accept");
            }

            return Collections.enumeration(names);
        }

    }

    @Override
    public void destroy() {
        // nothing
    }

    private static ObjectNode asObject(JsonNode n) {
        if (n != null && n.isArray()) {
            return (ObjectNode) n.get(0);
        }
        return (ObjectNode) n;
    }
}
