package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DacServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;

  @Inject
  public DacServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public void deleteDacAndDaas(Dac dac)
      throws IllegalArgumentException, SQLException {
    // fail fast
    if (dac == null) {
      throw new IllegalArgumentException("Invalid DAC");
    }
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);

      jdbi.useTransaction(handler -> {
        final String deleteFromLcDaa = "DELETE FROM lc_daa WHERE daa_id in (SELECT daa_id FROM data_access_agreement WHERE initial_dac_id = :dacId)";
        final String deleteFromDacDaa = "DELETE FROM dac_daa WHERE dac_id = :dacId";
        final String deleteFromDaa = "DELETE FROM data_access_agreement WHERE initial_dac_id = :dacId";
        final String deleteMembers = "DELETE FROM user_role WHERE dac_id = :dacId";
        final String updateDatasets = "UPDATE dataset SET dac_id = null, needs_approval = true WHERE dac_id = :dacId";
        final String deleteDac = "DELETE FROM dac where dac_id = :dacId";

        Update lcDaaDeletion = handler.createUpdate(deleteFromLcDaa);
        lcDaaDeletion.bind("dacId", dac.getDacId());
        lcDaaDeletion.execute();

        Update dacDaaDeletion = handler.createUpdate(deleteFromDacDaa);
        dacDaaDeletion.bind("dacId", dac.getDacId());
        dacDaaDeletion.execute();

        Update daaDeletion = handler.createUpdate(deleteFromDaa);
        daaDeletion.bind("dacId", dac.getDacId());
        daaDeletion.execute();

        Update memberDeletion = handler.createUpdate(deleteMembers);
        memberDeletion.bind("dacId", dac.getDacId());
        memberDeletion.execute();

        Update datasetUpdate = handler.createUpdate(updateDatasets);
        datasetUpdate.bind("dacId", dac.getDacId());
        datasetUpdate.execute();

        Update dacDeletion = handler.createUpdate(deleteDac);
        dacDeletion.bind("dacId", dac.getDacId());
        dacDeletion.execute();
        handler.commit();
      });
    });
  }
}
