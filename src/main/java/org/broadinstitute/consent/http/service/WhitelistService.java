package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.util.WhitelistCache;
import org.broadinstitute.consent.http.util.WhitelistParser;

import javax.ws.rs.BadRequestException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WhitelistService {

    public static final String WHITELIST_FILE_PREFIX = "lc_whitelist_";
    private final GCSService gcsService;
    private final WhitelistCache cache;

    @Inject
    public WhitelistService(GCSService gcsService, WhitelistCache cache) {
        this.gcsService = gcsService;
        this.cache = cache;
    }

    /**
     * Post the string content to a white list file in GCS.
     *
     * @param fileData File Data
     * @return URL value of the posted file data
     * @throws BadRequestException The exception
     */
    public GenericUrl postWhitelist(String fileData) throws BadRequestException {
        if (!validateWhitelist(fileData)) {
            throw new BadRequestException("Invalid white list data");
        }
        // get a timestamp to label the file
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        // push file to bucket
        String fileName = WHITELIST_FILE_PREFIX + timestamp + ".tsv";
        GenericUrl url = gcsService.postWhitelist(fileData, fileName);
        // regenerate caches
        cache.loadCaches(fileData);
        return url;
    }

    /**
     * Validate the required fields for Whitelist Entries
     *
     * @param fileData String value of the file content
     * @return Valid: True, invalid: False
     */
    public boolean validateWhitelist(String fileData) {
        List<WhitelistEntry> entries = new WhitelistParser().parseWhitelist(fileData);
        return entries.stream().noneMatch(e -> StringUtils.isBlank(e.getOrganization())) &&
                entries.stream().noneMatch(e -> StringUtils.isBlank(e.getName())) &&
                entries.stream().noneMatch(e -> StringUtils.isBlank(e.getEmail())) &&
                entries.stream().noneMatch(e -> StringUtils.isBlank(e.getCommonsId()));
    }

    public List<WhitelistEntry> findWhitelistEntriesForUser(DACUser user, List<ResearcherProperty> props) {
        List<WhitelistEntry> entries = new ArrayList<>();
        entries.addAll(cache.queryByEmail(user.getEmail()));
        entries.addAll(
                props.stream().
                        filter(p -> p.getPropertyKey().equalsIgnoreCase(ResearcherFields.ERA_USERNAME.getValue()) ||
                                p.getPropertyKey().equalsIgnoreCase(ResearcherFields.ERA_COMMONS_ID.getValue())).
                        map(p -> cache.queryByCommonsId(p.getPropertyValue())).
                        flatMap(List::stream).
                        filter(Objects::nonNull).
                        distinct().
                        collect(Collectors.toList()));
        entries.addAll(
                props.stream().
                        filter(p -> p.getPropertyKey().equalsIgnoreCase(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue())).
                        map(p -> cache.queryByEmail(p.getPropertyValue())).
                        flatMap(List::stream).
                        filter(Objects::nonNull).
                        distinct().
                        collect(Collectors.toList()));
        return entries.stream().distinct().collect(Collectors.toList());
    }

}
