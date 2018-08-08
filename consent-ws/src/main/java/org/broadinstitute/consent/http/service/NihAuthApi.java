package org.broadinstitute.consent.http.service;

import io.jsonwebtoken.security.SignatureException;
import java.util.Map;

public interface NihAuthApi {

    Map<String, String> authenticateNih(String jwt, Integer userId) throws SignatureException;

    void deleteNihAccountById(Integer userId);
}
