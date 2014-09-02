package org.genomebridge.consent.autocomplete.service;

import com.google.inject.Inject;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.genomebridge.consent.autocomplete.resources.TermResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ElasticSearchAutocompleteAPI implements AutocompleteAPI {
    private static String FIELD_ID = "id";
    private static String FIELD_ONTOLOGY_TYPE = "ontology";
    private static String FIELD_LABEL = "label";
    private static String FIELD_DEFINITION = "definition";
    private static String FIELD_SYNONYM = "synonym";
    private static String FIELD_USABLE = "usable";
    private Client client;
    private String index;

    public ElasticSearchAutocompleteAPI(Client client, String index) {
        this.client = client;
        this.index = index;
    }

    private QueryBuilder buildQuery(String term) {
        return QueryBuilders.multiMatchQuery(term, String.format("%s^4", FIELD_ID), String.format("%s^2", FIELD_LABEL), FIELD_SYNONYM);
    }

    private List<TermResource> executeSearch(QueryBuilder qb, int limit) {
        List<TermResource> termList = new ArrayList<>();
        SearchHits hits = client.prepareSearch(index).setQuery(qb).setSize(limit).execute().actionGet().getHits();
        for (SearchHit hit: hits.getHits()) {
            Map<String, Object> fields = hit.getSource();
            String id = fields.containsKey(FIELD_ID) ? (String)fields.get(FIELD_ID) : null;
            String label = fields.containsKey(FIELD_LABEL) ? (String)fields.get(FIELD_LABEL) : null;
            String definition = fields.containsKey(FIELD_DEFINITION) ? (String)fields.get(FIELD_DEFINITION) : null;
            List<String> synonyms = fields.containsKey(FIELD_SYNONYM)
                    ? (List<String>) fields.get(FIELD_SYNONYM)
                    : Collections.<String>emptyList();
            TermResource tr = new TermResource();
            tr.id = id;
            tr.label = label;
            tr.definition = definition;
            tr.synonyms = synonyms;
            termList.add(tr);
        }
        return termList;
    }

    @Override
    public List<TermResource> lookup(String query, int limit) {
        return executeSearch(buildQuery(query), limit);
    }

    @Override
    public List<TermResource> lookup(String[] tags, String query, int limit) {
        FilterBuilder filter = FilterBuilders.andFilter(
                FilterBuilders.termFilter(FIELD_USABLE, true),
                FilterBuilders.termsFilter(FIELD_ONTOLOGY_TYPE, tags)
        );
        QueryBuilder queryBuilder = buildQuery(query);
        return executeSearch(QueryBuilders.filteredQuery(queryBuilder, filter), limit);
    }
}
