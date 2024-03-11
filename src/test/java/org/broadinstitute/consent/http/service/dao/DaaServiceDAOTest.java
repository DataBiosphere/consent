package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DaaServiceDAOTest extends DAOTestHelper {

  private static DaaServiceDAO serviceDAO;

  @BeforeEach
  void setUp() {
    serviceDAO = new DaaServiceDAO(jdbi, daaDAO, fileStorageObjectDAO);
  }

  @Test
  void testCreateDaa() {
    User user = createUser();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
    FileStorageObject fso = new FileStorageObject();
    fso.setFileName(RandomStringUtils.randomAlphabetic(10));
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    BlobId blobId = BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10));
    fso.setBlobId(blobId);
    fso.setMediaType(MediaType.TEXT_PLAIN_TYPE.getType());
    assertDoesNotThrow(() -> {
      Integer daaId = serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso);
      assertNotNull(daaId);
      DataAccessAgreement daa = daaDAO.findById(daaId);
      assertNotNull(daa);
      assertNotNull(daa.getFile());
      assertNotNull(daa.getInitialDacId());
      assertFalse(daa.getDacs().isEmpty());
    });
  }

}
