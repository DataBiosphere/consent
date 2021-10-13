package org.broadinstitute.consent.http.util;

import java.util.Optional;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;

public class DarUtil {

    public static String findPI(User user) {
        if (user != null && user.getProperties() != null) {
            Optional<UserProperty> isResearcher = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("isThePI") && prop.getPropertyValue().equalsIgnoreCase("true")).findFirst();
            if (isResearcher.isPresent()) {
                Optional<UserProperty> userName = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("profileName")).findFirst();
                if (userName.isPresent()) {
                    return userName.get().getPropertyValue();
                }
            }

            Optional<UserProperty> piName = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("piName")).findFirst();
            if (piName.isPresent()) {
              return piName.get().getPropertyValue();
            }
        }
        return "- -";
    }

}
