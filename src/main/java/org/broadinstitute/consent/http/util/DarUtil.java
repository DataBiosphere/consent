package org.broadinstitute.consent.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.bson.Document;


public class DarUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Map<String, Object> parseAsMap(String str) throws IOException {
        ObjectReader reader = mapper.readerFor(Map.class);
        return reader.readValue(str);
    }

    public static boolean requiresManualReview(DataAccessRequest dar) {
        DataAccessRequestData data = dar.getData();
        return data.getPopulation() || data.getOther() || data.getIllegalBehavior() ||
                data.getAddiction() || data.getSexualDiseases() || data.getStigmatizedDiseases() ||
                data.getVulnerablePopulation() || data.getPopulationMigration() || data.getPsychiatricTraits() ||
                data.getNotHealth();
    }

    public static boolean darRequiresManualReview(DataAccessRequest dar) {
        return
            Objects.nonNull(dar.getData()) && (
                (Objects.nonNull(dar.getData().getPopulation()) && dar.getData().getPopulation()) ||
                (Objects.nonNull(dar.getData().getOther()) && dar.getData().getOther()) ||
                (Objects.nonNull(dar.getData().getIllegalBehavior()) && dar.getData().getIllegalBehavior()) ||
                (Objects.nonNull(dar.getData().getAddiction()) && dar.getData().getAddiction()) ||
                (Objects.nonNull(dar.getData().getSexualDiseases()) && dar.getData().getSexualDiseases()) ||
                (Objects.nonNull(dar.getData().getStigmatizedDiseases()) && dar.getData().getStigmatizedDiseases()) ||
                (Objects.nonNull(dar.getData().getVulnerablePopulation()) && dar.getData().getVulnerablePopulation()) ||
                (Objects.nonNull(dar.getData().getPopulationMigration()) && dar.getData().getPopulationMigration()) ||
                (Objects.nonNull(dar.getData().getPsychiatricTraits()) && dar.getData().getPsychiatricTraits()) ||
                (Objects.nonNull(dar.getData().getNotHealth()) && dar.getData().getNotHealth())
            );
    }

    @Deprecated // Use darRequiresManualReview(DataAccessRequest dar)
    public static boolean requiresManualReview(Document dar) throws IOException {
        Map<String, Object> form = parseAsMap(dar.toJson());
        List<String> fieldsForManualReview = Arrays.asList(
            DarConstants.POPULATION,
            DarConstants.OTHER,
            DarConstants.ILLEGAL_BEHAVE,
            DarConstants.ADDICTION,
            DarConstants.SEXUAL_DISEASES,
            DarConstants.STIGMATIZED_DISEASES,
            DarConstants.VULNERABLE_POP,
            DarConstants.POP_MIGRATION,
            DarConstants.PSYCH_TRAITS,
            DarConstants.NOT_HEALTH);

        return !fieldsForManualReview.stream().
                filter(field -> form.containsKey(field) && Boolean.valueOf(form.get(field).toString())).collect(Collectors.toList()).isEmpty();
    }

    public static List<Integer> getIntegerList(Document dar, String key) {
        List<?> datasets = dar.get(key, List.class);
        if (Objects.nonNull(datasets)) {
            return datasets.stream().
                filter(Objects::nonNull).
                map(o -> Integer.valueOf(o.toString())).
                collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
