package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.AcknowledgmentDAO;
import org.broadinstitute.consent.http.models.Acknowledgment;
import org.broadinstitute.consent.http.models.User;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AcknowledgmentService {
    private final AcknowledgmentDAO acknowledgmentDAO;

    public AcknowledgmentService(AcknowledgmentDAO acknowledgmentDAO) {
        this.acknowledgmentDAO = acknowledgmentDAO;
    }

    public Map<String, Acknowledgment> findAcknowledgmentsForUser(User user) {
        return acknowledgmentListToMap(acknowledgmentDAO.findAcknowledgmentsForUser(user.getUserId()));
    }

    public Acknowledgment findAcknowledgmentForUserByKey(User user, String key) {
        return acknowledgmentDAO.findAcknowledgmentsByKeyForUser(key, user.getUserId());
    }

    public Map<String, Acknowledgment> makeAcknowledgments(List<String> keys, User user) {
        Integer userId = user.getUserId();
        for (String key : keys) {
            acknowledgmentDAO.upsertAcknowledgment(key, userId);
        }
        List<Acknowledgment> acknowledgmentList = acknowledgmentDAO.findAcknowledgmentsForUser(keys, userId);
        return acknowledgmentListToMap(acknowledgmentList);
    }

    private Map<String, Acknowledgment> acknowledgmentListToMap(List<Acknowledgment> acknowledgments){
        return acknowledgments.stream().collect(Collectors.toMap(Acknowledgment::getAckKey, Function.identity()));
    }

    public void deleteAcknowledgmentForUserByKey(User user, String key) {
        acknowledgmentDAO.deleteAcknowledgment(key, user.getUserId());
    }
}
