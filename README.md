# Fedora JSON-LD addons

[![Build Status](https://travis-ci.org/DataConservancy/fcrepo-jsonld.svg?branch=master)](https://travis-ci.org/DataConservancy/fcrepo-jsonld)

* Compacton filter: Compacts all json-ld responses from the repository with respect to a given context.
* Deserialization filter:  Accepts compacted (or uncompacted) JSON-LD, validates it, and persists it to the repository.  Uses cached or pre-loaded contexts where possible.
* JSON Merge Patch filter: Accepts _compacted_ JSON-LD and performs an [RFC7386](https://tools.ietf.org/html/rfc7386) merge patch against the persisted resource, in compact form.

## Configuration

### Static Loaded Contexts

Context URIs can be mapped to a files to pre-load contexts so that they can be used/cached without requiring resolution on the internet.  As many contexts as desired can be loaded using the following pattern for a given arbitrary context name `NAME`.  Define system properties or environment varlables as follows:

1. Define environment variables `COMPACTION_PRELOAD_URI_NAME` and `COMPACTION_PRELOAD_FILE_NAME`

       export COMPACTION_PRELOAD_URI_MY_CONTEXT_1=http://example.org/context.jsonld
       export COMPACTION_PRELOAD_FILE_MY_CONTEXT_1=/path/to/file

1. Define system properties `compaction.preload.uri.NAME` and `compaction.preload.file.NAME`

       -Dcompaction.preload.uri.my.context.1=http://example.org/context.jsonld
       -Dcompaction.preload.file.my.context.1=/path/to/file

### Compaction

A context can be provided such that json-ld responses are compacted with respect to the given context by default.  Use _one of_ the following three methods:

1. Define an environment variable `COMPACTION_URI`

       export COMPACTION_URI=http://example.org/context.jsonld

2. Define a system property `compaction.uri`

       -Dcompaction.uri=http://example.org/context.jsonld

### Strict JSON-LD

When deserializing json-ld, normally unknown JSON attributes are ignored.  This can be problematic if json-ld from users is known/assumed/required to be in a compact form such that every attribute is presumed to have a definition.  In this scenario, an undefined attribute is an _error_.  Strict jsonld processing will throw a bad request error (400) if user-submitted jsonld contaiins any content that isn't defined in a context.

1. Define an environment variable `JSONLD_STRICT`

       export JSONLD_STRICT=true

2. Define a system property `jsonld.strict`

       -Djsonld.strict=true

### Minimal JSON-LD

When returning json-ld in compact form, when this is set the server will omit any properties that aren't mapped to anything in the context.

1. Define an environment variable `JSONLD_MINIMAL_CONTEXT`

       export JSONLD_MINIMAL CONTEXT=true

2. Define a system property `jsonld.minimal.context`

       -Djsonld.minimal.context=true

### Persisted contexts

This will cause the original jsonld context URI to be persisted in the repository, so that subsequent requests are compacted with respect to this context upon retrieval.

1. Define an environment variable `JSONLD_CONTEXT_PERSIST`

       export JSONLD_CONTEXT_PERSIST=true

2. Define a system property `jsonld.minimal.context`

       -Djsonld.context.persist=true
