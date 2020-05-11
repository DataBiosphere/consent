package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    public GenericUrl postWhitelist(String fileData) throws IOException {
        // get a timestamp to label the file
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        // push file to bucket
        String fileName = "lc_whitelist_" + timestamp + ".tsv";
        return gcsStore.postWhitelist(IOUtils.toInputStream(fileData, Charset.defaultCharset()), fileName);
    }

}
