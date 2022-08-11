package org.broadinstitute.consent.http.models;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class DatasetTests {

    @Test
    public void testParseIdentifierToAlias() {
        assertEquals(3, (int)Dataset.parseIdentifierToAlias("DUOS-3"));
        assertEquals(3, (int)Dataset.parseIdentifierToAlias("DUOS-000003"));
        assertEquals(123456, (int)Dataset.parseIdentifierToAlias("DUOS-123456"));

        assertThrows(IllegalArgumentException.class, ()->Dataset.parseIdentifierToAlias("asdf-123456"));
        assertThrows(IllegalArgumentException.class, ()->Dataset.parseIdentifierToAlias("DUOS-1234 56"));
        assertThrows(IllegalArgumentException.class, ()->Dataset.parseIdentifierToAlias("DUOS-1234as56"));
    }
}
