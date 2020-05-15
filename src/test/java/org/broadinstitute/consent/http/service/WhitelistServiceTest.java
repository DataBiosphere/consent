package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.models.WhitelistHeaders;
import org.broadinstitute.consent.http.util.WhitelistCache;
import org.broadinstitute.consent.http.util.WhitelistParserTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class WhitelistServiceTest {

    @Mock
    GCSService gcsService;

    WhitelistCache cache;

    WhitelistService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(gcsService.postWhitelist(any(), any())).thenReturn(new GenericUrl("http://localhost:8000/whitelist.txt"));
    }

    private void initServices(String whitelistData) {
        try {
            when(gcsService.getMostRecentWhitelist()).thenReturn(whitelistData);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        cache = new WhitelistCache(gcsService);
        service = new WhitelistService(gcsService, cache);
    }

    @Test
    public void testPostWhitelist_valid() {
        String fileData = WhitelistParserTest.makeSampleWhitelistFile(2, false);
        initServices(fileData);
        GenericUrl url = service.postWhitelist(fileData);
        assertNotNull(url);
    }

    @Test(expected = BadRequestException.class)
    public void testPostWhitelist_invalid() {
        String fileData = WhitelistParserTest.makeSampleWhitelistFile(2, true);
        initServices(fileData);
        service.postWhitelist(fileData);
    }

    @Test
    public void testValidateWhitelist_valid() {
        String fileData = WhitelistParserTest.makeSampleWhitelistFile(2, false);
        initServices(fileData);
        boolean valid = service.validateWhitelist(fileData);
        assertTrue(valid);
    }

    @Test
    public void testValidateWhitelist_invalid() {
        String fileData = WhitelistParserTest.makeSampleWhitelistFile(2, true);
        initServices(fileData);
        boolean valid = service.validateWhitelist(fileData);
        assertFalse(valid);
    }

    @Test
    public void testFindWhitelistEntriesForUser_CommonsId() {
        String matchField = "commons";
        String fileData = generateWhitelistWithData(matchField);
        DACUser user = new DACUser();
        user.setDacUserId(1);
        user.setEmail("email");
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(1, ResearcherFields.ERA_COMMONS_ID.getValue(), matchField));

        initServices(fileData);
        service.postWhitelist(fileData);
        List<WhitelistEntry> entries = service.findWhitelistEntriesForUser(user, properties);
        assertEquals(1, entries.size());
    }

    @Test
    public void testFindWhitelistEntriesForUser_Email() {
        String matchField = "email";
        String fileData = generateWhitelistWithData(matchField);
        DACUser user = new DACUser();
        user.setDacUserId(1);
        user.setEmail(matchField);
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(1, ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), matchField));

        initServices(fileData);
        service.postWhitelist(fileData);
        List<WhitelistEntry> entries = service.findWhitelistEntriesForUser(user, properties);
        assertEquals(1, entries.size());
    }

    @Test
    public void testFindWhitelistEntriesForUser_Org() {
        String matchField = "org";
        String fileData = generateWhitelistWithData(matchField);
        DACUser user = new DACUser();
        user.setDacUserId(1);
        user.setEmail("email");
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(1, ResearcherFields.INSTITUTION.getValue(), matchField));

        initServices(fileData);
        service.postWhitelist(fileData);
        List<WhitelistEntry> entries = service.findWhitelistEntriesForUser(user, properties);
        assertEquals(1, entries.size());
    }

    @Test
    public void testFindWhitelistEntriesForUser_NoResults() {
        String matchField = RandomStringUtils.random(5, true, true);
        String fileData = WhitelistParserTest.makeSampleWhitelistFile(2, false);
        DACUser user = new DACUser();
        user.setDacUserId(1);
        user.setEmail(matchField);
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(1, ResearcherFields.ERA_COMMONS_ID.getValue(), matchField));
        properties.add(new ResearcherProperty(1, ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), matchField));
        properties.add(new ResearcherProperty(1, ResearcherFields.INSTITUTION.getValue(), matchField));

        initServices(fileData);
        service.postWhitelist(fileData);
        List<WhitelistEntry> entries = service.findWhitelistEntriesForUser(user, properties);
        assertEquals(0, entries.size());
    }

    private String generateWhitelistWithData(String value) {
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
        for (int i = 0; i < 7; i++) {
            builder.append(value).append(tab);
        }
        builder.append(RandomStringUtils.randomAlphabetic(10)).append(newline);
        return builder.toString();
    }

}
