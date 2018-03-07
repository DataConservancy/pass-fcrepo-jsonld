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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies json-ld compaction to the content written to an output stream, if instructed to do so.
 * <p>
 * If {@link #compactionEnabled} is called (before writing content, of course), then anything written to the wrapped
 * OutputStream will be collected in a buffer, compacted, and written to the wrapped buffer upon {@link #close()}.
 * Otherwise, the bytes are passed along unmodified.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class CompactingOutputStream extends FilterOutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(CompactingOutputStream.class);

    final Compactor compactor;

    private boolean compactionEnabled = false;

    private final URL context;

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    /**
     * Wrap the given OutputStream with the given compactor and context URL.
     *
     * @param out OutputStream to wrap
     * @param compactor Compactor to use
     * @param context Context URL. May be null (in which case, no compaction is done).
     */
    public CompactingOutputStream(OutputStream out, Compactor compactor, URL context) {
        super(out);
        this.compactor = compactor;
        this.context = context;
    }

    @Override
    public void write(int b) throws IOException {
        if (compactionEnabled && context != null) {
            captured.write(b);
        } else {
            super.write(b);
        }
    }

    /**
     * Tell the compactor to do JSON-LD compaction when it starts getting input.
     */
    public void enableCompaction() {
        LOG.debug("Enabling compaction");
        this.compactionEnabled = true;
    }

    @Override
    public void close() throws IOException {

        try {
            if (compactionEnabled && context != null) {
                LOG.debug("Doing compaction and writing to {}, which is a {}", super.out, super.out.getClass());
                final String compacted = compactor.compact(new String(captured.toByteArray(), UTF_8), context);
                LOG.debug("Writing compacted jsonld: {}", compacted);
                super.out.write(compacted.getBytes(UTF_8));
            } else {
                LOG.debug("Not doing compaction");
            }
        } catch (final Exception e) {
            throw new RuntimeException("Could not compact jsonld", e);
        } finally {
            super.close();
        }
    }
}
