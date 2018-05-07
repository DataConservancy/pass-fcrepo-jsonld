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

package org.dataconservancy.fcrepo.jsonld.deserialize;

import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_PERSIST_CONTEXT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.JSONLD_STRICT;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.getValue;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.loadContexts;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataconservancy.fcrepo.jsonld.BadRequestException;
import org.dataconservancy.fcrepo.jsonld.JsonldNtriplesTranslator;
import org.dataconservancy.fcrepo.jsonld.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * @author apb@jhu.edu
 */
public class DeserializationFilter implements Filter {

    JsonldNtriplesTranslator translator;

    private static final Logger LOG = LoggerFactory.getLogger(DeserializationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LogUtil.adjustLogLevels();

        LOG.info("Initializing JSON-LD deserialiation");

        final JsonLdOptions options = new JsonLdOptions();

        loadContexts(options);

        boolean strict = false;
        if (getValue(JSONLD_STRICT) != null && !getValue(JSONLD_STRICT).equals("false")) {
            strict = true;
        }

        boolean persistContexts = false;
        if (getValue(JSONLD_PERSIST_CONTEXT) != null && !getValue(JSONLD_PERSIST_CONTEXT).equals("false")) {
            persistContexts = true;
        }
        translator = new JsonldNtriplesTranslator(options, strict, persistContexts);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final HttpServletResponse resp = (HttpServletResponse) response;

        LOG.debug("Deserialization filter considering the request");

        final String method = ((HttpServletRequest) request).getMethod();
        final String contentType = Optional.ofNullable(
                request.getContentType()).orElse(Optional.ofNullable(((HttpServletRequest) request).getHeader(
                        "content-type")).orElse(""));

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) &&
                contentType.contains("application/ld+json")) {
            try {
                LOG.debug("Deserialization filter is deserializing JSON-LD");
                chain.doFilter(new DeserializationWrapper((HttpServletRequest) request, translator), response);
            } catch (final BadRequestException e) {
                resp.setStatus(400);
                try (Writer out = resp.getWriter()) {
                    out.write(e.getMessage());
                }
                LOG.warn("Bad request", e);
            }
        } else {
            LOG.debug("Deserialization filter is doing nothing: " + method + ", " + contentType);
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}
