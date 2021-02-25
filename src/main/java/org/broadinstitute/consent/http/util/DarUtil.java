package org.broadinstitute.consent.http.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bson.Document;

public class DarUtil {

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
