package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TDRService {
    private final DataAccessRequestService dataAccessRequestService;
    private final DatasetService datasetService;
    private final DatasetDAO datasetDAO;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public TDRService(DataAccessRequestService dataAccessRequestService, DatasetService datasetService, DatasetDAO datasetDAO) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.datasetService = datasetService;
        this.datasetDAO = datasetDAO;
    }

    public ApprovedUsers getApprovedUsersForDataset(Dataset dataset) {
        Collection<User> users = dataAccessRequestService.getUsersApprovedForDataset(dataset);

        List<ApprovedUser> approvedUsers = users
                .stream()
                .map((u) -> new ApprovedUser(u.getEmail()))
                .sorted(Comparator.comparing(ApprovedUser::getEmail))
                .collect(Collectors.toList());

        return new ApprovedUsers(approvedUsers);
    }

    public List<Integer> getDatasetIdsByIdentifier(List<String> identifiers) {
        List<Integer> datasetIds = identifiers
                .stream()
                .filter(identifier -> !identifier.isBlank())
                .map(identifier -> datasetDAO.findDatasetByAlias(Dataset.parseIdentifierToAlias(identifier)))
                // technically, it is possible to have two dataset identifiers which
                // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
                .filter(d -> !identifiers.contains(d.getDatasetIdentifier()))
                .map(d -> d.getDataSetId())
                .toList();

        return datasetIds;
    }
}
