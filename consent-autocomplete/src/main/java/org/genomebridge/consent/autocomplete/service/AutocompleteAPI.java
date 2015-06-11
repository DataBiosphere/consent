package org.genomebridge.consent.autocomplete.service;

import org.genomebridge.consent.autocomplete.resources.TermResource;

import java.util.List;

public interface AutocompleteAPI {
    List<TermResource> lookup(String query, int limit);
    List<TermResource> lookup(String[] tags, String query, int limit);
}
