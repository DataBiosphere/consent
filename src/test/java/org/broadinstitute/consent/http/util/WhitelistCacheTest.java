package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class WhitelistCacheTest {

    @Mock
    private GCSService gcsService;

    private WhitelistCache cache;

    private final int count = 4;

    private final String whitelistData = WhitelistParserTest.makeSampleWhitelistFile(count, false);

    private List<WhitelistEntry> whitelistEntries;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        whitelistEntries = new WhitelistParser().parseWhitelist(whitelistData);
        when(gcsService.getMostRecentWhitelist()).thenReturn(whitelistData);
        cache = new WhitelistCache(gcsService);
    }

    @Test
    public void testLoadCachesFromService() {
        testIndividualCacheQueries();
    }

    @Test
    public void testLoadCachesFromData() {
        cache.loadCaches(whitelistData);
        testIndividualCacheQueries();
    }

    private void testIndividualCacheQueries() {
        whitelistEntries.forEach(e -> {
            List<WhitelistEntry> commonsMatches = cache.queryByCommonsId(e.getCommonsId());
            assertEquals(1, commonsMatches.size());
            List<WhitelistEntry> emailMatches = cache.queryByEmail(e.getEmail());
            assertEquals(1, emailMatches.size());
            List<WhitelistEntry> orgMatches = cache.queryByOrganization(e.getOrganization());
            assertEquals(1, orgMatches.size());
        });
        assertTrue(cache.queryByCommonsId("").isEmpty());
        assertTrue(cache.queryByEmail("").isEmpty());
        assertTrue(cache.queryByOrganization("").isEmpty());
    }

}
