CREATE TABLE if not exists ACT_GE_PROPERTY  (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

CREATE TABLE if not exists ACT_GE_BYTEARRAY  (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ bytea,
    GENERATED_ boolean,
    primary key (ID_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '7.0.1.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

-- force-commit