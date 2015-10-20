package org.genomebridge.consent.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class DataSetServiceTest extends AbstractTest{

    public String postDataSetFile(Boolean overwrite) {
        try {
            return path2Url(String.format("/dataset?overwrite=%s", URLEncoder.encode(overwrite.toString(), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("/dataset?overwrite=%s", overwrite);
        }
    }
}
