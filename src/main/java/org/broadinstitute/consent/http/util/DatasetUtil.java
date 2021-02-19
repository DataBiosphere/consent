package org.broadinstitute.consent.http.util;

import org.apache.commons.lang3.StringUtils;

public class DatasetUtil {

    private static final String PREFIX = "DUOS-";

    public static String parseAlias(Integer aliasUid) {
        String alias = PREFIX + StringUtils.leftPad(aliasUid.toString(), 6, "0");
        return alias;
    }

}
