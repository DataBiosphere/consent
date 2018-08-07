package org.broadinstitute.consent.http.service;

import io.jsonwebtoken.io.DecodingException;

import java.sql.BatchUpdateException;
import java.util.Map;
import javax.ws.rs.core.Response;

public interface NihAuthApi {

    Map<String, String> authenticateNih(String jwt, Integer userId) throws DecodingException;

    String generateToken();

    Response deleteNihAccountById(Integer userId) throws BatchUpdateException;
}
