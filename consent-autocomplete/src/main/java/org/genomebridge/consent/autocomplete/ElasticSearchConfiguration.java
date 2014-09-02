package org.genomebridge.consent.autocomplete;

import java.util.List;

public class ElasticSearchConfiguration {
    public List<String> servers;
    public String clusterName = "elasticsearch";
    public String index;
}
