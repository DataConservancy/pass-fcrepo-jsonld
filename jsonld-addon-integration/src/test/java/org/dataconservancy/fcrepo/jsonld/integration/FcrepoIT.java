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

package org.dataconservancy.fcrepo.jsonld.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;

/**
 * @author apb@jhu.edu
 */
public interface FcrepoIT {

    static final String fcrepoBaseURI = String.format("http://localhost:%s/%s/rest/", System.getProperty(
            "fcrepo.dynamic.test.port", "8080"), System.getProperty("fcrepo.cxtPath", "fcrepo"));

    default <T> T attempt(final int times, final Callable<T> it) {

        Throwable caught = null;

        for (int tries = 0; tries < times; tries++) {
            try {
                return it.call();
            } catch (final Throwable e) {
                caught = e;
                try {
                    Thread.sleep(1000);
                    System.out.println(".");
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        throw new RuntimeException("Failed executing task", caught);
    }

    static void assertSuccess(FcrepoResponse response) throws IOException {
        if (response.getStatusCode() > 299) {
            final String message = IOUtils.toString(response.getBody(), UTF_8);
            fail("Fcrepo request failed, " + response.getStatusCode() + ": " + message);
        }
    }
}
