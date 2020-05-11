package org.broadinstitute.consent.http.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.models.WhitelistHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhitelistParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<WhitelistEntry> parseWhitelist(String fileData) {
        List<WhitelistEntry> entries = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
        try(CSVReader reader = new CSVReaderBuilder(new StringReader(fileData)).
                withSkipLines(1).
                withCSVParser(parser).
                build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                entries.add(makeEntry(line));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return entries;
    }

    private WhitelistEntry makeEntry(String[] line) {
        WhitelistEntry entry = new WhitelistEntry();
        List<String> values =Arrays.asList(line);
        try {
            if (values.get(WhitelistHeaders.ORGANIZATION.ordinal()) != null) {
               entry.setOrganization(values.get(WhitelistHeaders.ORGANIZATION.ordinal()));
            }
            if (values.get(WhitelistHeaders.COMMONS_ID.ordinal()) != null) {
               entry.setCommonsId(values.get(WhitelistHeaders.COMMONS_ID.ordinal()));
            }
            if (values.get(WhitelistHeaders.NAME.ordinal()) != null) {
               entry.setName(values.get(WhitelistHeaders.NAME.ordinal()));
            }
            if (values.get(WhitelistHeaders.EMAIL.ordinal()) != null) {
               entry.setEmail(values.get(WhitelistHeaders.EMAIL.ordinal()));
            }
            if (values.get(WhitelistHeaders.SIGNING_OFFICIAL_NAME.ordinal()) != null) {
               entry.setSigningOfficialName(values.get(WhitelistHeaders.SIGNING_OFFICIAL_NAME.ordinal()));
            }
            if (values.get(WhitelistHeaders.SIGNING_OFFICIAL_EMAIL.ordinal()) != null) {
               entry.setSigningOfficialEmail(values.get(WhitelistHeaders.SIGNING_OFFICIAL_EMAIL.ordinal()));
            }
            if (values.get(WhitelistHeaders.IT_DIRECTOR_NAME.ordinal()) != null) {
               entry.setItDirectorName(values.get(WhitelistHeaders.IT_DIRECTOR_NAME.ordinal()));
            }
            if (values.get(WhitelistHeaders.IT_DIRECTOR_EMAIL.ordinal()) != null) {
               entry.setItDirectorEmail(values.get(WhitelistHeaders.IT_DIRECTOR_EMAIL.ordinal()));
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error(e.getMessage());
        }
        return entry;
    }

}
