package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.UseRestrictionConfig;
import org.broadinstitute.consent.http.models.grammar.*;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class UseRestrictionConverterTest {

    UseRestriction femaleRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
            ),
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/female")
            )
    );

    UseRestriction allDataRestriction = new And(
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/control")
            ),
            new Or(
                    new Named("http://purl.obolibrary.org/obo/DOID_4023"),
                    new Named("http://purl.obolibrary.org/obo/DOID_9854"),
                    new Named("http://purl.obolibrary.org/obo/DOID_0050738")

            ),
            new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit")
    );

    UseRestriction maleRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
            ),
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/male")
            )
    );

    UseRestriction boysRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
            ),
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/male"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/children")
            )
    );

    UseRestriction girlsRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
            ),
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/female"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/children")
            )
    );

    UseRestriction childrenRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure")),
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
            ),
            new And(
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/For_profit"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/children")
            )
    );

    UseRestriction controlsPopulationRestriction = new And(
            new And(
                    new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/methods_research")),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/population_structure"),
                    new Named("http://www.broadinstitute.org/ontologies/DUOS/control")
            ),
            new Named("http://www.broadinstitute.org/ontologies/DUOS/Non_profit")
    );



    String femaleData = "{  "
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
            + "   \"methods\":false,"
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
            + "   \"gender\":\"F\""
            + "}";

    String maleData = "{  "
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
            + "   \"methods\":false,"
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
            + "   \"gender\":\"M\""
            + "}";

     String boysData = "{  "
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
            + "   \"diseases\":false,"
            + "   \"methods\":false,"
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
            + "   \"gender\":\"M\""
            + "}";

      String girlsData = "{  "
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
            + "   \"methods\":false,"
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
            + "   \"gender\":\"F\""
            + "}";

       String childrensData = "{  "
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
            + "   \"methods\":false,"
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
            + "   \"nothealth\":false"
            + "}";



        String allData = "{  "
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
            + "   \"controls\":true,"
            + "   \"population\":true,"
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


         String controlsAndPopulationData = "{  "
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
            + "   \"diseases\":false,"
            + "   \"methods\":false,"
            + "   \"controls\":true,"
            + "   \"population\":true,"
            + "   \"forProfit\":false,"
            + "   \"onegender\":false,"
            + "   \"pediatric\":false,"
            + "   \"illegalbehave\":false,"
            + "   \"addiction\":false,"
            + "   \"sexualdiseases\":false,"
            + "   \"stigmatizediseases\":false,"
            + "   \"vulnerablepop\":false,"
            + "   \"popmigration\":false,"
            + "   \"psychtraits\":false,"
            + "   \"nothealth\":false"
            + "}";


    String methods = "http://www.broadinstitute.org/ontologies/DUOS/methods_research";
    String aggregate = "http://www.broadinstitute.org/ontologies/DUOS/aggregate_research";
    String population = "http://www.broadinstitute.org/ontologies/DUOS/population_structure";
    String men = "http://www.broadinstitute.org/ontologies/DUOS/male";
    String women = "http://www.broadinstitute.org/ontologies/DUOS/female";
    String profit = "http://www.broadinstitute.org/ontologies/DUOS/For_profit";
    String nonProfit = "http://www.broadinstitute.org/ontologies/DUOS/Non_profit";
    String pediatric = "http://www.broadinstitute.org/ontologies/DUOS/children";
    String girls = "http://www.broadinstitute.org/ontologies/DUOS/girls";
    String boys = "http://www.broadinstitute.org/ontologies/DUOS/boys";
    String controls = "http://www.broadinstitute.org/ontologies/DUOS/control";

    public UseRestrictionConfig config() {
        UseRestrictionConfig config = new UseRestrictionConfig();

        config.setMale(men);
        config.setMethods(methods);
        config.setAggregate(aggregate);
        config.setNonProfit(nonProfit);
        config.setPediatric(pediatric);
        config.setPopulation(population);
        config.setProfit(profit);
        config.setFemale(women);
        config.setBoys(boys);
        config.setGirls(girls);
        config.setControls(controls);
        return  config;
    }

    @Test
    public void testParseFemaleData(){
        UseRestrictionConverter instance = new UseRestrictionConverter(config());
        UseRestriction result = instance.parseJsonFormulary(femaleData);
        assertTrue(result.toString().equals(femaleRestriction.toString()));
    }

    @Test
    public void testParseMaleData(){
        UseRestrictionConverter  instance = new UseRestrictionConverter(config());
        UseRestriction result = instance.parseJsonFormulary(maleData);
        assertTrue(result.toString().equals(maleRestriction.toString()));
    }

    @Test
    public void testParseGirlsData(){
        UseRestrictionConverter   instance = new UseRestrictionConverter(config());
        UseRestriction  result = instance.parseJsonFormulary(girlsData);
        assertTrue(result.toString().equals(girlsRestriction.toString()));
    }

    @Test
    public void testParseBoysData() {
        UseRestrictionConverter   instance = new UseRestrictionConverter(config());
        UseRestriction  result = instance.parseJsonFormulary(boysData);
        assertTrue(result.toString().equals(boysRestriction.toString()));
    }

    @Test
    public void testParseChildrenData() {
        UseRestrictionConverter   instance = new UseRestrictionConverter(config());
        UseRestriction  result = instance.parseJsonFormulary(childrensData);
        assertTrue(result.toString().equals(childrenRestriction.toString()));
    }

    @Test
    public void testParseAllData() {
        UseRestrictionConverter   instance = new UseRestrictionConverter(config());
        UseRestriction  result = instance.parseJsonFormulary(allData);
        assertTrue(result.toString().equals(allDataRestriction.toString()));
    }

    @Test
    public void testParseControlsAndPopulationData() {
        UseRestrictionConverter   instance = new UseRestrictionConverter(config());
        UseRestriction  result = instance.parseJsonFormulary(controlsAndPopulationData);
        assertTrue(result.toString().equals(controlsPopulationRestriction.toString()));
    }

}
