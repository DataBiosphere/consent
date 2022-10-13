package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DatasetServiceDAO {

    private final Jdbi jdbi;
    private final DatasetDAO datasetDAO;

    @Inject
    public DatasetServiceDAO(Jdbi jdbi, DatasetDAO datasetDAO) {
        this.jdbi = jdbi;
        this.datasetDAO = datasetDAO;
    }

    public List<DatasetProperty> synchronizeDatasetProperties(Integer datasetId, List<DatasetProperty> properties) throws SQLException {
        jdbi.useHandle(
                handle -> {
                    // By default, new connections are set to auto-commit which breaks our rollback strategy.
                    // Turn that off for this connection. This will not affect existing or new connections and
                    // only applies to the current one in this handle.
                    handle.getConnection().setAutoCommit(false);
                    // 1. Generate inserts for missing dictionary terms
                    // 2. Generate inserts for new dataset properties
                    // 3. Generate updates for existing dataset properties
                    // 4. Generate deletes for outdated dataset properties
                    List<Update> updates = new ArrayList<>(generateDictionaryInsertsForProperties(handle, properties));
                    // We need to know existing properties for all property operations
                    Set<DatasetProperty> existingProps = datasetDAO.findDatasetPropertiesByDatasetId(datasetId);
                    updates.addAll(generatePropertyInserts(handle, datasetId, properties, existingProps));
                    updates.addAll(generatePropertyUpdates(handle, datasetId, properties, existingProps));
                    updates.addAll(generatePropertyDeletes(handle, properties, existingProps));
                    updates.forEach(Update::execute);
                    handle.commit();
                }
        );
        return datasetDAO.findDatasetPropertiesByDatasetId(datasetId).stream().toList();
    }

    private List<Update> generateDictionaryInsertsForProperties(Handle handle, List<DatasetProperty> properties) {
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        List<String> keyValues = dictionaryTerms.stream().map(Dictionary::getKey).toList();
        List<Update> updates = new ArrayList<>();
        properties.forEach(prop -> {
            if (!keyValues.contains(prop.getPropertyName())) {
                updates.add(createDictionaryInsert(handle, prop.getPropertyName()));
            }
        });
        return updates;
    }

    private Update createDictionaryInsert(Handle handle, String key) {
        final String sql = " INSERT INTO dictionary (key, required) VALUES (:key, FALSE) ON CONFLICT DO NOTHING ";
        Update insert = handle.createUpdate(sql);
        insert.bind("key", key);
        return insert;
    }

    private List<Update> generatePropertyInserts(Handle handle, Integer datasetId, List<DatasetProperty> properties, Set<DatasetProperty> existingProps) {
        Timestamp now = new Timestamp(new Date().getTime());
        List<Update> updates = new ArrayList<>();
        List<String> existingPropNames = existingProps.stream().map(DatasetProperty::getPropertyName).toList();
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

    private List<Update> generatePropertyUpdates(Handle handle, Integer datasetId, List<DatasetProperty> properties, Set<DatasetProperty> existingProps) {
        List<Update> updates = new ArrayList<>();
        List<String> existingPropNames = existingProps.stream().map(DatasetProperty::getPropertyName).toList();
        // Generate updates for props we already know
        properties.forEach(prop -> {
            if (existingPropNames.contains(prop.getPropertyName())) {
                prop.setDataSetId(datasetId);
                updates.add(createPropertyUpdate(handle, prop));
            }
        });
        return updates;
    }

    private List<Update> generatePropertyDeletes(Handle handle, List<DatasetProperty> properties, Set<DatasetProperty> existingProps) {
        List<Update> updates = new ArrayList<>();
        List<String> newPropNames = properties.stream().map(DatasetProperty::getPropertyName).toList();
        // Generate deletes for existing props not enumerated in the new props
        existingProps.forEach(existingProp -> {
            if (!newPropNames.contains(existingProp.getPropertyName())) {
                updates.add(createPropertyDelete(handle, existingProp));
            }
        });
        return updates;
    }

    private Update createPropertyInsert(Handle handle, DatasetProperty property, Timestamp now) {
        // TODO - the key will need to be a subquery since it could be a previous insert into dictionary
        final String sql = """
            INSERT INTO dataset_property (dataset_id, property_key, schema_property, property_value, property_type, create_date ) 
            VALUES (:datasetId, :propertyKey, :schemaProperty, :propertyStringValue, :propertyTypeValue, :createDate)
        """;
        Update insert = handle.createUpdate(sql);
        insert.bind("datasetId", property.getDataSetId());
        insert.bind("propertyKey", property.getPropertyKey());
        insert.bind("schemaProperty", property.getSchemaProperty());
        insert.bind("propertyStringValue", property.getPropertyValueAsString());
        insert.bind("propertyTypeValue", property.getPropertyTypeAsString());
        insert.bind("createDate", now);
        return insert;
    }

    private Update createPropertyUpdate(Handle handle, DatasetProperty property) {
        final String sql = """
            UPDATE dataset_property
            SET property_value = :propertyStringValue
            WHERE dataset_id = :datasetId
        """;
        Update insert = handle.createUpdate(sql);
        insert.bind("datasetId", property.getDataSetId());
        insert.bind("propertyStringValue", property.getPropertyValueAsString());
        return insert;
    }

    private Update createPropertyDelete(Handle handle, DatasetProperty property) {
        final String sql = """
            DELETE FROM dataset_property
            WHERE dataset_id = :datasetId
        """;
        Update insert = handle.createUpdate(sql);
        insert.bind("datasetId", property.getDataSetId());
        return insert;
    }
}
