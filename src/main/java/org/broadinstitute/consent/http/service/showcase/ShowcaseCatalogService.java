package org.broadinstitute.consent.http.service.showcase;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.ShowcaseLookupResponse;
import org.broadinstitute.consent.http.models.ShowcasePageResponse;
import org.broadinstitute.consent.http.models.ShowcaseStatsResponse;
import org.broadinstitute.consent.http.models.ShowcaseUnavailableResult;

/**
 * Answers the public Data Showcase lookup from the S2.1 catalog projection. Rankings and pages read
 * aliases only, and every summary comes from the projection, so no mode can return a field that the
 * projection does not.
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
      case ShowcaseRankingLookup ranking -> new ShowcaseLookupResponse(lookupRanking(ranking));
      case ShowcaseAllLookup all -> lookupPage(all);
      case ShowcaseStatsLookup stats -> lookupStats(stats);
    };
  }

  /**
   * One result for each identifier, in the lookup's order: the summary when the dataset is eligible
   * for the section, otherwise {@link ShowcaseUnavailableResult}. All identifiers resolve in one
   * query, and nothing checks separately whether a dataset exists, so a private dataset and a
   * nonexistent one get the same result.
   */
  public List<Object> lookupIdentifiers(ShowcaseIdentifiersLookup lookup) {
    Map<String, ShowcaseDatasetSummary> eligible =
        summariesByIdentifier(lookup.identifiers(), lookup.section(), null);
    return lookup.identifiers().stream()
        .<Object>map(
            identifier -> {
              ShowcaseDatasetSummary summary = eligible.get(identifier);
              return summary != null ? summary : new ShowcaseUnavailableResult(identifier);
            })
        .toList();
  }

  /**
   * Eligible pins first, in their order, then the ranked datasets, up to the limit. A pin outside
   * the section or the scope is dropped, an exclusion wins over a pin, and pins never take the
   * result past the limit. Ranking runs after eligibility, so a hidden dataset never takes a place.
   */
  public List<ShowcaseDatasetSummary> lookupRanking(ShowcaseRankingLookup lookup) {
    Set<String> exclusions = Set.copyOf(lookup.exclusions());
    List<String> candidatePins =
        lookup.pins().stream().filter(pin -> !exclusions.contains(pin)).toList();
    Map<String, ShowcaseDatasetSummary> eligiblePins =
        summariesByIdentifier(candidatePins, lookup.section(), lookup.scope());
    List<ShowcaseDatasetSummary> pins =
        candidatePins.stream()
            .map(eligiblePins::get)
            .filter(Objects::nonNull)
            .limit(lookup.limit())
            .toList();

    Set<String> notRanked = new LinkedHashSet<>(lookup.exclusions());
    notRanked.addAll(lookup.pins());
    List<Integer> rankedAliases =
        datasetDAO.findShowcaseNewestAliases(
            lookup.section().isOpenAccessOnly(),
            lookup.scope() == null ? null : lookup.scope().dacNames(),
            aliases(notRanked),
            lookup.limit() - pins.size());
    Map<String, ShowcaseDatasetSummary> ranked =
        summariesByIdentifier(identifiers(rankedAliases), lookup.section(), lookup.scope());

    return Stream.concat(
            pins.stream(),
            rankedAliases.stream()
                .map(Dataset::parseAliasToIdentifier)
                .map(ranked::get)
                .filter(Objects::nonNull))
        .toList();
  }

  /**
   * One page of every eligible dataset in identifier order. The cursor comes from the page's last
   * alias, so a dataset that leaves the catalog between two calls cannot move the page boundary.
   */
  public ShowcasePageResponse lookupPage(ShowcaseAllLookup lookup) {
    int afterAlias = lookup.afterAlias() == null ? -1 : lookup.afterAlias();
    List<Integer> aliases = datasetDAO.findShowcaseAliasPage(afterAlias, lookup.pageSize() + 1);
    boolean hasNextPage = aliases.size() > lookup.pageSize();
    List<Integer> page = hasNextPage ? aliases.subList(0, lookup.pageSize()) : aliases;
    return new ShowcasePageResponse(
        datasetDAO.findShowcaseSummaries(page),
        hasNextPage ? ShowcaseCursor.encode(page.getLast()) : null);
  }

  /**
   * The catalog-eligible datasets in the scope and their distinct studies, in one query. Section
   * rules do not apply, so the counts cover every dataset that some shelf can show.
   */
  public ShowcaseStatsResponse lookupStats(ShowcaseStatsLookup lookup) {
    return datasetDAO.countShowcaseDatasets(
        lookup.scope() == null ? null : lookup.scope().dacNames());
  }

  /** The summaries among the identifiers that the section and the scope admit, in one query. */
  private Map<String, ShowcaseDatasetSummary> summariesByIdentifier(
      Collection<String> identifiers, ShowcaseSection section, ShowcaseScope scope) {
    if (identifiers.isEmpty()) {
      return Map.of();
    }
    return datasetDAO.findShowcaseSummaries(aliases(identifiers)).stream()
        .filter(section::admits)
        .filter(summary -> inScope(summary, scope))
        .collect(Collectors.toMap(ShowcaseDatasetSummary::identifier, Function.identity()));
  }

  /**
   * The SQL matches {@code dac.name} the same way: exact, and a deleted DAC never matches. A
   * dataset with no DAC is outside every DAC scope; the null check also keeps an immutable list
   * from throwing on {@code contains(null)}.
   */
  private static boolean inScope(ShowcaseDatasetSummary summary, ShowcaseScope scope) {
    return scope == null
        || scope.dacNames() == null
        || (summary.dacName() != null && scope.dacNames().contains(summary.dacName()));
  }

  private static List<Integer> aliases(Collection<String> identifiers) {
    return identifiers.stream().map(Dataset::parseIdentifierToAlias).toList();
  }

  private static List<String> identifiers(List<Integer> aliases) {
    return aliases.stream().map(Dataset::parseAliasToIdentifier).toList();
  }
}
