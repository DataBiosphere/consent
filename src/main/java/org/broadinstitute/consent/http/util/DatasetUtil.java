package org.broadinstitute.consent.http.util;


public class DatasetUtil {

    private static final String PREFIX = "DUOS-";

    public static String parseAlias(Integer aliasUid) {
        String alias;
        if(aliasUid < 10) {
            alias = PREFIX + "00000" + aliasUid;
        }
        else if(aliasUid < 100) {
            alias = PREFIX + "0000" + aliasUid;
        }
        else if(aliasUid < 1000) {
            alias = PREFIX + "000" + aliasUid;
        }
        else if(aliasUid < 10000) {
            alias = PREFIX + "00" + aliasUid;
        }
        else if(aliasUid < 100000) {
            alias = PREFIX + "0" + aliasUid;
        }
        else {
            alias = PREFIX + aliasUid;
        }
        return alias;

    }

}
