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
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.getValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Substitutes text found in request bodies, urlencoded form POSTS, or request query parameters.
 * <p>
 * Can be configured to selectively substitute based on incoming host, or content-type.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class SubstitutionRequestFilter implements Filter {

    static final Logger LOG = LoggerFactory.getLogger(SubstitutionRequestFilter.class);

    static final String SUBSTITUTION_REQUEST_HOST = "request.substitute.filter.host";

    static final String SUBSTITUTION_REQUEST_TERM = "request.substitute.term";

    static final String SUBSTITUTION_REQUEST_REPLACEMENT = "request.substitute.replacement";

    String term;

    String replacement;

    String host;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        LOG.info("Initializing substitution filter");

        host = Optional.ofNullable(filterConfig.getInitParameter("filter-host")).orElse(getValue(
                SUBSTITUTION_REQUEST_HOST));

        term = Optional.ofNullable(filterConfig.getInitParameter("term")).orElse(getValue(
                SUBSTITUTION_REQUEST_TERM));
        replacement = Optional.ofNullable(filterConfig.getInitParameter("replacement")).orElse(getValue(
                SUBSTITUTION_REQUEST_REPLACEMENT));

        if (term != null) {
            LOG.info("Searching for term {}", term);

            if (replacement != null) {
                LOG.info("Replacing with {}", replacement);
            }
        }

        if (host != null) {
            LOG.info("Applying substitutions only from host " + host);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        LOG.debug("Do filter");

        if (term == null || host != null && !host.equals(((HttpServletRequest) request).getHeader("host"))) {
            LOG.debug("Skipping processing.  Term: {}, host: {}, desired host: {}", term, host,
                    ((HttpServletRequest) request).getHeader("host"));
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest req = new ParamReplacingWrapper((HttpServletRequest) request);

        final String method = req.getMethod();

        final String contentType = Optional.ofNullable(req.getHeader(
                "content-type")).orElse(Optional.ofNullable(req.getContentType()).orElse(""));

        if ("POST".equals(method) && contentType.contains("urlencoded")) {
            LOG.debug("POST urlencoded");
            chain.doFilter(new BodyReplacingWrapper(
                    req,
                    body -> encode(decode(body).replace(term, replacement))), response);
        } else if ("POST".equals(method)) {
            LOG.debug("POST no urlencode");
            chain.doFilter(new BodyReplacingWrapper(
                    req,
                    body -> body.replace(term, replacement)), response);
        } else {
            LOG.debug("Nothing, method: " + method);
            chain.doFilter(req, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    private static String encode(String content) {
        try {
            return URLEncoder.encode(content, UTF_8.toString());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String decode(String content) {
        try {
            return URLDecoder.decode(content, UTF_8.toString());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class BodyReplacingWrapper extends HttpServletRequestWrapper {

        private final Function<String, String> replacer;

        public BodyReplacingWrapper(HttpServletRequest request, Function<String, String> replacer) {
            super(request);
            this.replacer = replacer;
        }

        @SuppressWarnings("resource")
        @Override
        public ServletInputStream getInputStream() throws IOException {

            final ServletInputStream delegate = super.getInputStream();

            final AtomicReference<ByteArrayInputStream> translated = new AtomicReference<>();

            return new ServletInputStream() {

                @Override
                public int read() throws IOException {

                    if (translated.get() == null) {
                        final String content = IOUtils.toString(delegate, UTF_8);
                        final String replaced = replacer.apply(content);
                        translated.set(new ByteArrayInputStream(
                                replaced.getBytes(UTF_8)));
                        LOG.debug("Raw body: " + content);
                        LOG.debug("Translated: " + replaced);
                    }
                    return translated.get().read();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    delegate.setReadListener(readListener);
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            };
        }
    }

    private class ParamReplacingWrapper extends HttpServletRequestWrapper {

        /**
         * @param request
         */
        public ParamReplacingWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String paramName) {
            final String value = super.getParameter(paramName);
            if (value != null) {
                return value.replace(term, replacement);
            }
            return value;
        }

        @Override
        public String[] getParameterValues(String paramName) {
            final String values[] = super.getParameterValues(paramName);
            if (values != null) {
                for (int index = 0; index < values.length; index++) {
                    if (values[index] != null) {
                        values[index] = values[index].replace(term, replacement);
                    }
                }
            }
            return values;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            final Map<String, String[]> paramMap = super.getParameterMap();
            if (paramMap != null) {
                for (final String[] values : paramMap.values()) {
                    for (int index = 0; index < values.length; index++) {
                        if (values[index] != null) {
                            values[index] = values[index].replace(term, replacement);
                        }
                    }
                }
            }
            return paramMap;
        }

    }
}
