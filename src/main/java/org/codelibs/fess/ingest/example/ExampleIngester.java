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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.crawler.entity.AccessResult;
import org.codelibs.fess.crawler.entity.AccessResultData;
import org.codelibs.fess.crawler.entity.ResponseData;
import org.codelibs.fess.crawler.entity.ResultData;
import org.codelibs.fess.crawler.transformer.Transformer;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.ingest.Ingester;
import org.codelibs.fess.util.ComponentUtil;

public class ExampleIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(ExampleIngester.class);

    @Override
    public Map<String, Object> process(final Map<String, Object> target, final DataStoreParams params) {
        log("DATASTORE CRAWL: %s", target);
        return target;
    }

    @Override
    public ResultData process(final ResultData target, final ResponseData responseData) {
        final AccessResult<?> accessResult = ComponentUtil.getComponent("accessResult");
        accessResult.init(responseData, target);
        final AccessResultData<?> accessResultData = accessResult.getAccessResultData();
        final Transformer transformer = ComponentUtil.getComponent(accessResultData.getTransformerName());
        final Object data = transformer.getData(accessResultData);
        log("WEB/FILE CRAWL: %s", data);
        return target;
    }

    protected void log(final String format, final Object data) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format(format, data));
        }
    }
}
