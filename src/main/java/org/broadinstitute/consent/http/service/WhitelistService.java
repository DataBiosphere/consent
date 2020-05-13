package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.util.WhitelistCache;
import org.broadinstitute.consent.http.util.WhitelistParser;

import javax.ws.rs.BadRequestException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

}
