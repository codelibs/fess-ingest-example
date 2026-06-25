Fess Ingest Example
[![Java CI with Maven](https://github.com/codelibs/fess-ingest-example/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ingest-example/actions/workflows/maven.yml)
==========================

## Overview

Fess Ingest Example is a sample project that demonstrates how to extend Fess's ingest functionality. This project serves as a template for creating custom ingesters that can process crawled data before it's indexed into the search engine.

This sample implementation enriches each crawled document with a marker field, logs it, and handles two types of crawl results:

- **Data Store Crawl**: Data from databases, CSV files, etc.
- **Web/File Crawl**: Content from websites and file systems

It also demonstrates **ingester priority**: when multiple ingesters are registered, Fess runs them in priority order (lower number first), and each ingester receives the document already modified by the previous one. This example lowers its priority to `50` so it runs before ingesters that keep the default of `99`.

## What is a Fess Ingester?

A Fess Ingester is a plugin that intercepts crawled data and allows you to:
- Transform and enrich data before indexing
- Filter out unwanted content
- Send data to external systems
- Add custom metadata
- Integrate with third-party APIs

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-ingest-example/).

## Installation

See [Plugin](https://fess.codelibs.org/15.7/admin/plugin-guide.html) of Administration guide.

## How to Implement Your Own Ingester

### Step 1: Create Your Ingester Class

Create a class that extends `org.codelibs.fess.ingest.Ingester` and override the process methods:

```java
package org.codelibs.fess.ingest.example;

import java.util.Map;
import org.codelibs.fess.crawler.entity.ResponseData;
import org.codelibs.fess.crawler.entity.ResultData;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.ingest.Ingester;

public class ExampleIngester extends Ingester {

    public ExampleIngester() {
        // Optional: run before ingesters that keep the default priority (99).
        // Lower numbers run first.
        setPriority(50);
    }

    // Process data from Data Store crawls (databases, CSV, etc.)
    @Override
    public Map<String, Object> process(final Map<String, Object> target,
                                       final DataStoreParams params) {
        // Your custom processing logic here.
        // Example: add custom fields, transform values, call external APIs.
        // Mutate "target" in place and return it.
        target.put("custom_field", "custom_value");
        return target;
    }

    // Process data from Web/File crawls
    @Override
    public ResultData process(final ResultData target,
                             final ResponseData responseData) {
        // Your custom processing logic here.
        // Example: inspect responseData, enrich metadata, then return target.
        return target;
    }
}
```

### Step 2: Register Your Ingester

Create or edit `src/main/resources/fess_ingest++.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE components PUBLIC "-//DBFLUTE//DTD LastaDi 1.0//EN"
    "http://dbflute.org/meta/lastadi10.dtd">
<components>
    <component name="exampleIngester"
               class="org.codelibs.fess.ingest.example.ExampleIngester">
        <postConstruct name="register"></postConstruct>
    </component>
</components>
```

The `<postConstruct name="register">` directive automatically registers your ingester with Fess.

### Choosing What to Override

`Ingester` exposes three public `process(...)` entry points, one per crawl type:

- `process(Map<String, Object> target, DataStoreParams params)` - data store crawls
- `process(Map<String, Object> target, AccessResult<String> accessResult)` - web/file crawls (map form)
- `process(ResultData target, ResponseData responseData)` - web/file crawls (raw crawler result)

The two `Map` overloads both delegate, by default, to a single protected hook:

```java
protected Map<String, Object> process(final Map<String, Object> target) {
    return target;
}
```

If you only need to enrich the document map and do not care which crawl produced it, **overriding this protected `process(Map)` is the simplest hook** - it covers both data store and web/file (map) crawls at once.

### Ingester Priority

Multiple ingesters can be registered at the same time. Fess runs them in **priority order**, where a **lower number runs first** (the default is `99`). Each ingester receives the document already modified by the previous one, so priority lets you control the order of enrichment steps. Set it via the constructor or `setPriority(int)`, or override `getPriority()`.

### Step 3: Build and Package

```bash
mvn clean package
```

This will create a JAR file in the `target` directory.

### Step 4: Install the Plugin

1. Copy the generated JAR file to Fess's plugin directory
2. Restart Fess
3. Your ingester will now process all crawled data

## Understanding the Example Implementation

The `ExampleIngester` class in this project demonstrates:

### Data Store Crawl Processing

```java
@Override
public Map<String, Object> process(final Map<String, Object> target,
                                   final DataStoreParams params) {
    target.put(INGESTED_BY, ExampleIngester.class.getSimpleName()); // enrich the document
    log("DATASTORE CRAWL: %s", target);
    return target;
}
```

The `target` Map contains fields from your data source (database columns, CSV fields, etc.). You can:
- Modify field values
- Add new fields
- Remove unwanted fields

You must return the (possibly modified) `target`. The pipeline chains the returned value into the next ingester and then into the indexer **without a null check**, so returning `null` does **not** skip a document - it breaks indexing. There is no skip mechanism via the return value.

### Web/File Crawl Processing

```java
@Override
public ResultData process(final ResultData target,
                         final ResponseData responseData) {
    final AccessResult<?> accessResult = ComponentUtil.getComponent("accessResult");
    accessResult.init(responseData, target);
    final AccessResultData<?> accessResultData = accessResult.getAccessResultData();
    final Transformer transformer = ComponentUtil.getComponent(
        accessResultData.getTransformerName());
    final Object data = transformer.getData(accessResultData);
    log("WEB/FILE CRAWL: %s", data);
    return target;
}
```

This method processes web pages and files. The `data` object contains the transformed document with fields like:
- `url`: Document URL
- `title`: Page title
- `content`: Extracted text content
- `mimetype`: Content type
- And more...

> **Note:** The `accessResult`/`transformer` reconstruction above is only needed to *inspect* the transformed document (here, to log it). A typical ingester does not need it and can simply modify and return `target`.

## Use Cases

Here are some practical examples of what you can do with a custom ingester:

### 1. Add Custom Metadata

```java
public Map<String, Object> process(final Map<String, Object> target,
                                   final DataStoreParams params) {
    target.put("indexed_date", new Date());
    target.put("department", "Engineering");
    return target;
}
```

### 2. Normalize or Redact Fields

```java
public Map<String, Object> process(final Map<String, Object> target,
                                   final DataStoreParams params) {
    // Drop a field you do not want indexed
    target.remove("internal_only_column");
    // Normalize a value in place
    final Object label = target.get("label");
    if (label instanceof String) {
        target.put("label", ((String) label).trim().toLowerCase());
    }
    return target;
}
```

> **Note:** An ingester cannot drop a whole document by returning `null` - the returned
> value is added to the index list unchecked, so `null` causes errors rather than a skip.
> To exclude documents, restrict the crawl configuration (e.g. URL/path filters or the
> data store query) instead.

### 3. Send to External Systems

```java
public Map<String, Object> process(final Map<String, Object> target,
                                   final DataStoreParams params) {
    // Send data to analytics system
    analyticsClient.track(target);
    // Continue with normal indexing
    return target;
}
```

### 4. Enrich with External Data

```java
public Map<String, Object> process(final Map<String, Object> target,
                                   final DataStoreParams params) {
    String userId = (String) target.get("user_id");
    UserDetails details = userService.getUserDetails(userId);
    target.put("user_name", details.getName());
    target.put("user_department", details.getDepartment());
    return target;
}
```

## Development

### Build

```bash
mvn clean package
```

### Test

```bash
mvn test
```

### Project Structure

```
fess-ingest-example/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/codelibs/fess/ingest/example/
│   │   │       └── ExampleIngester.java          # Main ingester implementation
│   │   └── resources/
│   │       └── fess_ingest++.xml                 # DI component registration
│   └── test/
│       ├── java/
│       │   └── org/codelibs/fess/ingest/example/
│       │       └── ExampleIngesterTest.java      # Unit tests
│       └── resources/
│           └── test_app.xml                       # Test configuration
├── pom.xml                                        # Maven configuration
└── README.md
```

## Requirements

- Java 21
- Maven 3.8 or later
- Fess 15.x

## License

Apache License 2.0

## Resources

- [Fess Documentation](https://fess.codelibs.org/)
- [Fess GitHub Repository](https://github.com/codelibs/fess)
- [Plugin Guide](https://fess.codelibs.org/15.7/admin/plugin-guide.html)
