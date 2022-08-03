package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class TDRService {
    private final DataAccessRequestService dataAccessRequestService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Inject
    public TDRService(DataAccessRequestService dataAccessRequestService) {
        this.dataAccessRequestService = dataAccessRequestService;
    }


    public ApprovedUsers getApprovedUsersForDataset(Dataset dataset) {
        List<User> users = this.dataAccessRequestService.getUsersApprovedForDataset(dataset);

        List<ApprovedUser> approvedUsers =
                users.stream().map((u) -> new ApprovedUser(u.getEmail())).collect(Collectors.toList());

        return new ApprovedUsers(approvedUsers);
    }
}
