package org.broadinstitute.consent.http.models;


import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class DatasetTests {

    @Test
    public void testParseIdentifierToAlias() {
        Assertions.assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-3"));
        Assertions.assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-000003"));
        Assertions.assertEquals(123456, (int) Dataset.parseIdentifierToAlias("DUOS-123456"));

        assertThrows(IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("asdf-123456"));
        assertThrows(IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("DUOS-1234 56"));
        assertThrows(IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("DUOS-1234as56"));
    }

    @Test
    public void testIsStringMatchName() {
        String name = RandomStringUtils.randomAlphanumeric(20);

        Dataset ds = new Dataset();
        ds.setName(name);

        Assertions.assertTrue(ds.isStringMatch(name));
        Assertions.assertTrue(ds.isStringMatch(name.substring(5, 10)));
        Assertions.assertTrue(ds.isStringMatch(name.substring(10, 15)));

        Assertions.assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(30)));
    }

    @Test
    public void testIsStringMatchNameCaseIndependent() {
        String name = RandomStringUtils.randomAlphabetic(20);

        Dataset ds = new Dataset();
        ds.setName(name.toLowerCase());

        Assertions.assertTrue(ds.isStringMatch(name.toUpperCase()));
        Assertions.assertTrue(ds.isStringMatch(name.toUpperCase().substring(7, 14)));
    }

    @Test
    public void testIsStringMatchDatasetProperty() {
        Dataset ds = new Dataset();

        String value = RandomStringUtils.randomAlphanumeric(20);

        DatasetProperty dsp = new DatasetProperty();
        dsp.setPropertyValue(value);
        dsp.setPropertyType(PropertyType.String);
        ds.setProperties(Set.of(dsp));

        Assertions.assertTrue(ds.isStringMatch(value));
        Assertions.assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(25)));
    }

    @Test
    public void testIsStringMatchIdentifier() {
        Dataset ds = new Dataset();
        ds.setAlias(1235);

        Assertions.assertTrue(ds.isStringMatch("DUOS-001235"));
        Assertions.assertTrue(ds.isStringMatch("DUOS"));
        Assertions.assertTrue(ds.isStringMatch("123"));
        Assertions.assertTrue(ds.isStringMatch("001235"));
        Assertions.assertFalse(ds.isStringMatch("DUOS-123456"));
    }

    @Test
    public void testIsStringMatchDataUseCommercial() {
        Dataset ds = new Dataset();

        Assertions.assertFalse(ds.isStringMatch("collaborator"));

        DataUse du = new DataUseBuilder().setCollaboratorRequired(true).build();

        ds.setDataUse(du);

        Assertions.assertTrue(ds.isStringMatch("collaborator"));
        Assertions.assertTrue(ds.isStringMatch("collab"));
    }

    @Test
    public void testIsStringMatchDataUseIrb() {
        Dataset ds = new Dataset();

        Assertions.assertFalse(ds.isStringMatch("irb"));

        DataUse du = new DataUse();
        du.setEthicsApprovalRequired(true);

        ds.setDataUse(du);

        Assertions.assertTrue(ds.isStringMatch("irb"));
        Assertions.assertTrue(ds.isStringMatch("irb"));
    }

    @Test
    public void testIsStringMatchDataUseDiseases() {
        Dataset ds = new Dataset();

        Assertions.assertFalse(ds.isStringMatch("cancer"));
        Assertions.assertFalse(ds.isStringMatch("alzheimers"));

        DataUse du = new DataUse();
        du.setDiseaseRestrictions(List.of("cancer", "alzheimers"));

        ds.setDataUse(du);

        Assertions.assertTrue(ds.isStringMatch("cancer"));
        Assertions.assertTrue(ds.isStringMatch("alzheimers"));
    }

    @Test
    public void testIsStringMatchMultipleTerms() {
        Dataset ds = new Dataset();

        ds.setName("asdf");
        ds.setAlias(1234);

        Assertions.assertTrue(ds.isStringMatch("ASD DUOS-001234"));
        Assertions.assertTrue(ds.isStringMatch("asdf 123"));

        Assertions.assertFalse(ds.isStringMatch("asf DUOS-001234"));
        Assertions.assertFalse(ds.isStringMatch("asd 122"));

    }
}
