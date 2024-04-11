package org.broadinstitute.consent.http.db.liquibase.changes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import liquibase.change.custom.CustomTaskChange;
import liquibase.change.custom.CustomTaskRollback;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.RollbackImpossibleException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class MigrateDataDepositors implements CustomTaskChange, CustomTaskRollback, ConsentLogger {

  @Override
  public void execute(Database database) throws CustomChangeException {
    try {
      var dbConn = (JdbcConnection) database.getConnection();
      Map<Integer, List<String>> dataDepositorsOwnersMap = findDataDepositorsAndOwners(dbConn);
      Map<Integer, List<String>> dataCustodiansMap = findDataCustodians(dbConn);
    } catch (Exception e) {
      throw new CustomChangeException(e.getMessage(), e);
    }
  }

  private Map<Integer, List<String>> findDataDepositorsAndOwners(JdbcConnection dbConn)
      throws CustomChangeException {
    Map<Integer, List<String>> dsCustodianMap = new HashMap<>();
    try {
      // find datasets with studies that have data depositors union-ed with older data owners
      var query = """
            -- Data Depositors
            SELECT DISTINCT ds.dataset_id, dp.property_value
            FROM dataset ds
            INNER JOIN dataset_property dp ON ds.dataset_id = dp.dataset_id
            INNER JOIN dictionary d ON dp.property_key = d.key_id AND key = 'Data Depositor'
            WHERE ds.study_id IS NOT NULL
            UNION DISTINCT
            -- Data Owners
            SELECT DISTINCT dua.datasetid AS dataset_id, u.email AS property_value
            FROM dataset_user_association dua
            INNER JOIN users u ON dua.dacuserid = u.user_id
            INNER JOIN dataset d ON dua.datasetid = d.dataset_id AND d.study_id IS NOT NULL;
          """;
      var selectStatement = dbConn.createStatement();
      var rs = selectStatement.executeQuery(query);
      while (rs.next()) {
        Integer dsId = rs.getInt("dataset_id");
        String value = rs.getString("property_value");
        if (!StringUtils.isBlank(value)) {
          dsCustodianMap.computeIfAbsent(dsId, k -> new ArrayList<>()).add(value);
        }
      }
    } catch (Exception e) {
      throw new CustomChangeException(e.getMessage(), e);
    }
    return dsCustodianMap;
  }

  private Map<Integer, List<String>> findDataCustodians(JdbcConnection dbConn)
      throws CustomChangeException {
    Map<Integer, List<String>> dataOwnerMap = new HashMap<>();
    Gson gson = new Gson();
    Type listOfStringObject = new TypeToken<ArrayList<String>>() {}.getType();
    try {
      // find datasets with studies that have data custodian property array values
      var query = """
            SELECT DISTINCT ds.dataset_id, dp.property_value
            FROM dataset ds
            INNER JOIN dataset_property dp ON ds.dataset_id = dp.dataset_id
            INNER JOIN dictionary d ON dp.property_key = d.key_id AND key = 'Data Custodian Email'
            WHERE ds.study_id IS NOT NULL
          """;
      var selectStatement = dbConn.createStatement();
      var rs = selectStatement.executeQuery(query);
      while (rs.next()) {
        Integer dsId = rs.getInt("dataset_id");
        String value = rs.getString("property_value");
        try {
          List<String> values = gson.fromJson(value, listOfStringObject);
          if (!values.isEmpty()) {
            dataOwnerMap.computeIfAbsent(dsId, k -> new ArrayList<>()).addAll(values);
          }
        } catch (Exception e) {
          logException(String.format("Exception deserializing data custodian emails from dataset id: %s", dsId), e);
        }
      }
    } catch (Exception e) {
      throw new CustomChangeException(e.getMessage(), e);
    }
    return dataOwnerMap;
  }

  @Override
  public String getConfirmationMessage() {
    return "";
  }

  @Override
  public void setUp() throws SetupException {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {

  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  @Override
  public void rollback(Database database)
      throws CustomChangeException, RollbackImpossibleException {

  }
}
