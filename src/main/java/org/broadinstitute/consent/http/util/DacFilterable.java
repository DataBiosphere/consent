package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface DacFilterable {

    default boolean isAuthUserAdmin(DACUserDAO dacUserDAO, AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmail(authUser.getName());
        return user.getRoles().stream().
                anyMatch(ur -> ur.getRoleId().equals(UserRoles.ADMIN.getRoleId()));
    }

    default boolean isAuthUserChair(DACUserDAO dacUserDAO, AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmail(authUser.getName());
        return user.getRoles().stream().
                anyMatch(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()));
    }

    default List<ConsentManage> filterConsentManageByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                         List<ConsentManage> consentManages,
                                                         AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consentManages.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    default List<Integer> getDacIdsForUser(DacDAO dacDAO, AuthUser authUser) {
        return dacDAO.findDacsForEmail(authUser.getName())
                .stream()
                .map(Dac::getDacId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    default Collection<Consent> filterConsentsByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                    Collection<Consent> consents,
                                                    AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return consents;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consents.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    default List<Election> filterElectionsByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                DataSetDAO dataSetDAO, List<Election> elections,
                                                AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return elections;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return elections.stream().
                filter(e -> dataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

}
