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

import static java.lang.String.join;
import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.extract;
import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.props;
import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.removePrefix;
import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.toEnvName;
import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.toPropName;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class ConfigUtilTest {

    @Test
    public void toPropNameTest() {
        final String desiredName = "test.prop.123";

        final String envName = "TEST_PROP_123";
        final String propName = "test.prOp.123";

        assertEquals(desiredName, toPropName(propName));
        assertEquals(desiredName, toPropName(envName));
    }

    @Test
    public void toEnvNameTest() {
        final String desiredName = "TEST_PROP_123";

        final String envName = "TEST_PROP_123";
        final String propName = "test.prOp.123";

        assertEquals(desiredName, toEnvName(propName));
        assertEquals(desiredName, toEnvName(envName));
    }

    @Test
    public void removePrefixTest() {
        final String PREFIX = "the.prefix";
        final String KEY = "the.key";

        assertEquals(KEY, removePrefix(PREFIX, join(",", PREFIX, KEY)));
    }

    @Test
    public void extractPropsTest() {
        final String PREFIX = "my.prefix";
        final String KEY1 = "key1";
        final String KEY2 = "key2";
        final String VALUE1 = "myValue1";
        final String VALUE2 = "myValue2";
        System.setProperty(join(".", PREFIX, KEY1), VALUE1);
        System.setProperty(join(".", PREFIX, KEY2), VALUE2);
        System.setProperty("does.not.match", "bogus");

        final Map<String, String> extracted = extract(props(), PREFIX);

        assertEquals(2, extracted.size());
        assertEquals(VALUE1, extracted.get(KEY1));
        assertEquals(VALUE2, extracted.get(KEY2));

    }

}
