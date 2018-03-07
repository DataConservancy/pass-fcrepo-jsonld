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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class CompactingOutputStreamTest {

    @Test
    public void noCompactTest() throws Exception {

        final String INPUT = "this is input";

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Compactor compactor = mock(Compactor.class);
        final URL context = new URL("http://example.org");

        final CompactingOutputStream toTest = new CompactingOutputStream(out, compactor, context);

        toTest.write(INPUT.getBytes(UTF_8));
        toTest.close();

        verifyZeroInteractions(compactor);
        assertEquals(INPUT, new String(out.toByteArray(), UTF_8));
    }

    @Test
    public void compactTest() throws Exception {

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final String INPUT = "this is the input to compact";
        final URL CONTEXT = new URL("http://example.org/context");
        final String COMPACTED = "compaacted";

        final Compactor compactor = mock(Compactor.class);

        when(compactor.compact(eq(INPUT), eq(CONTEXT))).thenReturn(COMPACTED);

        final CompactingOutputStream toTest = new CompactingOutputStream(out, compactor, CONTEXT);
        toTest.enableCompaction();
        toTest.write(INPUT.getBytes(UTF_8));
        toTest.close();

        assertEquals(COMPACTED, new String(out.toByteArray(), UTF_8));

    }

}
