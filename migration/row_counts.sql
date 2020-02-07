/**
                       consent.access_rp          0        124     1.4 kB          0.837s
consent.accesselection_consentelection          0        177     1.3 kB          0.836s
      consent.approval_expiration_time          0          0                     1.338s
           consent.consentassociations          0         58     3.0 kB          1.333s
                      consent.consents          0         57    66.0 kB          1.949s
                           consent.dac          0          5     0.3 kB          1.838s
             consent.databasechangelog          0        138    21.2 kB          2.339s
                       consent.dacuser          0         81     6.3 kB          2.344s
                   consent.datarequest          0          0                     2.831s
         consent.databasechangeloglock          0          1     0.0 kB          2.869s
               consent.datasetproperty          0        441    21.9 kB          3.321s
                       consent.dataset          0         59     3.9 kB          3.362s
                 consent.dataset_audit          0        229    18.3 kB          3.846s
        consent.dataset_audit_property          0       1899   100.1 kB          3.194s
      consent.dataset_user_association          0          2     0.1 kB          3.621s
                    consent.dictionary          0         11     0.2 kB          3.487s
                      consent.election          0        383   332.4 kB          4.362s
                  consent.email_entity          0        616     2.2 MB          4.336s
                    consent.email_type          0          4     0.1 kB          3.738s
                  consent.match_entity          0        148    12.9 kB          4.278s
               consent.researchpurpose          0          0                     4.184s
                     consent.user_role          0        107     2.1 kB          4.475s
                   consent.help_report          0         13     0.6 kB          2.726s
                          consent.vote          0       2392   148.3 kB          4.671s
           consent.researcher_property          0        220     7.3 kB          3.268s
                         consent.roles          0          6     0.1 kB          3.338s
                 consent.user_role_bak          0         84     1.1 kB          3.528s
               consent.workspace_audit          0        170    13.3 kB          3.718s
 */
SELECT 'select count(*) from access_rp' AS ' ';
select count(*) from access_rp;
SELECT 'select count(*) from accesselection_consentelection' AS ' ';
select count(*) from accesselection_consentelection;
SELECT 'select count(*) from approval_expiration_time' AS ' ';
select count(*) from approval_expiration_time;
SELECT 'select count(*) from consentassociations' AS ' ';
select count(*) from consentassociations;
SELECT 'select count(*) from consents' AS ' ';
select count(*) from consents;
SELECT 'select count(*) from dac' AS ' ';
select count(*) from dac;
SELECT 'select count(*) from DATABASECHANGELOG' AS ' ';
select count(*) from DATABASECHANGELOG;
SELECT 'select count(*) from dacuser' AS ' ';
select count(*) from dacuser;
SELECT 'select count(*) from datarequest' AS ' ';
select count(*) from datarequest;
SELECT 'select count(*) from DATABASECHANGELOGLOCK' AS ' ';
select count(*) from DATABASECHANGELOGLOCK;
SELECT 'select count(*) from datasetproperty' AS ' ';
select count(*) from datasetproperty;
SELECT 'select count(*) from dataset' AS ' ';
select count(*) from dataset;
SELECT 'select count(*) from dataset_audit' AS ' ';
select count(*) from dataset_audit;
SELECT 'select count(*) from dataset_audit_property' AS ' ';
select count(*) from dataset_audit_property;
SELECT 'select count(*) from dataset_user_association' AS ' ';
select count(*) from dataset_user_association;
SELECT 'select count(*) from dictionary' AS ' ';
select count(*) from dictionary;
SELECT 'select count(*) from election' AS ' ';
select count(*) from election;
SELECT 'select count(*) from email_entity' AS ' ';
select count(*) from email_entity;
SELECT 'select count(*) from email_type' AS ' ';
select count(*) from email_type;
SELECT 'select count(*) from match_entity' AS ' ';
select count(*) from match_entity;
SELECT 'select count(*) from researchpurpose' AS ' ';
select count(*) from researchpurpose;
SELECT 'select count(*) from user_role' AS ' ';
select count(*) from user_role;
SELECT 'select count(*) from help_report' AS ' ';
select count(*) from help_report;
SELECT 'select count(*) from vote' AS ' ';
select count(*) from vote;
SELECT 'select count(*) from researcher_property' AS ' ';
select count(*) from researcher_property;
SELECT 'select count(*) from roles' AS ' ';
select count(*) from roles;
SELECT 'select count(*) from user_role_bak' AS ' ';
select count(*) from user_role_bak;
SELECT 'select count(*) from workspace_audit' AS ' ';
select count(*) from workspace_audit;
