package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.Not;
import org.broadinstitute.consent.http.models.grammar.Nothing;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.grammar.Some;
import org.broadinstitute.consent.http.models.grammar.Only;
import org.broadinstitute.consent.http.models.grammar.Or;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for full object deserialization from valid json strings for each of the grammar cases.
 */
public class RestrictionParserTest {

    @Test
    public void testAnd() throws Exception {
        And restriction = (And) UseRestriction.parse("{\"type\":\"and\",\"operands\":[{\"type\":\"named\",\"name\":\"DOID:1\"},{\"type\":\"named\",\"name\":\"DOID:2\"}]}");
        Assert.assertTrue(restriction.getType().equals("and"));
        Assert.assertTrue(restriction.getOperands().length == 2);
    }

    @Test
    public void testEverything() throws Exception {
        Everything restriction = (Everything) UseRestriction.parse("{\"type\":\"everything\"}");
        Assert.assertTrue(restriction.getType().equals("everything"));
    }

    @Test
    public void testNamed() throws Exception {
        Named restriction = (Named) UseRestriction.parse("{\"type\":\"named\",\"name\":\"DOID:1\"}");
        Assert.assertTrue(restriction.getType().equals("named"));
        Assert.assertTrue(restriction.getName().equals("DOID:1"));
    }

    @Test
    public void testNot() throws Exception {
        Not restriction = (Not) UseRestriction.parse("{\"type\":\"not\",\"operand\":{\"type\":\"named\",\"name\":\"DOID:1\"}}");
        Assert.assertTrue(restriction.getType().equals("not"));
    }

    @Test
    public void testNothing() throws Exception {
        Nothing restriction = (Nothing) UseRestriction.parse("{\"type\":\"nothing\"}");
        Assert.assertTrue(restriction.getType().equals("nothing"));
    }

    @Test
    public void testOnly() throws Exception {
        Only restriction = (Only) UseRestriction.parse("{\"type\":\"only\",\"property\":\"methods\",\"target\":{\"type\":\"named\",\"name\":\"DOID:1\"}}");
        Assert.assertTrue(restriction.getType().equals("only"));
        Assert.assertTrue(restriction.getProperty().equals("methods"));
        Assert.assertTrue(restriction.getTarget().toString().contains("DOID:1"));
    }

    @Test
    public void testOr() throws Exception {
        Or restriction = (Or) UseRestriction.parse("{\"type\":\"or\",\"operands\":[{\"type\":\"named\",\"name\":\"DOID:1\"},{\"type\":\"named\",\"name\":\"DOID:2\"}]}");
        Assert.assertTrue(restriction.getType().equals("or"));
        Assert.assertTrue(restriction.getOperands().length == 2);
    }

    @Test
    public void testSome() throws Exception {
        Some restriction = (Some) UseRestriction.parse("{\"type\":\"some\",\"property\":\"methods\",\"target\":{\"type\":\"named\",\"name\":\"DOID:1\"}}");
        Assert.assertTrue(restriction.getType().equals("some"));
        Assert.assertTrue(restriction.getProperty().equals("methods"));
        Assert.assertTrue(restriction.getTarget().toString().contains("DOID:1"));
    }

}
