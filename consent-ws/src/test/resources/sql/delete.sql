DELETE FROM dataset_audit_property;
DELETE FROM dataset_audit;
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20657'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20658'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20659'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20659'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20659'));
DELETE FROM vote WHERE voteId IN (2000);
DELETE FROM election WHERE electionId IN (130);
DELETE FROM consents WHERE consentId IN ('testId4');
DELETE FROM dataset_user_association WHERE datasetId IN (1, 2);
DELETE FROM dataset WHERE datasetId IN (1, 2);
DELETE FROM dataset WHERE objectId IN ('SC-20657', 'SC-20659', 'SC-20658', 'SC-20660');
DELETE FROM approval_expiration_time;
DELETE FROM consentassociations;
DELETE FROM workspace_audit;
DELETE FROM vote WHERE dacUserId = 5;
DELETE FROM user_role WHERE dacUserId = 5;
UPDATE consents SET updated = null;
