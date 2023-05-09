package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;

import java.util.List;

public class PaginationResponse<T> {

    private Integer unfilteredCount;
    private Integer filteredCount;
    private Integer filteredPageCount;
    private List<T> results;
    private List<String> paginationTokens;

    public Integer getUnfilteredCount() {
        return unfilteredCount;
    }

    public PaginationResponse<T> setUnfilteredCount(Integer unfilteredCount) {
        this.unfilteredCount = unfilteredCount;
        return this;
    }

    public Integer getFilteredCount() {
        return filteredCount;
    }

    public PaginationResponse<T> setFilteredCount(Integer filteredCount) {
        this.filteredCount = filteredCount;
        return this;
    }

    public Integer getFilteredPageCount() {
        return filteredPageCount;
    }

    public PaginationResponse<T> setFilteredPageCount(Integer filteredPageCount) {
        this.filteredPageCount = filteredPageCount;
        return this;
    }

    public List<T> getResults() {
        return results;
    }

    public PaginationResponse<T> setResults(List<T> results) {
        this.results = results;
        return this;
    }

    public List<String> getPaginationTokens() {
        return paginationTokens;
    }

    public PaginationResponse<T> setPaginationTokens(List<String> paginationTokens) {
        this.paginationTokens = paginationTokens;
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
