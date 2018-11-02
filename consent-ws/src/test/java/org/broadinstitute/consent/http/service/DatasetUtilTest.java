package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.util.DatasetUtil;
import org.junit.Assert;
import org.junit.Test;

public class DatasetUtilTest {

    private final String ALIAS_1 = "DUOS-000008";
    private final String ALIAS_2 = "DUOS-000010";
    private final String ALIAS_3 = "DUOS-000088";
    private final String ALIAS_4 = "DUOS-000150";
    private final String ALIAS_5 = "DUOS-001001";
    private final String ALIAS_6 = "DUOS-010080";
    private final String ALIAS_7 = "DUOS-1000888";


    @Test
    public void testAliasLessThan10(){
       String alias = DatasetUtil.parseAlias(8);
        Assert.assertTrue(alias.equals(ALIAS_1));
    }


    @Test
    public void testAliasEquals10(){
        String alias = DatasetUtil.parseAlias(10);
        Assert.assertTrue(alias.equals(ALIAS_2));
    }


    @Test
    public void testAliasGreatherThan10(){
        String alias = DatasetUtil.parseAlias(88);
        Assert.assertTrue(alias.equals(ALIAS_3));
    }

    @Test
    public void testAliasGreatherThan100(){
        String alias = DatasetUtil.parseAlias(150);
        Assert.assertTrue(alias.equals(ALIAS_4));
    }


    @Test
    public void testAliasGreatherThan1000(){
        String alias = DatasetUtil.parseAlias(1001);
        Assert.assertTrue(alias.equals(ALIAS_5));
    }


    @Test
    public void testAliasGreatherThan10000(){
        String alias = DatasetUtil.parseAlias(10080);
        Assert.assertTrue(alias.equals(ALIAS_6));
    }

    @Test
    public void testAliasGreatherThan100000(){
        String alias = DatasetUtil.parseAlias(1000888);
        Assert.assertTrue(alias.equals(ALIAS_7));
    }
}
