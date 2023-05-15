package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DatasetServiceDAO {

  private final Jdbi jdbi;
  private final DatasetDAO datasetDAO;

  @Inject
  public DatasetServiceDAO(Jdbi jdbi, DatasetDAO datasetDAO) {
    this.jdbi = jdbi;
    this.datasetDAO = datasetDAO;
  }

  public record StudyInsert(String name,
                            String description,
                            List<String> dataTypes,
                            String piName,
                            Boolean publicVisibility,
                            Integer userId,
                            List<StudyProperty> props,
                            List<FileStorageObject> files) {

  }

  public record DatasetInsert(String name,
                              Integer dacId,
                              DataUse dataUse,
                              Integer userId,
                              List<DatasetProperty> props,
                              List<FileStorageObject> files) {

  }

  public record DatasetUpdate(Integer datasetId,
                              String name,
                              Integer dacId,
                              DataUse dataUse,
                              Integer userId,
                              List<DatasetProperty> props,
                              List<FileStorageObject> files) {
  }

  /**
   * Inserts a set of datasets, optionally under a study.
   *
   * @param study    If provided, creates a study and links all datasets to it; if null, no study is
   *                 created.
   * @param datasets The datasets to create.
   * @return The IDs of the datasets created.
   * @throws SQLException if DB transaction fails.
   */
  public List<Integer> insertDatasetRegistration(StudyInsert study, List<DatasetInsert> datasets)
      throws SQLException {
    final List<Integer> createdDatasets = new ArrayList<>();

    jdbi.useHandle(
        handle -> {
          // By default, new connections are set to auto-commit which breaks our rollback strategy.
          // Turn that off for this connection. This will not affect existing or new connections and
          // only applies to the current one in this handle.
          handle.getConnection().setAutoCommit(false);

          Integer studyId = null;
          if (Objects.nonNull(study)) {
            studyId = executeInsertStudy(handle, study);
          }

          for (DatasetInsert insert : datasets) {
            Integer datasetId = executeInsertDatasetWithFiles(
                handle,
                insert.name(),
                insert.dacId(),
                studyId,
                insert.dataUse(),
                insert.userId(),
                insert.props(),
                insert.files());

            createdDatasets.add(datasetId);
          }

          handle.commit();
        }
    );
    return createdDatasets;
  }

  public Integer executeInsertDatasetWithFiles(Handle handle,
      String name,
      Integer dacId,
      Integer studyId,
      DataUse dataUse,
      Integer userId,
      List<DatasetProperty> properties,
      List<FileStorageObject> uploadedFiles) {
    // insert dataset
    Integer datasetId = datasetDAO.insertDataset(
        name,
        new Timestamp(new Date().getTime()),
        userId,
        null,
        false,
        dataUse.toString(),
        dacId
    );

    if (Objects.nonNull(studyId)) {
      datasetDAO.updateStudyId(datasetId, studyId);
    }

    // insert properties
    executeSynchronizeDatasetProperties(handle, datasetId, properties);

    // files
    executeInsertFiles(handle, uploadedFiles, userId, datasetId.toString());

    return datasetId;
  }

  private Integer executeInsertStudy(Handle handle, StudyInsert insert) {
    StudyDAO studyDAO = handle.attach(StudyDAO.class);
    UUID uuid = UUID.randomUUID();
    Integer studyId = studyDAO.insertStudy(
        insert.name,
        insert.description,
        insert.piName,
        insert.dataTypes,
        insert.publicVisibility,
        insert.userId,
        Instant.now(),
        uuid
    );

    for (StudyProperty prop : insert.props) {
      studyDAO.insertStudyProperty(
          studyId,
          prop.getKey(),
          prop.getType().toString(),
          prop.getValue().toString()
      );
    }

    executeInsertFiles(
        handle,
        insert.files,
        insert.userId,
        uuid.toString());

    return studyId;
  }

  private void executeInsertFiles(Handle handle, List<FileStorageObject> files, Integer userId,
      String entityId) {
    FileStorageObjectDAO fileStorageObjectDAO = handle.attach(FileStorageObjectDAO.class);
    for (FileStorageObject file : files) {
      fileStorageObjectDAO.insertNewFile(
          file.getFileName(),
          file.getCategory().getValue(),
          file.getBlobId().toGsUtilUri(),
          file.getMediaType(),
          entityId,
          userId,
          Instant.now()
      );
    }
  }

  public List<Integer> updateDataset(List<DatasetUpdate> updates) throws SQLException {
    final List<Integer> updatedDataset = null;

    jdbi.useHandle(
        handle -> {
          // By default, new connections are set to auto-commit which breaks our rollback strategy.
          // Turn that off for this connection. This will not affect existing or new connections and
          // only applies to the current one in this handle.
          handle.getConnection().setAutoCommit(false);

          for (DatasetUpdate update : updates) {
            Integer datasetUpdatesNew = executeUpdateDatasetWithFiles(
                handle,
                update.datasetId(),
                update.name(),
                update.dacId(),
                update.userId(),
                update.props(),
                update.files());

            updatedDataset.add(datasetUpdatesNew);
          }

          handle.commit();
        }
    );
    return updatedDataset;
  }

  public Integer executeUpdateDatasetWithFiles(Handle handle,
      Integer datasetId,
      String name,
      Integer dacId,
      Integer userId,
      List<DatasetProperty> properties,
      List<FileStorageObject> uploadedFiles) {
    // update dataset
    Integer datasetUpdate = datasetDAO.updateDatasetNew(
        datasetId,
        name,
        new Timestamp(new Date().getTime()),
        userId,
        false,
        dacId
    );

    // insert properties
    executeSynchronizeDatasetProperties(handle, datasetUpdate, properties);

    // files
    executeInsertFiles(handle, uploadedFiles, userId, datasetUpdate.toString());

    return datasetUpdate;
  }

  // Helper methods to generate Dictionary inserts
  private void executeSynchronizeDatasetProperties(Handle handle, Integer datasetId,
      List<DatasetProperty> properties) {
    List<Update> updates = new ArrayList<>(generateDictionaryInserts(handle, properties));
    // We need to know existing properties for all property operations
    Set<DatasetProperty> existingProps = datasetDAO.findDatasetPropertiesByDatasetId(datasetId);

    // 1. Generate inserts for missing dictionary terms
    // 2. Generate inserts for new dataset properties
    updates.addAll(generatePropertyInserts(handle, datasetId, properties, existingProps));

    // 3. Generate updates for existing dataset properties
    updates.addAll(generatePropertyUpdates(handle, datasetId, properties, existingProps));

    // 4. Generate deletes for outdated dataset properties
    updates.addAll(generatePropertyDeletes(handle, properties, existingProps));
    updates.forEach(Update::execute);
  }

  private List<Update> generateDictionaryInserts(Handle handle, List<DatasetProperty> properties) {
    List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
    HashSet<String> keyValues = new HashSet<>(
        dictionaryTerms.stream().map(Dictionary::getKey).toList());
    List<Update> updates = new ArrayList<>();
    properties.forEach(prop -> {
      if (!keyValues.contains(prop.getPropertyName())) {
        updates.add(createDictionaryInsert(handle, prop.getPropertyName()));
      }
    });
    return updates;
  }

  private Update createDictionaryInsert(Handle handle, String key) {
    final String sql = """
            INSERT INTO dictionary (key, required)
            VALUES (:key, FALSE)
            ON CONFLICT DO NOTHING
        """;
    Update insert = handle.createUpdate(sql);
    insert.bind("key", key);
    return insert;
  }

  // Helper methods to generate DatasetProperty inserts

  private List<Update> generatePropertyInserts(Handle handle, Integer datasetId,
      List<DatasetProperty> properties, Set<DatasetProperty> existingProps) {
    Timestamp now = new Timestamp(new Date().getTime());
    List<Update> updates = new ArrayList<>();
    HashSet<String> existingPropNames = new HashSet<>(
        existingProps.stream().map(DatasetProperty::getPropertyName).toList());
    // Generate new inserts for props we don't know about yet
    properties.forEach(prop -> {
      if (!existingPropNames.contains(prop.getPropertyName())) {
        prop.setDataSetId(datasetId);
        prop.setCreateDate(now);
        updates.add(createPropertyInsert(handle, prop, now));
      }
    });
    return updates;
  }

  private Update createPropertyInsert(Handle handle, DatasetProperty property, Timestamp now) {
    final String sql = """
            INSERT INTO dataset_property (dataset_id, property_key, schema_property, property_value, property_type, create_date )
            SELECT :datasetId,
                    (SELECT DISTINCT key_id FROM dictionary WHERE LOWER(key) = LOWER(:propertyName) ORDER BY key_id LIMIT 1),
                    :schemaProperty, :propertyStringValue, :propertyTypeValue, :createDate
        """;
    Update insert = handle.createUpdate(sql);
    insert.bind("datasetId", property.getDataSetId());
    insert.bind("propertyKey", property.getPropertyKey());
    insert.bind("propertyName", property.getPropertyName());
    insert.bind("schemaProperty", property.getSchemaProperty());
    insert.bind("propertyStringValue", property.getPropertyValueAsString());
    insert.bind("propertyTypeValue", property.getPropertyTypeAsString());
    insert.bind("createDate", now);
    return insert;
  }

  // Helper methods to generate DatasetProperty updates

  private List<Update> generatePropertyUpdates(Handle handle, Integer datasetId,
      List<DatasetProperty> properties, Set<DatasetProperty> existingProps) {
    List<Update> updates = new ArrayList<>();
    HashSet<String> existingPropNames = new HashSet<>(
        existingProps.stream().map(DatasetProperty::getPropertyName).toList());
    // Generate value updates for props that exist
    properties.forEach(prop -> {
      if (existingPropNames.contains(prop.getPropertyName())) {
        prop.setDataSetId(datasetId);
        updates.add(createPropertyUpdate(handle, prop));
      }
    });
    return updates;
  }

  private Update createPropertyUpdate(Handle handle, DatasetProperty property) {
    final String sql = """
            UPDATE dataset_property
            SET property_value = :propertyStringValue
            WHERE dataset_id = :datasetId
            AND property_key = :propertyKey
            AND property_id = :propertyId
        """;
    Update insert = handle.createUpdate(sql);
    insert.bind("datasetId", property.getDataSetId());
    insert.bind("propertyStringValue", property.getPropertyValueAsString());
    insert.bind("propertyKey", property.getPropertyKey());
    insert.bind("propertyId", property.getPropertyId());
    return insert;
  }

  // Helper methods to generate DatasetProperty deletes

  private List<Update> generatePropertyDeletes(Handle handle, List<DatasetProperty> properties,
      Set<DatasetProperty> existingProps) {
    List<Update> updates = new ArrayList<>();
    HashSet<String> newPropNames = new HashSet<>(
        properties.stream().map(DatasetProperty::getPropertyName).toList());
    // Generate deletes for existing props that do not exist in the new props
    existingProps.forEach(existingProp -> {
      if (!newPropNames.contains(existingProp.getPropertyName())) {
        updates.add(createPropertyDelete(handle, existingProp));
      }
    });
    return updates;
  }

  private Update createPropertyDelete(Handle handle, DatasetProperty property) {
    final String sql = """
            DELETE FROM dataset_property
            WHERE dataset_id = :datasetId
            AND property_key = :propertyKey
            AND property_id = :propertyId
        """;
    Update insert = handle.createUpdate(sql);
    insert.bind("datasetId", property.getDataSetId());
    insert.bind("propertyKey", property.getPropertyKey());
    insert.bind("propertyId", property.getPropertyId());
    return insert;
  }
}
