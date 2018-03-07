# Fedora JSON-LD addons

[![Build Status](https://travis-ci.org/DataConservancy/fcrepo-jsonld.svg?branch=master)](https://travis-ci.org/DataConservancy/fcrepo-jsonld)


## Quick start

1. Build via `mvn clean install`
2. Copy the shaded artifact `jsonld-addon-filters/target/jsonld-addon-filters-0.0.1-SNAPSHOT-shaded.jar` to your servlet container's shared library directory, e.g. `${CATALINA_HOME}/common/lib`
3. Add to the global `web.xml` the following filter configuration:

       <filter>
         <filter-name>jsonld-compaction-filter</filter-name>
         <filter-class>org.dataconservancy.fcrepo.jsonld.compact.CompactionFilter</filter-class>
         <async-supported>true</async-supported>
       </filter>

       <filter-mapping>
         <filter-name>jsonld-compaction-filter</filter-name>
         <url-pattern>/*</url-pattern>
       </filter-mapping>

## Configuration

### Compaction

A context can be provided such that all json-ld responses are compacted with respect to the given context.  Use _one of_ the following three methods:

1. Define an environment variable `COMPACTION_URI`

       export COMPACTION_URI=http://example.org/context.jsonld

2. Define a system property `compaction.uri`

       -Dcompaction.uri=http://example.org/context.jsonld

3. Use an init parameter `context` on the filter in the `web.xml`

       <filter>
         <filter-name>jsonld-compaction-filter</filter-name>
         <filter-class>org.dataconservancy.fcrepo.jsonld.compact.CompactionFilter</filter-class>
         <async-supported>true</async-supported>
         <init-param>
           <param-name>context</param-name>
           <param-value>http://example.org/context.jsonld</param-value>
         </init-param>
       </filter>

### Static Loaded Contexts

Context URIs can be mapped to a files to pre-load contexts so that they can be used/cached without requiring resolution on the internet.  As many contexts as desired can be loaded using the following pattern for a given arbitrary context name `NAME`.  Define system properties or environment varlables as follows:

1. Define environment variables `COMPACTION_PRELOAD_URI_NAME` and `COMPACTION_PRELOAD_FILE_NAME`

       export COMPACTION_PRELOAD_URI_MY_CONTEXT_1=http://example.org/context.jsonld
       export COMPACTION_PRELOAD_FILE_MY_CONTEXT_1=/path/to/file

1. Define system properties `compaction.preload.uri.NAME` and `compaction.preload.file.NAME`

       -Dcompaction.preload.uri.my.context.1=http://example.org/context.jsonld
       -Dcompaction.preload.file.my.context.1=/path/to/file

