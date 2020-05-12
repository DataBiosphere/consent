package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.util.WhitelistParser;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WhitelistService {

    private final GCSStore gcsStore;

    @Inject
    public WhitelistService(GCSStore gcsStore) {
        this.gcsStore = gcsStore;
    }

    /**
     * Post the string content to a white list file in GCS.
     *
     * @param fileData File Data
     * @return URL value of the posted file data
     * @throws IOException The exception
     */
    public GenericUrl postWhitelist(String fileData) throws BadRequestException, IOException {
        if (!validateWhitelist(fileData)) {
            throw new BadRequestException("Invalid white list data");
        }
        // get a timestamp to label the file
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        // push file to bucket
        String fileName = "lc_whitelist_" + timestamp + ".tsv";
        return gcsStore.postWhitelist(IOUtils.toInputStream(fileData, Charset.defaultCharset()), fileName);
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
