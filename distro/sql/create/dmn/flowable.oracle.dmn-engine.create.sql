
SET DEFINE OFF;

CREATE TABLE ACT_DMN_DATABASECHANGELOG (ID VARCHAR2(255) NOT NULL, AUTHOR VARCHAR2(255) NOT NULL, FILENAME VARCHAR2(255) NOT NULL, DATEEXECUTED TIMESTAMP NOT NULL, ORDEREXECUTED INTEGER NOT NULL, EXECTYPE VARCHAR2(10) NOT NULL, MD5SUM VARCHAR2(35), DESCRIPTION VARCHAR2(255), COMMENTS VARCHAR2(255), TAG VARCHAR2(255), LIQUIBASE VARCHAR2(20), CONTEXTS VARCHAR2(255), LABELS VARCHAR2(255), DEPLOYMENT_ID VARCHAR2(10));

CREATE TABLE ACT_DMN_DEPLOYMENT (ID_ VARCHAR2(255) NOT NULL, NAME_ VARCHAR2(255), CATEGORY_ VARCHAR2(255), DEPLOY_TIME_ TIMESTAMP, TENANT_ID_ VARCHAR2(255), PARENT_DEPLOYMENT_ID_ VARCHAR2(255), CONSTRAINT PK_ACT_DMN_DEPLOYMENT PRIMARY KEY (ID_));

CREATE TABLE ACT_DMN_DEPLOYMENT_RESOURCE (ID_ VARCHAR2(255) NOT NULL, NAME_ VARCHAR2(255), DEPLOYMENT_ID_ VARCHAR2(255), RESOURCE_BYTES_ BLOB, CONSTRAINT PK_ACT_DMN_DEPLOYMENT_RESOURCE PRIMARY KEY (ID_));

CREATE TABLE ACT_DMN_DECISION_TABLE (ID_ VARCHAR2(255) NOT NULL, NAME_ VARCHAR2(255), VERSION_ INTEGER, KEY_ VARCHAR2(255), CATEGORY_ VARCHAR2(255), DEPLOYMENT_ID_ VARCHAR2(255), PARENT_DEPLOYMENT_ID_ VARCHAR2(255), TENANT_ID_ VARCHAR2(255), RESOURCE_NAME_ VARCHAR2(255), DESCRIPTION_ VARCHAR2(255), CONSTRAINT PK_ACT_DMN_DECISION_TABLE PRIMARY KEY (ID_));

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'activiti', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', SYSTIMESTAMP, 1, '7:d878c2672ead57b5801578fd39c423af', 'createTable tableName=ACT_DMN_DEPLOYMENT; createTable tableName=ACT_DMN_DEPLOYMENT_RESOURCE; createTable tableName=ACT_DMN_DECISION_TABLE', '', 'EXECUTED', NULL, NULL, '3.5.3', '7878885550');

CREATE TABLE ACT_DMN_HI_DECISION_EXECUTION (ID_ VARCHAR2(255) NOT NULL, DECISION_DEFINITION_ID_ VARCHAR2(255), DEPLOYMENT_ID_ VARCHAR2(255), START_TIME_ TIMESTAMP, END_TIME_ TIMESTAMP, INSTANCE_ID_ VARCHAR2(255), EXECUTION_ID_ VARCHAR2(255), ACTIVITY_ID_ VARCHAR2(255), FAILED_ NUMBER(1) DEFAULT 0, TENANT_ID_ VARCHAR2(255), EXECUTION_JSON_ CLOB, CONSTRAINT PK_ACT_DMN_HI_DECISION_EXECUTI PRIMARY KEY (ID_));

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', SYSTIMESTAMP, 3, '7:15a6bda1fce898a58e04fe6ac2d89f54', 'createTable tableName=ACT_DMN_HI_DECISION_EXECUTION', '', 'EXECUTED', NULL, NULL, '3.5.3', '7878885550');

