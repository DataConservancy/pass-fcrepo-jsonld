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

import org.dataconservancy.fcrepo.jsonld.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;

/**
 * Servlet filter which compacts responses according to
 *
 * @author apb@jhu.edu
 */
public class CompactionFilter implements Filter {

    public static final String CONTEXT_COMPACTION_URI_PROP = "compaction.uri";

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

            if (compactionWrapper.compactingOutputStream.compactionEnabled) {
                LOG.warn(new String(compactionWrapper.compactingOutputStream.captured.toByteArray()));
                ObjectNode rawJson = new ObjectMapper()
                        .readValue(compactionWrapper.compactingOutputStream.captured.toByteArray(), ObjectNode.class);
            }

            compactionWrapper.getOutputStream().close();
        } catch (final Exception e) {
            LOG.warn("Internal error", e);
            resp.setStatus(500);
            try (Writer out = resp.getWriter()) {
                out.write("Internal error: " + e.getMessage());
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
            } else {
                return orig;
            }
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.equalsIgnoreCase("accept")) {
                return Collections.enumeration(Arrays.asList(getHeader("accept")));
            } else {
                return super.getHeaders(name);
            }
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final Enumeration<String> origNames = super.getHeaderNames() == null ? Collections.enumeration(Collections
                    .emptyList()) : super.getHeaderNames();
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
}
