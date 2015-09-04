insert into consents(consentId, requiresManualReview,useRestriction,active,name,createDate,lastUpdate,sortDate) values ('testId',true,'{"type":"everything"}',true,'testName',now(),now(),now());
insert into consents(consentId, requiresManualReview,useRestriction,active,name,createDate,lastUpdate,sortDate) values ('testId2',true,'{"type":"everything"}',true,'testName1',now(),now(),now());
insert into consentassociations(associationId, consentId,associationType,objectId) values (100,'testId','associationType','1');
insert into consentassociations(associationId, consentId, associationType, objectId) values (101, 'testId', 'associationType', 'SC-20657')
insert into consentassociations(associationId, consentId, associationType, objectId) values (102, 'testId2', 'associationType', 'SC-20658')
insert into consentassociations(associationId, consentId, associationType, objectId) values (103, 'testId3', 'associationType', 'SC-20659')
insert into researchpurpose (purposeId,purpose) values(1,'General Use');
insert into dataset (dataSetId,name,createDate) values(1,'test','2015-08-05 13:58:50');
insert into dataset (dataSetId,name,createDate) values(2,'test','2015-08-05 13:58:50');
insert into datarequest (requestId, purposeId, dataSetId,description,researcher) values (1,'1','1','test','researcherTest');
insert into datarequest (requestId, purposeId, dataSetId,description,researcher) values (2,'1','1','test','researcherTest');
insert into dacuser(dacUserId,email,displayName,createDate) values(1,'test@broad.com','testUser','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(2,'test2@broad.com','testUser','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(3,'test3@broad.com','testUser','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(4,'test4@broad.com','testUser','2015-08-05 13:58:50');
insert into user_role(roleId, dacUserId) values(2,1);
insert into user_role(roleId, dacUserId) values(1,2);
insert into user_role(roleId, dacUserId) values(1,3);
insert into user_role(roleId, dacUserId) values(1,4);

