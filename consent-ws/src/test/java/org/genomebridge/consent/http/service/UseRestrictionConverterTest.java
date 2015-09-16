package org.genomebridge.consent.http.service;

import com.google.gson.Gson;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class UseRestrictionConverterTest {

    private final String SAMPLE_JSON = "{  \n" +
            "    \"forProfit\" : false,\n"+
            "    \"onegender\" : true,\n"+
            "    \"gender\" : \"M\",\n"+
            "    \"pediatric\" : true,\n"+
            "    \"illegalbehave\" : true,\n"+
            "    \"addiction\" : false,\n"+
            "    \"sexualdiseases\" : false,\n"+
            "    \"stigmatizediseases\" : false,\n"+
            "    \"vulnerablepop\" : false,\n"+
            "    \"psychtraits\" : false,\n"+
            "    \"popmigration\" : true,\n"+
            "    \"nothealth\" : false,\n"+
            "    \"datasetId\" : \"SC-20659\",\n"+
            "    \"rus\" : \"A RUS is a brief description of the applicant’s proposed use of the dataset(s). The RUS will be reviewed by all parties responsible for data covered by this Data Access Request. Please note that if access is approved, you agree that the RUS, along with your name and institution, will be included on this website to describe your research project to the public.\",\n"+
            "    \"non_tech_rus\" : \"Non-Technical summary\",\n"+
            "    \"diseases\" : true,\n"+
            "    \"other\" : true,\n"+
            "    \"othertext\" : \"Other\",\n"+
            "    \"population\" : true,\n"+
            "    \"controls\" : true,\n"+
            "    \"methods\" : true,\n"+
            "    \"ontologies\" : [ \n"+
            "        {\n"+
            "            \"id\" : \"http://purl.obolibrary.org/obo/DOID_9505\",\n"+
            "            \"label\" : \"cannabis-abuse\",\n"+
            "            \"definition\" : \"A substance abuse that involves the recurring use of cannabis despite negative consequences.\",\n"+
            "            \"synonyms\" : [ \n"+
            "                \"marijuana abuse\"\n"+
            "            ]\n"+
            "        }, \n"+
            "        {\n"+
            "            \"id\" : \"http://purl.obolibrary.org/obo/DOID_7603\",\n"+
            "            \"label\" : \"fibrosarcomatous-osteosarcoma\",\n"+
            "            \"definition\" : null,\n"+
            "            \"synonyms\" : [ \n"+
            "                \"Fibrosarcomatous Osteogenic sarcoma\", \n"+
            "                \"Fibroblastic osteosarcoma (morphologic abnormality)\"\n"+
            "            ]\n"+
            "        }\n"+
            "    ],\n"+
            "    \"investigator\" : \"Investigator\",\n"+
            "    \"institution\" : \"InstitutionName\",\n"+
            "    \"department\" : \"Department\",\n"+
            "    \"division\" : \"Division\",\n"+
            "    \"address2\" : \"Street Address 2\",\n"+
            "    \"address1\" : \"Street Address 1\",\n"+
            "    \"city\" : \"City\",\n"+
            "    \"state\" : \"State\",\n"+
            "    \"country\" : \"Argentina\",\n"+
            "    \"zipcode\" : \"1684\",\n"+
            "    \"projectTitle\" : \"Title!!!!\"\n"+
            "}";

    UseRestrictionConfig config;

    private UseRestrictionConverter converter;

    @Before
    public void setUp(){
        config = new UseRestrictionConfig();
        config.setPediatric("FakeChildrenURL");
        config.setMen("FakeMenURL");
        config.setWomen("FakeWomenURL");
        config.setMethods("FakeMethodsURL");
        config.setPopulation("FakePopulationURL");
        config.setProfit("FakeProfitURL");
        config.setNonProfit("FakeNonProfitURL");
        converter = new UseRestrictionConverter(config);
    }

    @Test
    public void testParseJsonFormulary() throws Exception {
        UseRestriction result = converter.parseJsonFormulary(SAMPLE_JSON);
        System.out.println(new Gson().toJson(result));
    }

    @Test
    public void testParseAsMap() throws Exception {
        Map<String, Object> map = converter.parseAsMap(SAMPLE_JSON);
        assertTrue(map.containsKey("vulnerablepop"));
        ArrayList<Object> ont = (ArrayList<Object>) map.get("ontologies");
        assertTrue(ont.size() == 2);
        assertTrue(map.get("gender").equals("M"));
    }
}