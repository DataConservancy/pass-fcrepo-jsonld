/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class CompactionWrapperTest {

    @Mock
    Compactor compactor;

    @Mock
    HttpServletResponse response;

    URL context = context();

    String INPUT_TEXT = "test input";

    String COMPACTED_TEXT = "compacted test input";

    ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {

        out = new ByteArrayOutputStream();

        when(compactor.compact(eq(INPUT_TEXT), eq(context))).thenReturn(COMPACTED_TEXT);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {

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
                throw new UnsupportedOperationException();
            }
        });
    }

    @Test
    public void noCompacitonOnNonJsonld() throws Exception {
        final CompactionWrapper toTest = new CompactionWrapper(response, compactor, context);
        try (OutputStream o = toTest.getOutputStream()) {
            o.write(INPUT_TEXT.getBytes(UTF_8));
        }

        verifyZeroInteractions(compactor);
        assertEquals(INPUT_TEXT, new String(out.toByteArray(), UTF_8));
    }

    @Test
    public void compactionOnAddHeaderTest() throws Exception {
        final CompactionWrapper toTest = new CompactionWrapper(response, compactor, context);
        toTest.addHeader("Content-Type", "application/ld+json");
        try (OutputStream o = toTest.getOutputStream()) {
            o.write(INPUT_TEXT.getBytes(UTF_8));
        }

        assertEquals(COMPACTED_TEXT, new String(out.toByteArray(), UTF_8));
    }

    @Test
    public void compactionOnSetHeaderTest() throws Exception {
        final CompactionWrapper toTest = new CompactionWrapper(response, compactor, context);
        toTest.setHeader("Content-Type", "application/ld+json");
        try (OutputStream o = toTest.getOutputStream()) {
            o.write(INPUT_TEXT.getBytes(UTF_8));
        }

        assertEquals(COMPACTED_TEXT, new String(out.toByteArray(), UTF_8));
    }

    @Test
    public void printWriterCompactionTest() throws Exception {
        final CompactionWrapper toTest = new CompactionWrapper(response, compactor, context);
        toTest.addHeader("Content-Type", "application/ld+json");
        try (PrintWriter w = toTest.getWriter()) {
            w.print(INPUT_TEXT);
        }

        assertEquals(COMPACTED_TEXT, new String(out.toByteArray(), UTF_8));
    }

    private URL context() {
        try {
            return new URL("http://example.org/context");
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
