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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
class CompactionWrapper extends HttpServletResponseWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(CompactionWrapper.class);

    private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";

    final CompactingOutputStream compactingOutputStream;

    final ServletOutputStream delegate;

    public CompactionWrapper(HttpServletResponse response, Compactor compactor, URL context) {
        super(response);

        try {
            delegate = super.getOutputStream();
            LOG.debug("Delegate output stream is " + delegate);
            compactingOutputStream = new CompactingOutputStream(delegate, compactor, context);
        } catch (final IOException e) {
            throw new RuntimeException("Could not open response output stream", e);
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                compactingOutputStream.write(b);
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
            public void close() throws IOException {
                LOG.debug("Closing compacting output stream");
                compactingOutputStream.close();
            }
        };
    }

    @Override
    public void setContentLength(int len) {
        LOG.debug("Ignoring content length of {}", len);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(compactingOutputStream);
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if (name.equalsIgnoreCase("content-type") && value.startsWith(JSON_LD_MEDIA_TYPE)) {
            compactingOutputStream.enableCompaction();
        }
    }

    @Override
    public void setHeader(String name, String value) {
        super.addHeader(name, value);
        if (name.equalsIgnoreCase("content-type") && value.startsWith(JSON_LD_MEDIA_TYPE)) {
            compactingOutputStream.enableCompaction();
        }
    }
}
