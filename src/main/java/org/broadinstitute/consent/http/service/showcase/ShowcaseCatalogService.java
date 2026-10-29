package org.broadinstitute.consent.http.service.showcase;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.ShowcaseLookupResponse;
import org.broadinstitute.consent.http.models.ShowcaseUnavailableResult;

/**
 * Answers the public Data Showcase lookup from the S2.1 catalog projection, so no mode can return a
 * field that the projection does not.
 */
public class ShowcaseCatalogService {

  private final DatasetDAO datasetDAO;

  @Inject
  public ShowcaseCatalogService(DatasetDAO datasetDAO) {
    this.datasetDAO = datasetDAO;
  }

  /** The response body for a validated lookup. */
  public Object lookup(ShowcaseLookup lookup) {
    return switch (lookup) {
      case ShowcaseIdentifiersLookup identifiers ->
          new ShowcaseLookupResponse(lookupIdentifiers(identifiers));
    };
  }

  /**
   * One result for each identifier, in the lookup's order: the summary when the dataset is eligible
   * for the section, otherwise {@link ShowcaseUnavailableResult}. All identifiers resolve in one
   * query, and nothing checks separately whether a dataset exists, so a private dataset and a
   * nonexistent one get the same result.
   */
  public List<Object> lookupIdentifiers(ShowcaseIdentifiersLookup lookup) {
    if (lookup.identifiers().isEmpty()) {
      return List.of();
    }
    List<Integer> aliases =
        lookup.identifiers().stream().map(Dataset::parseIdentifierToAlias).toList();
    Map<String, ShowcaseDatasetSummary> eligible =
        datasetDAO.findShowcaseSummaries(aliases).stream()
            .filter(lookup.section()::admits)
            .collect(Collectors.toMap(ShowcaseDatasetSummary::identifier, Function.identity()));
    return lookup.identifiers().stream()
        .<Object>map(
            identifier -> {
              ShowcaseDatasetSummary summary = eligible.get(identifier);
              return summary != null ? summary : new ShowcaseUnavailableResult(identifier);
            })
        .toList();
  }
}
