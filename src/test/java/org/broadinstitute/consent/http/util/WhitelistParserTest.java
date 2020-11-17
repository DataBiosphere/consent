package org.broadinstitute.consent.http.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.models.WhitelistHeaders;
import org.junit.Test;

public class WhitelistParserTest {

    @Test
    public void testParseWhitelist() {
        WhitelistParser parser = new WhitelistParser();
        int numRows = 3;
        String fileData = makeSampleWhitelistFile(numRows, false);
        List<WhitelistEntry> entries = parser.parseWhitelist(fileData);
        assertFalse(entries.isEmpty());
        assertEquals(numRows, entries.size());
    }

    public static String makeSampleWhitelistFile(int rows, boolean invalid) {
        String tab = "\t";
        String newline = "\n";
        StringBuilder builder = new StringBuilder();
        builder.append(WhitelistHeaders.ORGANIZATION.getValue()).append(tab).
                append(WhitelistHeaders.COMMONS_ID.getValue()).append(tab).
                append(WhitelistHeaders.NAME.getValue()).append(tab).
                append(WhitelistHeaders.EMAIL.getValue()).append(tab).
                append(WhitelistHeaders.SIGNING_OFFICIAL_NAME.getValue()).append(tab).
                append(WhitelistHeaders.SIGNING_OFFICIAL_EMAIL.getValue()).append(tab).
                append(WhitelistHeaders.IT_DIRECTOR_NAME.getValue()).append(tab).
                append(WhitelistHeaders.IT_DIRECTOR_EMAIL.getValue()).append(newline);
        for (int r = 0; r < rows; r++) {
            for (int i = 0; i < 7; i++) {
                String data = (invalid && i <= 3) ? "" : RandomStringUtils.randomAlphabetic(10);
                builder.append(data).append(tab);
            }
            builder.append(RandomStringUtils.randomAlphabetic(10)).append(newline);
        }
        return builder.toString();
    }

}
