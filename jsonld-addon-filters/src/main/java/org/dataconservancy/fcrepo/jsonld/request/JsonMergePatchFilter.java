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

package org.dataconservancy.fcrepo.jsonld.request;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_PERSIST_CONTEXT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_STRICT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.getValue;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.loadContexts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.github.jsonldjava.core.JsonLdOptions;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.fcrepo.jsonld.BadRequestException;
import org.dataconservancy.fcrepo.jsonld.JsonMergePatchTranslator;
import org.dataconservancy.fcrepo.jsonld.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class JsonMergePatchFilter implements Filter {

    Logger LOG = LoggerFactory.getLogger(JsonMergePatchFilter.class);

    static final String JSON_MERGE_PATCH = "application/merge-patch+json";

    static final String SPARQL_UPDATE = "application/sparql-update";

    JsonMergePatchTranslator translator;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LogUtil.adjustLogLevels();

        LOG.info("Initializing JSON Merge Patch Filter");

        final JsonLdOptions options = new JsonLdOptions();

        loadContexts(options);

        boolean strict = false;
        if (getValue(JSONLD_STRICT) != null && !getValue(JSONLD_STRICT).equals("false")) {
            LOG.info("Using strict JSON-LD");
            strict = true;
        }

        boolean persistContexts = false;
        if (getValue(JSONLD_PERSIST_CONTEXT) != null && !getValue(JSONLD_PERSIST_CONTEXT).equals("false")) {
            LOG.info("Will persist PATCHed context");
            persistContexts = true;
        }

        translator = new JsonMergePatchTranslator(options, strict, persistContexts);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;

        final String method = req.getMethod();

        final String contentType = Optional.ofNullable(req.getHeader(
            "content-type")).orElse(Optional.ofNullable(req.getContentType()).orElse(""));

        LOG.debug("Looking at request");
        if ("PATCH".equals(method) && contentType.contains(JSON_MERGE_PATCH)) {
            LOG.debug("Handling PATCH");
            try {
                chain.doFilter(new JsonMergePatchWrapper(req), new JsonMergePatchResponseWrapper(resp));
            } catch (final BadRequestException e) {
                resp.setStatus(400);
                try (Writer out = resp.getWriter()) {
                    out.write(e.getMessage());
                }
                LOG.warn("Bad request", e);
            }
        } else {
            LOG.debug("Not a json merge patch, ignoring");
            chain.doFilter(request, new JsonMergePatchResponseWrapper(resp));
        }
    }

    @Override
    public void destroy() {
        // nothing
    }

    class JsonMergePatchWrapper extends HttpServletRequestWrapper {

        final ByteArrayInputStream translated;

        final int length;

        public JsonMergePatchWrapper(HttpServletRequest request) {
            super(request);
            try (InputStream origInput = super.getInputStream()) {
                final String input = IOUtils.toString(origInput, UTF_8);
                LOG.debug("Got input JSON patch:\n{}", input);
                final String sparql = translator.toSparql(input, null);

                final byte[] sparqlBody = sparql.getBytes(UTF_8);
                LOG.debug("Translated to sparql/update:\n " + sparql);
                length = sparqlBody.length;
                translated = new ByteArrayInputStream(sparqlBody);
            } catch (final IOException e) {
                throw new RuntimeException("Could not read request body", e);
            }
        }

        @SuppressWarnings("resource")
        @Override
        public ServletInputStream getInputStream() throws IOException {

            final ServletInputStream origInput = super.getInputStream();

            return new ServletInputStream() {

                @Override
                public int read() throws IOException {
                    return translated.read();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    origInput.setReadListener(readListener);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public boolean isFinished() {
                    return !(translated.available() > 0);
                }
            };
        }

        @Override
        public int getContentLength() {
            return length;
        }

        @Override
        public long getContentLengthLong() {
            return length;
        }

        @Override
        public String getHeader(String name) {
            if (name.equalsIgnoreCase("content-type")) {
                return SPARQL_UPDATE;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.equalsIgnoreCase("content-type")) {
                return Collections.enumeration(Arrays.asList(SPARQL_UPDATE));
            }
            return super.getHeaders(name);
        }

        @Override
        public String getContentType() {
            return SPARQL_UPDATE;
        }
    }

    class JsonMergePatchResponseWrapper extends HttpServletResponseWrapper {

        public JsonMergePatchResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if (name.equalsIgnoreCase("accept-patch")) {
                super.addHeader(name, value + ", " + JSON_MERGE_PATCH);
            } else {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            super.addHeader(name, value);
            if (name.equalsIgnoreCase("accept-patch")) {
                super.addHeader(name, value + ", " + JSON_MERGE_PATCH);
            } else {
                super.addHeader(name, value);
            }
        }
    }

}
