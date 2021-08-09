package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.ws.rs.BadRequestException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PaginationToken {

  private static final Charset UTF_8 = StandardCharsets.UTF_8;
  private static final List<String> acceptableSortFields = Collections.emptyList();

  @JsonProperty
  private Integer page;

  @JsonProperty
  private Integer pageSize;

  @JsonProperty
  private String sortField;

  @JsonProperty
  private String sortDirection;

  @JsonProperty
  private List<String> filterTerms;

  @JsonProperty
  private Integer filteredCount;

  @JsonProperty
  private Integer filteredPageCount;

  @JsonProperty
  private Integer unfilteredCount;

  @JsonProperty
  private PaginationToken previousPageToken;

  @JsonProperty
  private PaginationToken nextPageToken;

  //constructor for request tokens
  public PaginationToken(Integer page, Integer pageSize, String sortField, String sortDirection, List<String> filterTerms) {
    checkSortField(sortField);
    checkSortDirection(sortDirection);
    this.page = page;
    this.pageSize = pageSize;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
    this.filterTerms = filterTerms;
    checkForValidNumbers();
  }

  //constructor for response tokens
  public PaginationToken(Integer page, Integer pageSize, String sortField, String sortDirection, List<String> filterTerms,
                         Integer filteredCount, Integer filteredPageCount, Integer unfilteredCount) {
    checkSortField(sortField);
    checkSortDirection(sortDirection);
    this.page = page;
    this.pageSize = pageSize;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
    this.filterTerms = filterTerms;
    this.filteredCount = filteredCount;
    this.filteredPageCount = filteredPageCount;
    this.unfilteredCount = unfilteredCount;
    checkForValidNumbers();
  }

  public Integer getPage() {
    return this.page;
  }
  public Integer getPageSize() {
    return this.pageSize;
  }

  public String getSortField() {
  	return this.sortField;
  }

  public String getSortDirection() {
    return this.sortDirection;
  }

  public List<String> getFilterTerms() {
    return this.filterTerms;
  }

  public Integer getFilteredCount() {
    return this.filteredCount;
  }

  public Integer getUnfilteredCount() {
    return this.unfilteredCount;
  }

  public Integer getFilteredPageCount() {
    return this.filteredPageCount;
  }

  public void generatePrevious() {
    if (page == 0) {
    this.previousPageToken = null;
    } else {
      this.previousPageToken = new PaginationToken(page - 1, pageSize, sortField, sortDirection, filterTerms);
    }
  }

  public void generateNext() {
    this.nextPageToken = new PaginationToken(page + 1, pageSize, sortField, sortDirection, filterTerms);
  }


  public String toBase64() {
    return Base64.getEncoder().encodeToString(new Gson().toJson(this).getBytes(UTF_8));
  }

  public static PaginationToken fromBase64(String str) {
    String json = new String(Base64.getDecoder().decode(str), UTF_8);
    try {
      PaginationToken result = new Gson().fromJson(json, PaginationToken.class);
      if (result.getPage() < 0) {
        throw new BadRequestException(
          String.format("Invalid pagination offset: %d", result.getPage()));
      }
      return result;
    } catch (JsonSyntaxException e) {
      throw new BadRequestException(String.format("Invalid pagination token: %s", str));
    }
  }

  private void checkSortField(String sortField) {
    if (!acceptableSortFields.contains(sortField)) {
      throw new BadRequestException("Cannot sort on given field: " + sortField);
    }
  }

  private void checkSortDirection(String sortDirection) {
    if (!sortDirection.equals("asc") && !sortDirection.equals("desc")) {
      throw new BadRequestException("Sort direction must be either 'asc' or 'desc");
    }
  }

  private void checkForValidNumbers() {
    if (page < 0 || pageSize < 0) {
      throw new BadRequestException("Integers representing counts and sizes cannot be negative");
    }
    if ((Objects.nonNull(filteredCount) && filteredCount < 0)
      || (Objects.nonNull(filteredPageCount) && filteredPageCount < 0)
      || (Objects.nonNull(unfilteredCount) && unfilteredCount < 0)) {
      throw new BadRequestException("Integers representing counts and sizes cannot be negative");
    }
    if (Objects.nonNull(filteredPageCount) && page > filteredCount) {
      throw new BadRequestException("Page cannot be greater than filtered page count");
    }
  }
}
