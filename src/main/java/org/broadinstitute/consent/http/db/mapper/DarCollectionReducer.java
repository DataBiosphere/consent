package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

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
        if(Objects.nonNull(collection) && Objects.nonNull(rowView.getColumn("dar_id", Integer.class))) {
          dar = rowView.getRow(DataAccessRequest.class);
          DataAccessRequestData data = RowMapperHelper.translate(rowView.getColumn("data", String.class));
          dar.setData(data);
        }
      } catch(MappingException e) {
        //ignore any exceptions
      }

      if(Objects.nonNull(dar)) {
        collection.addDar(dar);
      }
    }

}
