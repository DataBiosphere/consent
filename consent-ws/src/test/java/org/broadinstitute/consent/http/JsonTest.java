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
 * Test for valid json formats for each of the various grammar cases.
 */
public class JsonTest {

    @Test
    public void testAnd() {
        UseRestriction r = new And(new Named("DOID:1"), new Named("DOID:2"));
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"and\",\"operands\":[{\"type\":\"named\",\"name\":\"DOID:1\"},{\"type\":\"named\",\"name\":\"DOID:2\"}]}"));
    }

    @Test
    public void testEverything() {
        UseRestriction r = new Everything();
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"everything\"}"));
    }

    @Test
    public void testNamed() {
        UseRestriction r = new Named("DOID:1");
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"named\",\"name\":\"DOID:1\"}"));
    }

    @Test
    public void testNot() {
        UseRestriction r = new Not(new Named("DOID:1"));
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"not\",\"operand\":{\"type\":\"named\",\"name\":\"DOID:1\"}}"));
    }

    @Test
    public void testNothing() {
        UseRestriction r = new Nothing();
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"nothing\"}"));
    }

    @Test
    public void testOnly() {
        UseRestriction r = new Only("methods", new Named("DOID:1"));
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"only\",\"property\":\"methods\",\"target\":{\"type\":\"named\",\"name\":\"DOID:1\"}}"));
    }

    @Test
    public void testOr() {
        UseRestriction r = new Or(new Named("DOID:1"), new Named("DOID:2"));
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"or\",\"operands\":[{\"type\":\"named\",\"name\":\"DOID:1\"},{\"type\":\"named\",\"name\":\"DOID:2\"}]}"));
    }

    @Test
    public void testSome() {
        UseRestriction r = new Some("methods", new Named("DOID:1"));
        String json = r.toString();
        Assert.assertTrue(json.equals("{\"type\":\"some\",\"property\":\"methods\",\"target\":{\"type\":\"named\",\"name\":\"DOID:1\"}}"));
    }

}
