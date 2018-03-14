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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class UriProtocolFilterTest {

    final String ORIGINAL_URI = "http://example.org/original/uri";

    final String HTTPS_URI = "https://example.org/original/uri";

    @Mock
    FilterChain chain;

    @Captor
    ArgumentCaptor<HttpServletRequest> requestCaptor;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Before
    public void setUp() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(ORIGINAL_URI));
    }

    @Test
    public void noProtoTest() throws Exception {

        final UriProtocolFilter toTest = new UriProtocolFilter();

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(ORIGINAL_URI, requestCaptor.getValue().getRequestURL().toString());
    }

    @Test
    public void httpsProtoTest() throws Exception {

        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

        final UriProtocolFilter toTest = new UriProtocolFilter();

        toTest.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertEquals(HTTPS_URI, requestCaptor.getValue().getRequestURL().toString());
    }

}
