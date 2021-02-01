package org.broadinstitute.consent.http.models;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
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

  default String getDacUID(String name) {
    if (Objects.nonNull(name)) {
      for (int i = 0; i < name.length(); i++) {
        if (Character.isDigit(name.charAt(i))) {
          String uid = StringUtils.leftPad(name.substring(i), 6, "0");
          if (i == 0) {
            return uid;
          } else {
            return name.substring(0, i - 1).concat(uid);
          }
        }
      }
    }
    return "";
  }
}

