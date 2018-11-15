package org.broadinstitute.consent.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class DataSetServiceTest extends AbstractTest{

    public String postDataSetFile(Boolean overwrite, Integer userId) {
        try {
            return path2Url(String.format("/dataset/%s?overwrite=%s", userId, URLEncoder.encode(overwrite.toString(), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("/dataset?overwrite=%s", overwrite);
        }
    }
}
