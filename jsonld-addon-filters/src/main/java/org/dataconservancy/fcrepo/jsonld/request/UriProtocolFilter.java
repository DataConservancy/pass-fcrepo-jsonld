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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author apb@jhu.edu
 */
public class UriProtocolFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final HttpServletRequest req = ((HttpServletRequest) request);

        final Protocol proto = Protocol.of(req);

        if (proto.isDefined()) {
            chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {

                @Override
                public StringBuffer getRequestURL() {
                    return new StringBuffer(super.getRequestURL().toString()
                            .replaceFirst("^https?:", proto.getProtocol() + ":"));
                }
            }, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Nothing to do
    }

    private static class Protocol {

        final String proto;

        static Protocol of(HttpServletRequest request) {
            if (request.getHeader("X-Forwarded-Proto") != null) {
                return new Protocol(request.getHeader("X-Forwarded-Proto"));
            }

            return new Protocol(null);
        }

        private Protocol(String proto) {
            this.proto = proto;
        }

        boolean isDefined() {
            return proto != null;
        }

        String getProtocol() {
            return proto;
        }
    }

}
