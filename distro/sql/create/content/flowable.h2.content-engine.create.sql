
CREATE TABLE PUBLIC.ACT_CO_DATABASECHANGELOG (ID VARCHAR(255) NOT NULL, AUTHOR VARCHAR(255) NOT NULL, FILENAME VARCHAR(255) NOT NULL, DATEEXECUTED TIMESTAMP NOT NULL, ORDEREXECUTED INT NOT NULL, EXECTYPE VARCHAR(10) NOT NULL, MD5SUM VARCHAR(35), DESCRIPTION VARCHAR(255), COMMENTS VARCHAR(255), TAG VARCHAR(255), LIQUIBASE VARCHAR(20), CONTEXTS VARCHAR(255), LABELS VARCHAR(255), DEPLOYMENT_ID VARCHAR(10));

CREATE TABLE PUBLIC.ACT_CO_CONTENT_ITEM (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NOT NULL, MIME_TYPE_ VARCHAR(255), TASK_ID_ VARCHAR(255), PROC_INST_ID_ VARCHAR(255), CONTENT_STORE_ID_ VARCHAR(255), CONTENT_STORE_NAME_ VARCHAR(255), FIELD_ VARCHAR(400), CONTENT_AVAILABLE_ BOOLEAN DEFAULT FALSE, CREATED_ TIMESTAMP, CREATED_BY_ VARCHAR(255), LAST_MODIFIED_ TIMESTAMP, LAST_MODIFIED_BY_ VARCHAR(255), CONTENT_SIZE_ BIGINT DEFAULT 0, TENANT_ID_ VARCHAR(255), CONSTRAINT PK_ACT_CO_CONTENT_ITEM PRIMARY KEY (ID_));

CREATE INDEX PUBLIC.idx_contitem_taskid ON PUBLIC.ACT_CO_CONTENT_ITEM(TASK_ID_);

CREATE INDEX PUBLIC.idx_contitem_procid ON PUBLIC.ACT_CO_CONTENT_ITEM(PROC_INST_ID_);

INSERT INTO PUBLIC.ACT_CO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'activiti', 'org/flowable/content/db/liquibase/flowable-content-db-changelog.xml', NOW(), 1, '7:a17df43ed0c96adfef5271e1781aaed2', 'createTable tableName=ACT_CO_CONTENT_ITEM; createIndex indexName=idx_contitem_taskid, tableName=ACT_CO_CONTENT_ITEM; createIndex indexName=idx_contitem_procid, tableName=ACT_CO_CONTENT_ITEM', '', 'EXECUTED', NULL, NULL, '3.5.3', '7878883935');

