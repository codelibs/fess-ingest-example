/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ingest.example;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;

public class ExampleIngesterTest extends LastaFluteTestCase {

    private ExampleIngester ingester;

    String output = null;

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        ingester = new ExampleIngester() {
            protected void log(final String format, final Object data) {
                output = String.format(format, data);
            }
        };
    }

    @Override
    public void tearDown(TestInfo testInfo) throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown(testInfo);
    }

    @Test
    public void test_ds() {
        output = null;
        Map<String, Object> target = new HashMap<>();
        DataStoreParams params = new DataStoreParams();
        target.put("aaa", "111");
        ingester.process(target, params);
        assertEquals("DATASTORE CRAWL: {aaa=111}", output);
    }

    @Test
    public void test_wf() {
        // TODO
        assertTrue(true);
    }
}
