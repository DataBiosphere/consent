DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20657'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20658'));
DELETE FROM datasetproperty WHERE dataSetId IN ( Select dataSetId FROM dataset WHERE objectId IN('SC-20659'));
DELETE FROM dataset WHERE objectId IN ('SC-20657', 'SC-20659', 'SC-20658');

