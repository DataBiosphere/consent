package org.broadinstitute.consent.http.job;


import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.On;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@OnApplicationStart
@On("0 00 00 * * ?")
public class ElectionJob extends Job {

    private static final Logger logger = LoggerFactory.getLogger("ElectionJob");
    private ElectionAPI electionAPI;

    @Override
    public void doJob() {
        getElectionAPI();
        List<Election> electionList = electionAPI.findExpiredElections(ElectionType.DATA_SET.getValue());
        if (CollectionUtils.isNotEmpty(electionList)) {
            logger.info("closing elections..");
            electionList.stream().forEach(election -> {
                getElectionAPI().closeDataOwnerApprovalElection(election.getElectionId());
            });
        }
    }

    protected void setElectionAPI(ElectionAPI electionAPI) {
        this.electionAPI = electionAPI;
    }

    protected ElectionAPI getElectionAPI() {
        if (electionAPI == null) {
            electionAPI = AbstractElectionAPI.getInstance();
        }
        return electionAPI;
    }


}
