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

import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.loadContexts;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.dataconservancy.fcrepo.jsonld.JsonldNtriplesTranslator;

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
        LOG.info("Initializing JSON-LD deserialiation");

        final JsonLdOptions options = new JsonLdOptions();

        loadContexts(options);

        translator = new JsonldNtriplesTranslator();
        translator.setOptions(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        LOG.debug("Deserialization filter considering the request");

        final String method = ((HttpServletRequest) request).getMethod();
        final String contentType = Optional.ofNullable(
                request.getContentType()).orElse(Optional.ofNullable(((HttpServletRequest) request).getHeader(
                        "content-type")).orElse(""));

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) &&
                contentType.contains("application/ld+json")) {
            LOG.debug("Deserialization filter is deserializing JSON-LD");
            chain.doFilter(new DeserializationWrapper((HttpServletRequest) request, translator), response);
        } else {
            LOG.debug("Deserialization filter is doing nothing: " + method + ", " + contentType);
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}
