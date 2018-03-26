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

package org.dataconservancy.fcrepo.jsonld.response;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.extract;
import static org.dataconservancy.fcrepo.jsonld.ConfigUtil.props;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Substitutes text found in response bodoes.
 * <p>
 * Can be selectively configured to substitute conditionally based on incoming host, or media type.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class SubstitutionResponseFilter implements Filter {

    static final Logger LOG = LoggerFactory.getLogger(SubstitutionResponseFilter.class);

    final String SUBSTITUTION_RESPONSE_HOST = "response.substitute.filter.host";

    final String SUBSTITUTION_RESPONSE_TYPES = "response.substitute.types";

    final String SUBSTITUTION_RESPONSE_TERM = "response.substitute.term";

    final String SUBSTITUTION_RESPONSE_REPLACEMENT = "response.substitute.replacement";

    Map<String, String> types = new HashMap<>();

    Map<String, String> terms = new HashMap<>();

    Map<String, String> replacements = new HashMap<>();

    Map<String, String> hosts = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        extract(props(), SUBSTITUTION_RESPONSE_HOST)
                .entrySet().stream().forEach(e -> hosts.put(e.getValue(), e.getKey()));

        terms = extract(props(), SUBSTITUTION_RESPONSE_TERM);
        replacements = extract(props(), SUBSTITUTION_RESPONSE_REPLACEMENT);
        types = extract(props(), SUBSTITUTION_RESPONSE_TYPES);

        hosts.entrySet().forEach(host -> LOG.info("{}: Replacing {} with {}",
                host.getKey(),
                terms.get(host.getValue()),
                replacements.get(host.getValue())));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final String host = ((HttpServletRequest) request).getHeader("host");
        final String key = hosts.get(host);

        if (!hosts.containsKey(host) || terms.get(key) == null) {
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, new BodyReplacingFilter((HttpServletResponse) response, body -> body.replace(terms
                .get(key), replacements.get(key)), types.get(key)));
    }

    @Override
    public void destroy() {
    }

    private class BodyReplacingFilter extends HttpServletResponseWrapper {

        ReplacingOutputStream outputWrapper;

        boolean enableReplacement = false;

        final Function<String, String> transform;

        final String mediaTypes;

        /**
         * @param response
         */
        public BodyReplacingFilter(HttpServletResponse response, Function<String, String> transform,
                String mediaTypes) {
            super(response);
            this.transform = transform;
            this.mediaTypes = Optional.ofNullable(mediaTypes).orElse("");
        }

        @SuppressWarnings("resource")
        @Override
        public ServletOutputStream getOutputStream() throws IOException {

            final ServletOutputStream delegate = super.getOutputStream();
            if (outputWrapper == null) {
                outputWrapper = new ReplacingOutputStream(delegate, transform);
            }

            return new ServletOutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (enableReplacement) {
                        outputWrapper.write(b);
                    } else {
                        delegate.write(b);
                    }
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    delegate.setWriteListener(writeListener);
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void flush() throws IOException {
                    outputWrapper.flush();
                }

                @Override
                public void close() throws IOException {
                    if (enableReplacement) {
                        outputWrapper.close();
                    } else {
                        delegate.close();
                    }
                }
            };
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputWrapper == null) {
                getOutputStream();
            }
            return new PrintWriter(outputWrapper);
        }

        @Override
        public void setContentLength(int len) {
            if (len <= 0) {
                super.setContentLength(len);
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            outputWrapper.flush();
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, value);
            if (name.equalsIgnoreCase("content-type") && (mediaTypes.equals("") || mediaTypes.contains(value))) {
                enableReplacement = true;
            }
        }

        @Override
        public void setHeader(String name, String value) {
            super.addHeader(name, value);
            if (name.equalsIgnoreCase("content-type") && (mediaTypes.equals("") || mediaTypes.contains(value))) {
                enableReplacement = true;
            }
        }

        @Override
        public void setContentType(String type) {
            if (mediaTypes.equals("") || mediaTypes.contains(type)) {
                enableReplacement = true;
                super.setContentType(type);
            }
        }
    }

    private class ReplacingOutputStream extends FilterOutputStream {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        final Function<String, String> transform;

        public void reset() {
            out = new ByteArrayOutputStream();
        }

        public ReplacingOutputStream(OutputStream out, Function<String, String> transform) {
            super(out);
            this.transform = transform;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        @Override
        public void flush() throws IOException {
            super.out.write(transform.apply((new String(out.toByteArray(), UTF_8))).getBytes(UTF_8));
            super.out.flush();
            reset();
        }

        @Override
        public void close() throws IOException {
            try {
                flush();
            } finally {
                super.out.close();
            }
        }

    }

}
