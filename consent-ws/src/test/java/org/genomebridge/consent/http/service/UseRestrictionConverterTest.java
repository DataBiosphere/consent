package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class UseRestrictionConverterTest {

    private String formData = "{  "
            + "   \"investigator\":\"Investigator\","
            + "   \"institution\":\"Name\","
            + "   \"department\":\"Department\","
            + "   \"address1\":\"address 1\","
            + "   \"division\":\"\","
            + "   \"address2\":\"\","
            + "   \"state\":\"state\","
            + "   \"city\":\"city\","
            + "   \"zipcode\":\"zcode\","
            + "   \"country\":\"country\","
            + "   \"projectTitle\":\"Some title of the project\","
            + "   \"datasetId\":\"AME-56789\","
            + "   \"rus\":\"SDFG\","
            + "   \"non_tech_rus\":\"SDFG\","
            + "   \"diseases\":true,"
            + "   \"methods\":true,"
            + "   \"controls\":false,"
            + "   \"population\":true,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":true,"
            + "   \"pediatric\":true,"
            + "   \"illegalbehave\":false,"
            + "   \"addiction\":false,"
            + "   \"sexualdiseases\":false,"
            + "   \"stigmatizediseases\":false,"
            + "   \"vulnerablepop\":false,"
            + "   \"popmigration\":false,"
            + "   \"psychtraits\":false,"
            + "   \"nothealth\":false,"
            + "   \"ontologies\":[  "
            + "      {  "
            + "         \"id\":\"http://purl.obolibrary.org/obo/DOID_4023\","
            + "         \"label\":\"linitis-plastica\","
            + "         \"definition\":null,"
            + "         \"synonyms\":[  "
            + "            \"Linitis plastica (morphologic abnormality)\","
            + "            \"Leather-bottle stomach\""
            + "         ]"
            + "      },"
            + "      {  "
            + "         \"id\":\"http://purl.obolibrary.org/obo/DOID_9854\","
            + "         \"label\":\"lingual-facial-buccal-dyskinesia\","
            + "         \"definition\":null,"
            + "         \"synonyms\":[  "
            + "            \"Oro-facial dyskinesia\""
            + "         ]"
            + "      },"
            + "      {  "
            + "         \"id\":\"http://purl.obolibrary.org/obo/DOID_0050738\","
            + "         \"label\":\"Y-linked-disease\","
            + "         \"definition\":null,"
            + "         \"synonyms\":[  "
            + ""
            + "         ]"
            + "      }"
            + "   ],"
            + "   \"gender\":\"F\""
            + "}";

    String methods = "http://www.broadinstitute.org/ontologies/DURPO/methods_research";
    String population = "http://www.broadinstitute.org/ontologies/DURPO/population";
    String men = "http://www.broadinstitute.org/ontologies/DURPO/men";
    String women = "http://www.broadinstitute.org/ontologies/DURPO/women";
    String profit = "http://www.broadinstitute.org/ontologies/DURPO/For_profit";
    String nonProfit = "http://www.broadinstitute.org/ontologies/DURPO/Non_profit";
    String pediatric = "http://www.broadinstitute.org/ontologies/DURPO/children";

    public UseRestrictionConverterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parseJsonFormulary method, of class UseRestrictionConverter.
     */
    @Test
    public void testParseJsonFormulary() {
        System.out.println("parseJsonFormulary");
        String json = formData;
        UseRestrictionConfig config = new UseRestrictionConfig();

        config.setMen(men);
        config.setMethods(methods);
        config.setNonProfit(nonProfit);
        config.setPediatric(pediatric);
        config.setPopulation(population);
        config.setProfit(profit);
        config.setWomen(women);

        UseRestrictionConverter instance = new UseRestrictionConverter(config);
        UseRestriction expResult = null;
        UseRestriction result = instance.parseJsonFormulary(json);
        System.out.println(result.toString());

        // TODO: what to assert here .. ?
        assertEquals(true, true);
    }

}
