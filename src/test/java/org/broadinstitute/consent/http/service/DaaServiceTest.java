package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DaaServiceTest {

  @Mock
  private DaaServiceDAO daaServiceDAO;

  @Mock
  private DaaDAO daaDAO;

  private DaaService service;

  private void initService() {
    service = new DaaService(daaServiceDAO, daaDAO);
  }

  @Test
  void testCreateDaaWithFso() throws Exception {
    FileStorageObject fso = new FileStorageObject();
    DataAccessAgreement daa = new DataAccessAgreement();
    when(daaServiceDAO.createDaaWithFso(any(), any(), any())).thenReturn(1);
    when(daaDAO.findById(any())).thenReturn(daa);

    initService();
    service.createDaaWithFso(1, 1, fso);
  }

  @Test
  void testAddDacToDaa() {
    doNothing().when(daaDAO).createDacDaaRelation(any(), any());

    initService();
    service.addDacToDaa(1, 1);
  }

  @Test
  void testRemoveDacFromDaa() {
    doNothing().when(daaDAO).deleteDacDaaRelation(any(), any());

    initService();
    service.removeDacFromDaa(1, 1);
  }

}
