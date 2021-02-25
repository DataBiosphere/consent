package org.broadinstitute.consent.http.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.bson.Document;

public class DarUtil {

    public static boolean requiresManualReview(DataAccessRequest dar) {
        return
            Objects.nonNull(dar.getData()) && (
                (Objects.nonNull(dar.getData().getPoa()) && dar.getData().getPoa()) ||
                (Objects.nonNull(dar.getData().getPopulation()) && dar.getData().getPopulation()) ||
                (Objects.nonNull(dar.getData().getOther()) && dar.getData().getOther()) ||
                (Objects.nonNull(dar.getData().getOtherText()) && !dar.getData().getOtherText().strip().isEmpty()) ||
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
