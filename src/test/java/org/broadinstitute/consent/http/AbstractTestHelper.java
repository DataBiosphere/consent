package org.broadinstitute.consent.http;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public abstract class AbstractTestHelper {

  public static boolean enableTestContainers() {
    String defaultProp = "true";
    return Boolean.parseBoolean(System.getProperty("enableTestContainers", defaultProp));
  }

  public static String randomAlphabetic(int length) {
    return RandomStringUtils.secureStrong().nextAlphabetic(length);
  }

  public static String randomAlphanumeric(int length) {
    return RandomStringUtils.secureStrong().nextAlphanumeric(length);
  }

  public static int randomInt(int startInclusive, int endExclusive) {
    return RandomUtils.secureStrong().randomInt(startInclusive, endExclusive);
  }

}
