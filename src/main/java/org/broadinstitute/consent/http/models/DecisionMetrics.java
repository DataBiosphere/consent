package org.broadinstitute.consent.http.models;

import java.util.concurrent.TimeUnit;

public interface DecisionMetrics {

    String toString(String joiner);

    default Integer convertMillisToDays(Long tot) {
        //this will only ever catch an exception if the number of days
        //exceeds 2147483647 and thus can't be converted to an integer
        try {
            return Math.toIntExact(TimeUnit.MILLISECONDS.toDays(tot));
        } catch (ArithmeticException e) {
            return 2147483647;
        }
    }

}
