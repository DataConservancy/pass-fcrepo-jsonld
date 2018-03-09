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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
class DeserializationWrapper extends HttpServletRequestWrapper {

    private final ServletInputStream originalInputStream;

    private final JsonldNtriplesTranslator transltor;

    private static final Logger LOG = LoggerFactory.getLogger(DeserializationWrapper.class);

    /**
     * @param request
     */
    public DeserializationWrapper(HttpServletRequest request, JsonldNtriplesTranslator translator) {
        super(request);
        try {
            this.originalInputStream = request.getInputStream();
            this.transltor = translator;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServletInputStream getInputStream() {

        return new ServletInputStream() {

            final ByteArrayInputStream translatedOutputStream;

            boolean finished = false;

            {
                try (InputStream in = originalInputStream) {

                    final String originalBody = IOUtils.toString(
                            originalInputStream, UTF_8);
                    LOG.debug("Original content: " + originalBody);

                    final String translatedBody = transltor.translate(originalBody);
                    LOG.debug("Translated content: " + translatedBody);
                    translatedOutputStream = new ByteArrayInputStream(translatedBody.getBytes(UTF_8));

                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int read() throws IOException {
                final int byt = translatedOutputStream.read();
                if (byt == -1) {
                    finished = true;
                }
                return byt;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                originalInputStream.setReadListener(readListener);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public void close() throws IOException {
                translatedOutputStream.close();
            }
        };
    }

    @Override
    public String getContentType() {
        return "text/turtle";
    }

    @Override
    public String getHeader(String name) {
        if (name.equalsIgnoreCase("content-type")) {
            return "text/turtle";
        } else {
            return super.getHeader(name);
        }
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (name.equalsIgnoreCase("Content-Type")) {
            return Collections.enumeration(Arrays.asList("text/turtle"));
        } else {
            return super.getHeaders(name);
        }
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }
}
