package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
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
        DACUser user = dacUserDAO.findDACUserByEmailAndRoleId(authUser.getName(), UserRoles.ADMIN.getRoleId());
        return user != null;
    }

    default boolean isAuthUserChair(DACUserDAO dacUserDAO, AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmailAndRoleId(authUser.getName(), UserRoles.CHAIRPERSON.getRoleId());
        return user != null;
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

        List<Integer> userDataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());

//        List<String> referenceIds = elections.
//                stream().
//                map(Election::getReferenceId).
//                collect(Collectors.toList());
//
//        List<Integer> consentDataSetIds = dataSetDAO.
//                findDataSetsByConsentIds(referenceIds).
//                stream().
//                map(DataSet::getDataSetId).
//                collect(Collectors.toList());
//
//        /*
//            Types:
//            DATA_ACCESS("DataAccess"),
//            TRANSLATE_DUL("TranslateDUL"),
//            RP("RP"),
//            DATA_SET("DataSet");
//
//            Election -> TranslateDUL -> filter by dataset ids in consents (reference id = consent id)
//                     -> DATA_ACCESS -> filter by DAR dataset IDs in DAR (reference id = dar id)
//                     -> RP -> Same as DATA_ACCESS ... we're voting on the RP of the DAR.
//                     -> DATA_SET -> filter by dataset ids in election (dataset id = dataset id)
//         */
//
//        elections.stream().filter(e -> {
//            ElectionType type = ElectionType.valueOf(e.getElectionType());
//            switch (type) {
//                case DATA_ACCESS:
//                    return true;
//                case RP:
//                    return true;
//                case TRANSLATE_DUL:
//                    return true;
//                case DATA_SET:
//                    return true;
//                default:
//                    return true;
//            }
//        }).collect(Collectors.toList());

        return elections.stream().
                filter(e -> userDataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

}
