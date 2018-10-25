UPDATE dictionary SET key = 'Sample Collection ID' where key = 'Dataset ID';
insert into consents(consentId, requiresManualReview,useRestriction,active,name,createDate,lastUpdate,sortDate,dataUse,dataUseLetter,dulName) values ('testId',true,'{"type":"everything"}',true,'testName',now(),now(),now(),'','dataUseLetter-link-01.here','dulName-01.pdf');
insert into consents(consentId, requiresManualReview,useRestriction,active,name,createDate,lastUpdate,sortDate,dataUse,dataUseLetter,dulName) values ('testId2',true,'{"type":"everything"}',true,'testName1',now(),now(),now(),'','dataUseLetter-link-02.here','dulName-02.pdf');
insert into consents(consentId, requiresManualReview,useRestriction,active,name,createDate,lastUpdate,sortDate,dataUse,dataUseLetter,dulName) values ('testId3',true,'{"type":"everything"}',true,'testName3',now(),now(),now(),'','dataUseLetter-link-03.here','dulName-03.pdf');
insert into consents (consentId, requiresManualReview, useRestriction, active, name, createDate, sortDate, lastUpdate,dataUse,dataUseLetter,dulName) values ('testId4', 0, '{"type":"everything"}', 1, 'test name 4', '2015-11-06 14:39:48', '2015-11-06 14:39:48', '2015-11-06 14:39:48','','dataUseLetter-link-04.here','dulName-04.pdf');
insert into dataset (dataSetId,name,createDate, objectId, active) values(1,'test','2015-08-05 13:58:50','SC-20660', true);
insert into dataset (dataSetId,name,createDate) values(2,'test2','2015-08-05 13:58:50');
insert into dataset (dataSetId,name,createDate, objectId, active) values(3,'test3','2015-08-05 13:58:50','SC-20659', true);
insert into dataset (dataSetId,name,createDate, objectId, active) values(4,'test4','2015-08-05 13:58:50','SC-20658', true);
insert into dataset (dataSetId,name,createDate, objectId, active) values(5,'test5','2015-08-05 13:58:50','SC-20657', true);
insert into dataset (dataSetId,name,createDate, objectId, active) values(6,'test6','2015-08-05 13:58:50','1', true);
insert into consentassociations(associationId, consentId,associationType,dataSetId) values (100,'testId','associationType',6);
insert into consentassociations(associationId, consentId, associationType, dataSetId) values (101, 'testId1', 'associationType', 5);
insert into consentassociations(associationId, consentId, associationType, dataSetId) values (102, 'testId2', 'associationType', 4);
insert into consentassociations(associationId, consentId, associationType, dataSetId) values (103, 'testId3', 'associationType', 3);
insert into consentassociations(associationId, consentId, associationType, dataSetId) values (104, 'testId4', 'associationType', 1);
insert into dacuser(dacUserId,email,displayName,createDate) values(1,'test@broad.com','testUser','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(2,'test2@broad.com','testUser1','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(3,'test3@broad.com','testUser2','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(4,'test4@broad.com','testUser3','2015-08-05 13:58:50');
insert into dacuser(dacUserId,email,displayName,createDate) values(5,'oauthuser@broadinstitute.org','oauth','2015-08-05 13:58:50');
insert into election(electionId, electionType, status, createDate, referenceId) values (130, '2', 'Closed', '2015-11-06 17:52:18', 'testId4');
INSERT INTO vote(voteId, vote, dacUserId, createDate, electionId, rationale, reminderSent, type) VALUES (2000, 1, 1, '2015-08-05 13:58:50', 130, 'Empty', 0, 'CHAIRPERSON');
insert into researchpurpose (purposeId,purpose) values(1,'General Use');
insert into user_role(roleId, dacUserId) values(2,1);
insert into user_role(roleId, dacUserId, status) values(5,1,0);
insert into user_role(roleId, dacUserId) values(1,2);
insert into user_role(roleId, dacUserId) values(1,3);
insert into user_role(roleId, dacUserId) values(1,4);
insert into user_role(roleId, dacUserId) values(4,4);
insert into user_role(roleId, dacUserId) values(6,1);
insert into user_role(roleId, dacUserId) values(6,2);
insert into user_role(roleId, dacUserId) values(6,5);
insert into user_role(roleId, dacUserId) values(5,5);
insert into user_role(roleId, dacUserId) values(4,5);
insert into user_role(roleId, dacUserId) values(2,5);

