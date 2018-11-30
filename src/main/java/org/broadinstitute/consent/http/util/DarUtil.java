package org.broadinstitute.consent.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.bson.Document;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class DarUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Map<String, Object> parseAsMap(String str) throws IOException {
        ObjectReader reader = mapper.readerFor(Map.class);
        return reader.readValue(str);
    }

    public static boolean requiresManualReview(Document dar) throws IOException {
        Map<String, Object> form = parseAsMap(dar.toJson());
        List<String> fieldsForManualReview = Arrays.asList(
                        "population",
                        "other",
                        "illegalbehave",
                        "addiction",
                        "sexualdiseases",
                        "stigmatizediseases",
                        "vulnerablepop",
                        "popmigration",
                        "psychtraits",
                        "nothealth");

        return !fieldsForManualReview.stream().
                filter(field -> form.containsKey(field) && Boolean.valueOf(form.get(field).toString())).collect(Collectors.toList()).isEmpty();
    }

    public static  List<Integer> getIntegerList(Document dar, String key) {
        List<Object> datasets = dar.get(key, List.class);
        return datasets.stream().
                filter(Integer.class::isInstance).
                map(Integer.class::cast).
                collect(Collectors.toList());
    }

}
