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

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.COMPACTION_PROP_PRELOAD_FILES;
import static org.dataconservancy.fcrepo.jsonld.JsonldUtil.COMPACTION_PROP_PRELOAD_URIS;
import static org.dataconservancy.fcrepo.jsonld.compact.CompactionFilter.CONTEXT_COMPACTION_URI_PROP;
import static org.dataconservancy.fcrepo.jsonld.test.JsonldTestUtil.assertCompact;
import static org.dataconservancy.fcrepo.jsonld.test.JsonldTestUtil.getUncompactedJsonld;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataconservancy.fcrepo.jsonld.test.JsonldTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class CompactionFilterTest {

    @Mock
    HttpServletRequest originalRequest;

    @Mock
    HttpServletResponse originalResponse;

    HttpServletResponse filteredResponse;

    FilterChain chain;

    boolean originalOutIsClosed = false;

    ByteArrayOutputStream out;

    final String PRELOAD_URI_PROP = join(".", COMPACTION_PROP_PRELOAD_URIS, "farm.filter.test");

    final String PRELOAD_FILE_PROP = join(".", COMPACTION_PROP_PRELOAD_FILES, "farm.filter.test");

    @Before
    public void setUp() throws Exception {
        this.originalOutIsClosed = false;
        this.out = new ByteArrayOutputStream();
        final ServletOutputStream servletOut = new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void close() {
                originalOutIsClosed = true;
            }
        };

        when(originalResponse.getOutputStream()).thenReturn(servletOut);

        chain = new FilterChain() {

            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                ServletException {
                CompactionFilterTest.this.filteredResponse = (HttpServletResponse) response;
            }
        };
    }

    @After
    public void tearDown() {
        System.clearProperty(PRELOAD_FILE_PROP);
        System.clearProperty(PRELOAD_URI_PROP);
        System.clearProperty(CONTEXT_COMPACTION_URI_PROP);
    }

    @Test
    public void preloadtest() throws Exception {
        final String CONTEXT_URI = "http://example.org/CompactionFilterTest";

        System.setProperty(CONTEXT_COMPACTION_URI_PROP, CONTEXT_URI);
        System.setProperty(PRELOAD_URI_PROP, CONTEXT_URI);
        System.setProperty(PRELOAD_FILE_PROP, getContextFileLocation());

        final CompactionFilter toTest = new CompactionFilter();
        toTest.init(mock(FilterConfig.class));

        toTest.doFilter(originalRequest, originalResponse, chain);

        filteredResponse.setHeader("Content-Type", "application/ld+json");
        try (OutputStream o = filteredResponse.getOutputStream()) {
            o.write(getUncompactedJsonld().getBytes(UTF_8));
        }

        assertTrue(this.originalOutIsClosed);
        assertCompact(new String(out.toByteArray(), UTF_8));
    }

    private static String getContextFileLocation() {
        try {
            return Paths.get(JsonldTestUtil.class.getResource("/preload-context.jsonld").toURI()).toFile()
                        .getAbsolutePath();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
