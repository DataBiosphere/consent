package org.broadinstitute.consent.http.util;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WhitelistCache {

    private final GCSService gcsService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, List<WhitelistEntry>> commonsIdCache = new ConcurrentHashMap<>();
    private final Map<String, List<WhitelistEntry>> emailCache = new ConcurrentHashMap<>();
    private final Map<String, List<WhitelistEntry>> organizationCache = new ConcurrentHashMap<>();

    @Inject
    public WhitelistCache(GCSService gcsService) {
        this.gcsService = gcsService;
    }

    /**
     * Populate all of the query-able caches
     */
    public void loadCaches(String whitelistData) {
        List<WhitelistEntry> entries = new WhitelistParser().parseWhitelist(whitelistData);
        commonsIdCache.putAll(entries.stream().collect(Collectors.groupingBy(WhitelistEntry::getCommonsId)));
        emailCache.putAll(entries.stream().collect(Collectors.groupingBy(WhitelistEntry::getEmail)));
        organizationCache.putAll(entries.stream().collect(Collectors.groupingBy(WhitelistEntry::getOrganization)));
    }

    public List<WhitelistEntry> queryByCommonsId(String commonsId) {
        return tryCache(commonsIdCache, commonsId);
    }

    public List<WhitelistEntry> queryByEmail(String email) {
        return tryCache(emailCache, email);
    }

    public List<WhitelistEntry> queryByOrganization(String org) {
        return tryCache(organizationCache, org);
    }

    private void loadCachesFromStorage() throws Exception {
        loadCaches(gcsService.getMostRecentWhitelist());
    }

    private List<WhitelistEntry> tryCache(Map<String, List<WhitelistEntry>> cache, String value) {
        if (cache.isEmpty()) {
            try {
                loadCachesFromStorage();
            } catch (Exception e) {
                logger.error("Unable to load whitelist cache: " + e.getMessage());
            }
        }
        List<WhitelistEntry> matchingEntries = cache.get(value);
        if (matchingEntries.isEmpty()) {
            try {
                loadCachesFromStorage();
                // Try one more time ...
                return cache.get(value);
            } catch (Exception e) {
                logger.error("Unable to load whitelist cache: " + e.getMessage());
                return Collections.emptyList();
            }
        }
        return matchingEntries;
    }

}
