package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

import java.io.IOException;
import java.util.HashMap;

public class DataRequestSamplesHolder {

    private static final String completeDARJson = "{\n" +
            "    \"investigator\" : \"Somenone\",\n" +
            "    \"institution\" : \"npsrpn\",\n" +
            "    \"department\" : \"pnifnpvdpi\",\n" +
            "    \"division\" : \"noyon\",\n" +
            "    \"address1\" : \"lñubbi\",\n" +
            "    \"address2\" : \"uoñn\",\n" +
            "    \"city\" : \"n\",\n" +
            "    \"state\" : \"b\",\n" +
            "    \"zipcode\" : \"bj\",\n" +
            "    \"country\" : \"bubu\",\n" +
            "    \"projectTitle\" : \"bb\",\n" +
            "    \"rus\" : \"cerrefer\",\n" +
            "    \"non_tech_rus\" : \"frevrvre\",\n" +
            "    \"diseases\" : true,\n" +
            "    \"controls\" : true,\n" +
            "    \"onegender\" : false,\n" +
            "    \"forProfit\" : false,\n" +
            "    \"pediatric\" : true,\n" +
            "    \"illegalbehave\" : false,\n" +
            "    \"sexualdiseases\" : false,\n" +
            "    \"stigmatizediseases\" : true,\n" +
            "    \"popmigration\" : false,\n" +
            "    \"vulnerablepop\" : false,\n" +
            "    \"psychtraits\" : true,\n" +
            "    \"nothealth\" : true,\n" +
            "    \"userId\" : 3333,\n" +
            "    \"addiction\" : false,\n" +
            "    \"translated_restriction\" : \"Samples will be used under the following conditions:<br>Needs Manual Review.<br>Data will be used for health/medical/biomedical research [HMB(CC)]<br>Data will be used as a control sample set [NCTRL=0]<br>Data will not be used for commercial purpose<br>Data will be used to study ONLY a pediatric population [RS-[PEDIATRIC]]<br>\",\n" +
            "    \"datasetId\" : [ \n" +
            "        \"SC-20660\"\n" +
            "    ],\n" +
            "    \"datasetDetail\" : [ \n" +
            "        {\n" +
            "            \"datasetId\" : \"SC-20660\",\n" +
            "            \"name\" : \"test\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"dar_code\" : \"DAR-1\"\n" +
            "}";

    private static final String partialDarJson = "{\n" +
            "    \"investigator\" : \"Someone\",\n" +
            "    \"institution\" : \"divre\",\n" +
            "    \"department\" : \"nnl\",\n" +
            "    \"division\" : \"ml\",\n" +
            "    \"address1\" : \"n\",\n" +
            "    \"address2\" : \"n\",\n" +
            "    \"city\" : \"n\",\n" +
            "    \"state\" : \"ln\",\n" +
            "    \"zipcode\" : \"ln\",\n" +
            "    \"country\" : \"lknlknlknl\",\n" +
            "    \"projectTitle\" : \"nlnlnl\",\n" +
            "    \"datasetId\" : [ \n" +
            "        {\n" +
            "            \"concatenation\" : \"SC-20660   test\",\n" +
            "            \"id\" : \"SC-20660\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"rus\" : \"dwefwef\",\n" +
            "    \"non_tech_rus\" : \"cefer\",\n" +
            "    \"diseases\" : true,\n" +
            "    \"controls\" : true,\n" +
            "    \"userId\" : 3333,\n" +
            "    \"partial_dar_code\" : \"temp_DAR0\"\n" +
            "}";

    public static Document getSampleDar() throws IOException {
        Document document = new Document();
        document.putAll(jsonAsMap(completeDARJson));
        return document;
    }

    public static Document getSamplePartialDar() throws IOException {
        Document document = new Document();
        document.putAll(jsonAsMap(partialDarJson));
        return document;
    }

    private static HashMap<String,Object> jsonAsMap(String jsonSource) throws IOException {
        HashMap<String,Object> result = new ObjectMapper().readValue(jsonSource, HashMap.class);
        return result;
    }

}
