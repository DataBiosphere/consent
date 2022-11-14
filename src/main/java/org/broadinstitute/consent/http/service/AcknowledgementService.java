package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.User;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AcknowledgementService {
    private final AcknowledgementDAO acknowledgementDAO;

    public AcknowledgementService(AcknowledgementDAO acknowledgementDAO) {
        this.acknowledgementDAO = acknowledgementDAO;
    }

    public Map<String, Acknowledgement> getAcknowledgementsForUser(User user) {
        return acknowledgementListToMap(acknowledgementDAO.getAcknowledgementsForUser(user.getUserId()));
    }

    public Acknowledgement getAcknowledgementForUserByKey(User user, String key) {
        return acknowledgementDAO.getAcknowledgementsByKeyForUser(key, user.getUserId());
    }

    public Map<String, Acknowledgement> makeAcknowledgements(List<String> keys, User user) {
        Integer userId = user.getUserId();
        for (String key : keys) {
            acknowledgementDAO.upsertAcknowledgement(key, userId);
        }
        List<Acknowledgement> acknowledgementList = acknowledgementDAO.getAcknowledgementsForUser(keys, userId);
        return acknowledgementListToMap(acknowledgementList);
    }

    private Map<String, Acknowledgement> acknowledgementListToMap(List<Acknowledgement> acknowledgements){
        return acknowledgements.stream().collect(Collectors.toMap(Acknowledgement::getAck_key, Function.identity()));
    }
}
