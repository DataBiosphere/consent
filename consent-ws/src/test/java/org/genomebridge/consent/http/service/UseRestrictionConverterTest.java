package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class UseRestrictionConverterTest {

    private String femaleData = "{  "
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
            + "   \"population\":false,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":true,"
            + "   \"pediatric\":false,"
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

    private String maleData = "{  "
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
            + "   \"population\":false,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":true,"
            + "   \"pediatric\":false,"
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
            + "   \"gender\":\"M\""
            + "}";

        private String boysData = "{  "
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
            + "   \"population\":false,"
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
            + "   \"gender\":\"M\""
            + "}";

        private String girlsData = "{  "
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
            + "   \"population\":false,"
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

        private String childrensData = "{  "
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
            + "   \"population\":false,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":false,"
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
            + "   ]"
            + "}";

        private String allData = "{  "
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
            + "   \"population\":false,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":false,"
            + "   \"pediatric\":false,"
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
            + "   ]"
            + "}";

        private String singleData = "{  "
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
            + "   \"population\":false,"
            + "   \"forProfit\":true,"
            + "   \"onegender\":false,"
            + "   \"pediatric\":false,"
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
            + "      }"
            + "   ]"
            + "}";
        
    String methods = "http://www.broadinstitute.org/ontologies/DURPO/methods_research";
    String population = "http://www.broadinstitute.org/ontologies/DURPO/population";
    String men = "http://www.broadinstitute.org/ontologies/DURPO/male";
    String women = "http://www.broadinstitute.org/ontologies/DURPO/female";
    String profit = "http://www.broadinstitute.org/ontologies/DURPO/For_profit";
    String nonProfit = "http://www.broadinstitute.org/ontologies/DURPO/Non_profit";
    String pediatric = "http://www.broadinstitute.org/ontologies/DURPO/children";
    String girls = "http://www.broadinstitute.org/ontologies/DURPO/girls";
    String boys = "http://www.broadinstitute.org/ontologies/DURPO/boys";    

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
        UseRestrictionConfig config = new UseRestrictionConfig();

        config.setMale(men);
        config.setMethods(methods);
        config.setNonProfit(nonProfit);
        config.setPediatric(pediatric);
        config.setPopulation(population);
        config.setProfit(profit);
        config.setFemale(women);
        config.setBoys(boys);
        config.setGirls(girls);

        UseRestrictionConverter instance = new UseRestrictionConverter(config);
        UseRestriction result = instance.parseJsonFormulary(femaleData);
        System.out.println(result.toString());
        assertTrue(result.toString().contains("DURPO/female"));

        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(maleData);
        System.out.println(result.toString());
        assertTrue(result.toString().contains("DURPO/male"));

        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(girlsData);
        System.out.println(result.toString());
        assertTrue(result.toString().contains("DURPO/girls"));

        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(boysData);
        System.out.println(result.toString());
        assertTrue(result.toString().contains("DURPO/boys"));

        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(childrensData);
        System.out.println(result.toString());
        assertTrue(result.toString().contains("DURPO/children"));

        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(allData);
        System.out.println(result.toString());
        assertFalse(result.toString().contains("DURPO/children"));
        assertFalse(result.toString().contains("DURPO/boys"));
        assertFalse(result.toString().contains("DURPO/girls"));
        assertFalse(result.toString().contains("DURPO/female"));
        assertFalse(result.toString().contains("DURPO/male"));
    
        instance = new UseRestrictionConverter(config);
        result = instance.parseJsonFormulary(singleData);
        System.out.println(result.toString());

    }

}
