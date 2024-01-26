package org.broadinstitute.consent.http.models.passport;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public interface VisaClaimType {
  String type();
  Integer asserted();
  String value();
  String source();
  String by();
  default List<VisaCondition> conditions() {
    return List.of();
  }
}
