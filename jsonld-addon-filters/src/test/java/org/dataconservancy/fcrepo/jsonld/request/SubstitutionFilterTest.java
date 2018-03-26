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
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dataconservancy.fcrepo.jsonld.request.SubstitutionRequestFilter.SUBSTITUTION_REQUEST_REPLACEMENT;
import static org.dataconservancy.fcrepo.jsonld.request.SubstitutionRequestFilter.SUBSTITUTION_REQUEST_TERM;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class SubstitutionFilterTest {

    @Mock
    FilterChain chain;

    @Mock
    FilterConfig config;

    @Captor
    ArgumentCaptor<HttpServletRequest> requestCaptor;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Test
    public void noMatchTest() throws Exception {

        final String INPUT = "this is input";

        System.setProperty(SUBSTITUTION_REQUEST_TERM, "no match");
        System.setProperty(SUBSTITUTION_REQUEST_REPLACEMENT, "blah");

        when(request.getMethod()).thenReturn("POST");
        when(request.getInputStream()).thenReturn(servletStream(toInputStream(INPUT, UTF_8)));

        final SubstitutionRequestFilter toTest = new SubstitutionRequestFilter();
        toTest.init(config);

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(INPUT, IOUtils.toString(requestCaptor.getValue().getInputStream(), UTF_8));
    }

    @Test
    public void noMatchIrlencodedTest() throws Exception {

        final String INPUT = URLEncoder.encode("this=is/input", UTF_8.toString());

        System.setProperty(SUBSTITUTION_REQUEST_TERM, "no match");
        System.setProperty(SUBSTITUTION_REQUEST_REPLACEMENT, "blah");

        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");
        when(request.getInputStream()).thenReturn(servletStream(toInputStream(INPUT, UTF_8)));

        final SubstitutionRequestFilter toTest = new SubstitutionRequestFilter();
        toTest.init(config);

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(INPUT, IOUtils.toString(requestCaptor.getValue().getInputStream(), UTF_8));
    }

    @Test
    public void matchTest() throws Exception {

        final String INPUT = "this is input";
        final String TRANSLATED = "this is awesome";

        System.setProperty(SUBSTITUTION_REQUEST_TERM, "input");
        System.setProperty(SUBSTITUTION_REQUEST_REPLACEMENT, "awesome");

        when(request.getMethod()).thenReturn("POST");
        when(request.getInputStream()).thenReturn(servletStream(toInputStream(INPUT, UTF_8)));

        final SubstitutionRequestFilter toTest = new SubstitutionRequestFilter();
        toTest.init(config);

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(TRANSLATED, IOUtils.toString(requestCaptor.getValue().getInputStream(), UTF_8));
    }

    @Test
    public void matchUrlencodedTest() throws Exception {

        final String INPUT = URLEncoder.encode("this=is/input", UTF_8.toString());
        final String TRANSLATED = URLEncoder.encode("this=is excellent/awesome", UTF_8.toString());

        System.setProperty(SUBSTITUTION_REQUEST_TERM, "is/input");
        System.setProperty(SUBSTITUTION_REQUEST_REPLACEMENT, "is excellent/awesome");

        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");
        when(request.getInputStream()).thenReturn(servletStream(toInputStream(INPUT, UTF_8)));

        final SubstitutionRequestFilter toTest = new SubstitutionRequestFilter();
        toTest.init(config);

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(TRANSLATED, IOUtils.toString(requestCaptor.getValue().getInputStream(), UTF_8));
    }

    private ServletInputStream servletStream(InputStream content) {
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return content.read();
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public void close() throws IOException {
                content.close();
            }
        };
    }
}
