package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

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
