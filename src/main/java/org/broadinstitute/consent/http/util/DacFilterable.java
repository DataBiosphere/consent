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

    default List<Integer> getDacIdsForUser(DacDAO dacDAO, AuthUser authUser) {
        return dacDAO.findDacsForEmail(authUser.getName())
                .stream()
                .map(Dac::getDacId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Filter consents by the DAC they are associated with.
     * Consents that are not associated to any DAC are also considered valid.
     * In essence, we are filtering out consents associated to DACs the user is not a member of.
     */
    default List<ConsentManage> filterConsentManageByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                         List<ConsentManage> consentManages,
                                                         AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);

        // Non-DAC users can only see unassociated consents
        if (dacIds.isEmpty()) {
            return consentManages.
                    stream().
                    filter(c -> c.getDacId() == null).
                    collect(Collectors.toList());
        }

        return consentManages.stream().
                filter(c -> c.getDacId() == null || dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    /**
     * Filter consents by the DAC they are associated with.
     * Consents that are not associated to any DAC are also considered valid.
     * In essence, we are filtering out consents associated to DACs the user is not a member of.
     */
    default Collection<Consent> filterConsentsByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                    Collection<Consent> consents,
                                                    AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return consents;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);

        // Non-DAC users can only see unassociated consents
        if (dacIds.isEmpty()) {
            return consents.
                    stream().
                    filter(c -> c.getDacId() == null).
                    collect(Collectors.toList());
        }

        return consents.
                stream().
                filter(c -> c.getDacId() == null || dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    /**
     * Filter elections by the Dataset/DAC they are associated with.
     * Elections that are not associated to any Dataset/DAC are also considered valid.
     * In essence, we are filtering out elections associated to Datasets/DACs the user is not a member of.
     */
    default List<Election> filterElectionsByDAC(DacDAO dacDAO, DACUserDAO dacUserDAO,
                                                DataSetDAO dataSetDAO, List<Election> elections,
                                                AuthUser authUser) {
        if (isAuthUserAdmin(dacUserDAO, authUser)) {
            return elections;
        }
        List<Integer> dacIds = getDacIdsForUser(dacDAO, authUser);

        // Non-DAC users can only see unassociated elections
        if (dacIds.isEmpty()) {
            return elections.
                    stream().
                    filter(e -> e.getDataSetId() == null).
                    collect(Collectors.toList());
        }

        List<Integer> userDataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return elections.stream().
                filter(e -> e.getDataSetId() == null || userDataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

}
