package org.broadinstitute.consent.http.db;

import java.util.Collection;
import org.broadinstitute.consent.http.models.CacheDocument;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface CacheTableDAO extends Transactional<CacheTableDAO> {

  @SqlBatch(
      """
      INSERT into cache_table
      (key, jsondocument)
        (SELECT :key, :jsondocument::jsonb)
      """)
  @BatchChunkSize(50)
  int[] insert(@BindMethods Collection<CacheDocument> documents);

  @SqlQuery(
      """
        SELECT key, jsondocument from cache_table
      """)
  @RegisterConstructorMapper(CacheDocument.class)
  ResultIterable<CacheDocument> streamDocuments();
}


// Query Scratchpad for example type things that will need to be completed:
/*
UPDATE cache_table SET search_tsvector = to_tsvector('english', jsondocument);
UPDATE cache_table SET studyid = (jsondocument->'study'->>'studyId')::int
UPDATE cache_table SET phsid = (jsondocument->'study'->>'phsId')

UPDATE cache_table SET participantcount = (jsondocument->>'participantCount')::int
UPDATE cache_table SET publicvisibility = coalesce((jsondocument->>'publicVisibility')::boolean, false)
UPDATE cache_table SET dacapproval = coalesce((jsondocument->>'dacApproval')::boolean, false)
UPDATE cache_table SET accessmanagement = (jsondocument->>'accessManagement')

DROP INDEX idx_search_vector; CREATE INDEX idx_search_vector ON cache_table USING GIN (search_tsvector);

DROP INDEX idx_jsondocument; CREATE INDEX idx_jsondocument ON cache_table USING GIN(jsondocument)

                             CREATE INDEX idx_phsId ON cache_table (phsid)

SELECT * from cache_table where search_tsvector @@ to_tsquery('english', 'mit')

SELECT jsondocument->'study'->>'studyId' as studyId, json_agg(jsondocument) as jsondocument from cache_table where jsondocument @> '{"study": {"publicVisibility": true}}' GROUP BY studyId

DELETE from cache_table where jsondocument is not null

SELECT jsondocument->'study'->>'studyId' as studyId, json_agg(jsondocument) as jsondocument from cache_table where jsondocument @> '{"study": {"publicVisibility": true}}' GROUP BY studyId

select jsondocument from cache_table where jsondocument @> '{"study":{"dataTypes":["Raw Sequencing data"
]}}'

select jsondocument->>'participantCount' as participantCount, jsondocument from cache_table where jsondocument->>'participantCount'::int = 58

select jsondocument from cache_table where jsondocument @> '{"study":{"publicVisibility": true}}' AND (jsondocument @> '{"dacApproval": true}' OR jsondocument @> '{"accessManagement": ["open"]}' OR jsondocument @> '{"accessManagement": ["external]}')



select
    jsondocument->'study'->>'studyId' as myagg, json_agg(jsondocument)
from cache_table
where
    jsondocument @> '{"study":{"publicVisibility": true}}' AND ((dacapproval = true) OR (accessmanagement IN ('open', 'external')))
GROUP BY myagg

select
    studyid as myagg, json_agg(jsondocument)
from cache_table
where
    jsondocument @> '{"study":{"publicVisibility": true}}' AND ((dacapproval = true) OR (accessmanagement IN ('open', 'external')))
GROUP BY myagg



select accessmanagement, count(accessmanagement) from cache_table group by accessmanagement

select phsid, count(phsid) from cache_table group by phsid

select publicvisibility, count(publicvisibility) from cache_table group by publicvisibility
 */

// cache table DDL
/*
create table consent.cache_table
(
    key              text                  not null,
    jsondocument     jsonb                 not null,
    id               bigint generated always as identity
        constraint cache_table_pk
            primary key,
    search_tsvector  tsvector,
    publicvisibility boolean default false not null,
    dacapproval      boolean default false not null,
    accessmanagement text,
    participantcount integer,
    studyid          integer,
    phsid            text
);

alter table consent.cache_table
    owner to consent;

create index idx_search_vector
    on consent.cache_table using gin (search_tsvector);

create index idx_jsondocument
    on consent.cache_table using gin (jsondocument);

create index idx_phsid
    on consent.cache_table (phsid);


 */