package org.genomebridge.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.MatchDAO;
import org.genomebridge.consent.http.models.Match;
import javax.ws.rs.NotFoundException;
import java.util.List;


/**
 * Implementation class for MatchAPI.
 */
public class DatabaseMatchAPI extends AbstractMatchAPI {

    private MatchDAO matchDAO;
    private ConsentDAO consentDAO;
    private DataAccessRequestAPI accessAPI;


    public static void initInstance(MatchDAO matchDAO,  ConsentDAO consentDAO) {
        MatchAPIHolder.setInstance(new DatabaseMatchAPI(matchDAO, consentDAO));
    }

    private DatabaseMatchAPI(MatchDAO matchDAO, ConsentDAO consentDAO) {
        this.matchDAO = matchDAO;
        this.accessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentDAO = consentDAO;

    }

    @Override
    public Match create(Match match){
        validateConsent(match.getConsent());
        validatePurpose(match.getPurpose());
        try{
            Integer id = matchDAO.insertMatch(match.getConsent(), match.getPurpose(), match.getMatch());
            return findMatchById(id);
        }catch (Exception e){
            throw new IllegalArgumentException("Already exist a match for the specified consent and purpose");
        }
    }

    @Override
    public Match update(Match match, Integer id) {
        validateConsent(match.getConsent());
        validatePurpose(match.getPurpose());
        if (matchDAO.findMatchById(id) == null)
            throw new NotFoundException("Match for the specified id does not exist");
        matchDAO.updateMatch(match.getMatch(), match.getConsent(), match.getPurpose());
        return findMatchById(id);
    }


    @Override
    public Match findMatchById(Integer id) {
        Match match = matchDAO.findMatchById(id);
        if (match == null) {
            throw new NotFoundException("Match for the specified id does not exist");
        }
        return match;
    }

    @Override
    public List<Match> findMatchByConsentId(String consentId) {
        List<Match> matches = matchDAO.findMatchByConsentId(consentId);
        if (CollectionUtils.isEmpty(matches)) {
            throw new NotFoundException("Match for the specified consent id does not exist");
        }
        return matches;
    }

    @Override
    public List<Match> findMatchByPurposeId(String purposeId) {
        List<Match> matches = matchDAO.findMatchByPurposeId(purposeId);
        if (CollectionUtils.isEmpty(matches)) {
            throw new NotFoundException("Match for the specified consent id does not exist");
        }
        return matches;
    }

    @Override
    public void deleteMatch(Integer id) {
        matchDAO.deleteMatch(id);
    }

    private void validateConsent(String consentId) {
        if (StringUtils.isEmpty(consentDAO.checkConsentbyId(consentId))) {
            throw new IllegalArgumentException("Consent for the specified id does not exist");
        }
    }

    private void validatePurpose(String purposeId) {
        if (accessAPI.describeDataAccessRequestById(purposeId) == null) {
            throw new IllegalArgumentException("Purpose for the specified id does not exist");
        }
    }
}
