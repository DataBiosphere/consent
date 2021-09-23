package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

import javax.ws.rs.BadRequestException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PaginationToken {

  private static final Charset UTF_8 = StandardCharsets.UTF_8;
  // private static final Map<String> acceptableSortFields = Collections.emptyLi();

  @JsonProperty
  private Integer page;

  @JsonProperty
  private Integer pageSize;

  @JsonProperty
  private String sortField;

  @JsonProperty
  private String sortDirection;

  @JsonProperty
  private String filterTerm;

  @JsonProperty
  private Integer filteredCount;

  @JsonProperty
  private Integer filteredPageCount;

  @JsonProperty
  private Integer unfilteredCount;

  private Map<String, String> acceptableSortFields;

  //constructor for request tokens
  public PaginationToken(Integer page, Integer pageSize, String sortField, String sortDirection, String filterTerm, Map<String, String> acceptableSortFields) {
    this.acceptableSortFields = acceptableSortFields;
    checkSortField(sortField);
    checkSortDirection(sortDirection);
    this.page = page;
    this.pageSize = Objects.nonNull(pageSize) ? pageSize : 10;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
    this.filterTerm = filterTerm;
    checkForValidNumbers();
  }

  //constructor for response tokens
  public PaginationToken(Integer page, Integer pageSize, String sortField, String sortDirection, String filterTerm,
                         Integer filteredCount, Integer filteredPageCount, Integer unfilteredCount, Map<String, String> acceptableSortFields) {
    this.acceptableSortFields = acceptableSortFields;
    checkSortField(sortField);
    checkSortDirection(sortDirection);
    this.page = page;
    this.pageSize = pageSize;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
    this.filterTerm = filterTerm;
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

  public String getFilterTerm() {
    return this.filterTerm;
  }

  public Integer getFilteredCount() {
    return this.filteredCount;
  }

  public void setFilteredCount(Integer filteredCount) {
    this.filteredCount = filteredCount;
    /*
     * Three cases in determining filtered page count
     * 1. The count is less than the page size: page size = 1
     * 2. The count/page size modulo page size is 0, i.e. count of 50, page size of 10, page count is 5
     * 3. Anything else, i.e. count of 55, page size of 10, page size is 6
     */
    if (filteredCount <= this.getPageSize()) {
      setFilteredPageCount(1);
    } else if (((filteredCount/this.getPageSize()) % this.getPageSize()) == 0) {
      setFilteredPageCount(filteredCount/this.getPageSize());
    } else {
      setFilteredPageCount((filteredCount/this.getPageSize()) + 1);
    }
  }

  public Integer getUnfilteredCount() {
    return this.unfilteredCount;
  }

  public void setUnfilteredCount(Integer unfilteredCount) {
    this.unfilteredCount = unfilteredCount;
  }

  public Integer getFilteredPageCount() {
    return this.filteredPageCount;
  }

  public void setFilteredPageCount(Integer filteredPageCount) {
    this.filteredPageCount = filteredPageCount;
  }

  public String toBase64() {
    return Base64.getEncoder().encodeToString(new Gson().toJson(this).getBytes(UTF_8));
  }

  public void setAcceptableSortField(Map<String, String> acceptableSortFields) {
    this.acceptableSortFields = acceptableSortFields;
  }

  public Map<String, String> getAcceptableSortFields() {
    return acceptableSortFields;
  }

  private void checkSortField(String sortField) {
    if (Objects.nonNull(sortField)) {
      if (Objects.isNull(acceptableSortFields.get(sortField))) {
        throw new BadRequestException("Cannot sort on given field: " + sortField);
      }
    }
  }

  private void checkSortDirection(String sortDirection) {
    if (Objects.nonNull(sortDirection)) {
      if (!sortDirection.toLowerCase().equals("asc") && !sortDirection.toLowerCase().equals("desc")) {
        throw new BadRequestException("Sort direction must be either 'asc' or 'desc");
      }
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

  /**
   * Generate an ordered sequence of tokens from self.
   *
   * @return Ordered list of PaginationTokens
   */
  public List<PaginationToken> createListOfPaginationTokensFromSelf() {
    return IntStream.rangeClosed(1, this.getFilteredPageCount())
        .mapToObj(
            i -> new PaginationToken(
                i,
                this.getPageSize(),
                this.getSortField(),
                this.getSortDirection(),
                this.getFilterTerm(),
                this.getFilteredCount(),
                this.getFilteredPageCount(),
                this.getUnfilteredCount(),
                this.getAcceptableSortFields()))
        .collect(Collectors.toList());
  }

  public int getStartIndex() {
    return (this.getPage() * this.getPageSize()) - this.getPageSize();
  }

  public int getEndIndex() {
    return Math.min(
            (getStartIndex() + this.getPageSize()),
            (this.getFilteredCount()));
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
