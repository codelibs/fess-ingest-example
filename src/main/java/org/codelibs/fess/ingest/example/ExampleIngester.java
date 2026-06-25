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

/**
 * An example implementation of {@link Ingester} that enriches crawled documents
 * before they are indexed.
 *
 * <p>
 * Use this class as a starting point for your own ingester. The interesting
 * customization points are marked with {@code // customize here} comments below.
 * </p>
 *
 * <p>
 * When more than one ingester is registered, Fess runs them in priority order
 * (see {@link #getPriority()}): lower numbers run first, and each ingester
 * receives the document already modified by the previous one. The default
 * priority is {@code 99}; this example lowers it so that it runs before
 * ingesters that keep the default.
 * </p>
 */
public class ExampleIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(ExampleIngester.class);

    /** Field added to every document by this ingester. */
    protected static final String INGESTED_BY = "ingested_by";

    /**
     * Default constructor.
     */
    public ExampleIngester() {
        // Run this ingester before any ingester that keeps the default priority (99).
        // Lower numbers run first. Adjust or remove this to fit your ordering needs.
        setPriority(50);
    }

    @Override
    public Map<String, Object> process(final Map<String, Object> target, final DataStoreParams params) {
        // Data store crawls (databases, CSV, etc.) hand you the document fields as a
        // mutable Map. Mutate "target" in place and return it so the next ingester (and
        // ultimately the indexer) sees your changes.
        //
        // customize here: add, transform, or remove fields. For example:
        target.put(INGESTED_BY, ExampleIngester.class.getSimpleName());
        // target.put("indexed_at", java.time.Instant.now().toString());
        // target.remove("internal_only_column");

        log("DATASTORE CRAWL: %s", target);

        // NOTE: returning the (possibly modified) target is required. The pipeline
        // chains this value into the next ingester and then into the indexer; it does
        // NOT null-check it, so returning null does not "skip" a document - it breaks
        // indexing. There is no skip mechanism via the return value.
        return target;
    }

    @Override
    public ResultData process(final ResultData target, final ResponseData responseData) {
        // Web/file crawls give you the crawler's ResultData and the raw ResponseData.
        // The simplest customization works directly on these objects, e.g. inspecting
        // responseData.getUrl()/getMimeType() and returning target.
        //
        // The block below additionally reconstructs the transformed document so it can
        // be inspected (here, logged). This is an ADVANCED step and is only needed when
        // you want the parsed fields (title, content, ...) rather than the raw bytes.
        // It depends on the "accessResult" prototype component being registered in the
        // DI container (see test_app.xml for the test setup). A typical ingester does
        // not need it.
        final AccessResult<?> accessResult = ComponentUtil.getComponent("accessResult");
        accessResult.init(responseData, target);
        final AccessResultData<?> accessResultData = accessResult.getAccessResultData();
        final Transformer transformer = ComponentUtil.getComponent(accessResultData.getTransformerName());
        final Object data = transformer.getData(accessResultData);

        log("WEB/FILE CRAWL: %s", data);

        // customize here: modify "target"/"responseData" before indexing, then return target.
        return target;
    }

    /**
     * Logs the given data using the specified format.
     *
     * @param format the format string
     * @param data the data to log
     */
    protected void log(final String format, final Object data) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format(format, data));
        }
    }
}
