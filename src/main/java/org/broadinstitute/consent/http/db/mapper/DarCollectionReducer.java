package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

public class DarCollectionReducer implements LinkedHashMapRowReducer<Integer, DarCollection> {

    @Override
    public void accumulate(Map<Integer, DarCollection> map, RowView rowView) {
      DataAccessRequest dar = null;
      DarCollection collection = map.computeIfAbsent(
        rowView.getColumn("collection_id", Integer.class),
        id -> rowView.getRow(DarCollection.class));

      try{
        if(Objects.nonNull(collection) && Objects.nonNull(rowView.getColumn("id", Integer.class))) {
          dar = rowView.getRow(DataAccessRequest.class);

//          String darDataString = resultSet.getObject("data", PGobject.class).getValue();
//          if (Objects.nonNull(darDataString)) {
//            // Handle nested quotes
//            String quoteFixedDataString = darDataString.replaceAll("\\\\\"", "'");
//            // Inserted json data ends up double-escaped via standard jdbi insert.
//            String escapedDataString = unescapeJava(quoteFixedDataString);
//            try {
//              DataAccessRequestData data = DataAccessRequestData.fromString(escapedDataString);
//              data.setReferenceId(dar.getReferenceId());
//              dar.setData(data);
//            } catch (JsonSyntaxException | NullPointerException e) {
//              String message = "Unable to parse Data Access Request, reference id: " + dar.getReferenceId() + "; error: " + e.getMessage();
//              logger.error(message);
//              throw new SQLException(message);
//            }
//          }
          //aliased fields must be set directly
          dar.setCollectionId(rowView.getColumn("dar_collection_id", Integer.class));
          dar.setCreateDate(rowView.getColumn("dar_create_date", Timestamp.class));
          dar.setUpdateDate(rowView.getColumn("dar_update_date", Timestamp.class));
        }
      } catch(MappingException e) {
        //ignore any exceptions
      }

      if(Objects.nonNull(dar)) {
        collection.addDar(dar);
      }
    }

}
