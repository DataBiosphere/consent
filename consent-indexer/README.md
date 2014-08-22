# Indexer for Autocomplete Service

This module focuses on taking OBO/OWL files and writing them to
[Elastic Search](http://elasticsearch.org) for use by downstream
processes.

## Documents Written

The documents that get written include more information than is
indexed in the data-use-services as we need to allow for looking up
after we no longer support creating expressions with those names.

The document includes the URI for the named expression, a label,
definition, any synonyms (as applicable), the type of expression, and
whether or not the tag should be used for new expressions.

If a tag has been deprecated, it shouldn't be used for new
expressions, but can still be present in previous expressions. Because
of this, we still index these expressions, but tag them as being for
lookup only.

## Indexing

### Configuration Elastic Search

A sample configuration file:

```yaml
clusterName: elasticsearch
servers: [localhost]
indexName: ontology
```

`clusterName` is the name given to the cluster (usually just
'elasticsearch')
`servers` is a list of endpoints which are running ES
`indexName` is the index to write these entries to

### Command Line

The command line tool takes a tag which describes what the ontology
is, then the file where the ontology is stored. It will run through
and the documents out to the cluster.
