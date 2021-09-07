package org.broadinstitute.consent.http.models;

import java.util.List;

public class PaginationResponse<T> {

  private PaginationToken currentToken;
  private List<T> results;
  private List<PaginationToken> paginationTokens;

  public PaginationToken getCurrentToken() {
    return currentToken;
  }

  public PaginationResponse<T> setCurrentToken(PaginationToken currentToken) {
    this.currentToken = currentToken;
    return this;
  }

  public List<T> getResults() {
    return results;
  }

  public PaginationResponse<T> setResults(List<T> results) {
    this.results = results;
    return this;
  }

  public List<PaginationToken> getPaginationTokens() {
    return paginationTokens;
  }

  public PaginationResponse<T> setPaginationTokens(List<PaginationToken> paginationTokens) {
    this.paginationTokens = paginationTokens;
    return this;
  }
}
