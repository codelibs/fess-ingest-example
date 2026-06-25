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

import org.codelibs.fess.crawler.entity.ResponseData;
import org.codelibs.fess.crawler.entity.ResultData;
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
        final Map<String, Object> target = new HashMap<>();
        final DataStoreParams params = new DataStoreParams();
        target.put("aaa", "111");

        final Map<String, Object> result = ingester.process(target, params);

        // The same Map instance is mutated in place and returned.
        assertTrue(target == result);
        // The ingester enriches the document with its own marker field.
        assertEquals(ExampleIngester.class.getSimpleName(), result.get(ExampleIngester.INGESTED_BY));
        // The original field is preserved.
        assertEquals("111", result.get("aaa"));
    }

    @Test
    public void test_wf() {
        output = null;

        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://example.com/");
        responseData.setSessionId("test-session");

        final ResultData target = new ResultData();
        target.setTransformerName("textTransformer");
        target.setData("xyz".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        final ResultData result = ingester.process(target, responseData);

        assertEquals(target, result);
        assertEquals("WEB/FILE CRAWL: xyz", output);
    }
}
