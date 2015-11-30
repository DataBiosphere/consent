package org.broadinstitute.consent.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for ConsentAssociation object.
 * <p/>
 * Created by egolin on 9/16/14.
 */
public class ConsentAssociationTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    private static ConsentAssociation buildConsentAssociation(String atype, String... elements) {
        final ArrayList<String> elem_list = new ArrayList<>();
        Collections.addAll(elem_list, elements);
        return new ConsentAssociation(atype, elem_list);
    }

    @Test
    public void serializesToJSON() throws Exception {
        final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        assertThat(MAPPER.writeValueAsString(consent_association)).isEqualTo(fixture("fixtures/consentassociation.json"));
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        assertThat(MAPPER.readValue(fixture("fixtures/consentassociation.json"), ConsentAssociation.class)).
                isEqualToComparingFieldByField(consent_association);
    }

    @Test
    public void testEqualsTrue() throws Exception {
        final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        final ConsentAssociation consent_assoc2 = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        assertThat(consent_assoc1).isEqualTo(consent_assoc2);
    }

    @Test
    public void testEqualsNotMatchingElements() throws Exception {
        final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        final ConsentAssociation consent_assoc2 = buildConsentAssociation("sample", "SM-4321", "SM-8765");
        assertThat(consent_assoc1).isNotEqualTo(consent_assoc2);
    }

    @Test
    public void testEqualsNotMatchingAssociationType() throws Exception {
        final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        final ConsentAssociation consent_assoc2 = buildConsentAssociation("sampleSet", "SM-1234", "SM-5678");
        assertThat(consent_assoc1).isNotEqualTo(consent_assoc2);
    }

    @Test
    public void testToString() throws Exception {
        final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        assertThat(consent_association.toString()).isNotNull();
    }

    @Test
    public void testIsAssociationType() throws Exception {
        final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234", "SM-5678");
        assertThat(consent_association.isAssociationType("sample")).isTrue();
        assertThat(consent_association.isAssociationType("sampleSet")).isFalse();
    }
}
