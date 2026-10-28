package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.ShowcaseUnavailableResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowcaseCatalogServiceTest {

  @Mock private DatasetDAO datasetDAO;

  private ShowcaseCatalogService service;

  @BeforeEach
  void setUp() {
    service = new ShowcaseCatalogService(datasetDAO);
  }

  @Test
  void testResultsFollowTheSubmittedOrder() {
    when(datasetDAO.findShowcaseSummaries(List.of(456, 999, 123)))
        .thenReturn(List.of(summary("DUOS-000123", "open"), summary("DUOS-000456", "open")));

    List<Object> results = lookup("DUOS-000456", "DUOS-000999", "DUOS-000123");

    assertEquals(
        List.of(
            summary("DUOS-000456", "open"),
            new ShowcaseUnavailableResult("DUOS-000999"),
            summary("DUOS-000123", "open")),
        results);
  }

  /** The projection drops private and deleted datasets, so they read as nonexistent ones do. */
  @Test
  void testDatasetsWithoutSummariesAreUnavailable() {
    when(datasetDAO.findShowcaseSummaries(List.of(1, 2))).thenReturn(List.of());

    assertEquals(
        List.of(
            new ShowcaseUnavailableResult("DUOS-000001"),
            new ShowcaseUnavailableResult("DUOS-000002")),
        lookup("DUOS-000001", "DUOS-000002"));
  }

  @Test
  void testOpenAccessSectionAdmitsOpenDatasetsOnly() {
    when(datasetDAO.findShowcaseSummaries(List.of(1, 2, 3, 4)))
        .thenReturn(
            List.of(
                summary("DUOS-000001", "open"),
                summary("DUOS-000002", "controlled"),
                summary("DUOS-000003", "external"),
                summary("DUOS-000004", null)));

    assertEquals(
        List.of(
            summary("DUOS-000001", "open"),
            new ShowcaseUnavailableResult("DUOS-000002"),
            new ShowcaseUnavailableResult("DUOS-000003"),
            new ShowcaseUnavailableResult("DUOS-000004")),
        lookup("DUOS-000001", "DUOS-000002", "DUOS-000003", "DUOS-000004"));
  }

  @Test
  void testMaximumLookupIsOneQuery() {
    List<String> identifiers =
        IntStream.rangeClosed(1, ShowcaseLookupLimits.MAX_IDENTIFIERS)
            .mapToObj("DUOS-%06d"::formatted)
            .toList();
    List<Integer> aliases =
        IntStream.rangeClosed(1, ShowcaseLookupLimits.MAX_IDENTIFIERS).boxed().toList();
    when(datasetDAO.findShowcaseSummaries(aliases)).thenReturn(List.of());

    assertEquals(identifiers.size(), lookup(identifiers.toArray(String[]::new)).size());
    verify(datasetDAO).findShowcaseSummaries(aliases);
  }

  @Test
  void testEmptyLookupReadsNothing() {
    assertEquals(List.of(), lookup());
    verifyNoInteractions(datasetDAO);
  }

  /** An excluded pin stays out, an ineligible pin is dropped, and ranked pins are not repeated. */
  @Test
  void testRankingPutsEligiblePinsFirst() {
    when(datasetDAO.findShowcaseSummaries(List.of(3, 4)))
        .thenReturn(List.of(summary("DUOS-000004", "open")));
    when(datasetDAO.findShowcaseNewestAliases(true, null, List.of(2, 3, 4), 1))
        .thenReturn(List.of(5));
    when(datasetDAO.findShowcaseSummaries(List.of(5)))
        .thenReturn(List.of(summary("DUOS-000005", "open")));

    assertEquals(
        List.of(summary("DUOS-000004", "open"), summary("DUOS-000005", "open")),
        service.lookupRanking(
            new ShowcaseRankingLookup(
                ShowcaseSection.OPEN_ACCESS_DATASETS,
                ShowcaseRanking.NEWEST,
                null,
                2,
                List.of("DUOS-000002", "DUOS-000003", "DUOS-000004"),
                List.of("DUOS-000002"))));
  }

  /** An open pin with no DAC is outside a DAC scope; the scope list is immutable, as parsed. */
  @Test
  void testScopedRankingDropsPinWithoutDac() {
    when(datasetDAO.findShowcaseSummaries(List.of(1)))
        .thenReturn(List.of(summary("DUOS-000001", "open")));
    when(datasetDAO.findShowcaseNewestAliases(true, List.of("Example DAC"), List.of(1), 12))
        .thenReturn(List.of());

    assertEquals(
        List.of(),
        service.lookupRanking(
            new ShowcaseRankingLookup(
                ShowcaseSection.OPEN_ACCESS_DATASETS,
                ShowcaseRanking.NEWEST,
                new ShowcaseScope(List.copyOf(List.of("Example DAC"))),
                12,
                List.of("DUOS-000001"),
                List.of())));
  }

  private List<Object> lookup(String... identifiers) {
    return service.lookupIdentifiers(
        new ShowcaseIdentifiersLookup(ShowcaseSection.OPEN_ACCESS_DATASETS, List.of(identifiers)));
  }

  private static ShowcaseDatasetSummary summary(String identifier, String accessManagement) {
    return new ShowcaseDatasetSummary(
        identifier,
        "Example Cohort " + identifier,
        accessManagement,
        null,
        List.of("GRU"),
        null,
        "2026-10-02",
        null,
        null);
  }
}
