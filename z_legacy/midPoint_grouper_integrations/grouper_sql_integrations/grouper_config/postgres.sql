--Example PostgresSQL to create "intermediate" database between midPoint and Grouper
-- That database can be used for Grouper's subject source allowing midPoint
--  to provision subjects to Grouper. Then it can also be used for Grouper
--  to send back Group membership information to midPoint.
--
-- If you're just interested in the midPoint-Grouper integration
--  then skip to the "midPoint-Grouper SQL Integration" section below.
--

CREATE USER intermediate WITH PASSWORD 'intermediate' CREATEDB;
ALTER user intermediate WITH superuser;
CREATE DATABASE intermediate;

\c intermediate intermediate

--Used for midPoint to provision Grouper Subject Source
CREATE TABLE PERSON (
    ID VARCHAR(255) NOT NULL PRIMARY KEY,
    FIRST_NAME VARCHAR(255),
    LAST_NAME VARCHAR(255),
    DISPLAY_NAME VARCHAR(510),
    EMAIL_ADDRESS VARCHAR(255),
    DESCRIPTION VARCHAR(1024),
    LOGIN_ID VARCHAR(255),
    CREATED TIMESTAMP NOT NULL DEFAULT NOW(),
    UPDATED TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION trigger_set_timestamp() RETURNS TRIGGER AS $$
BEGIN
    NEW.UPDATED = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_person
    BEFORE UPDATE ON PERSON
    FOR EACH ROW EXECUTE PROCEDURE trigger_set_timestamp();

CREATE VIEW PERSON_V AS SELECT ID, FIRST_NAME, LAST_NAME, DISPLAY_NAME, EMAIL_ADDRESS, DESCRIPTION, LOWER(DESCRIPTION) AS DESCRIPTION_LOWER, LOGIN_ID, CREATED, UPDATED FROM PERSON;
--End Subject Source

--Used for midPoint-Grouper SQL Integration
CREATE TABLE MIDPOINT_GROUPS (
     GROUP_NAME VARCHAR(2048) NOT NULL,
     SUBJECT_ID VARCHAR(2048) NOT NULL,
     CREATED TIMESTAMP NOT NULL DEFAULT NOW(),
     UPDATED TIMESTAMP NOT NULL DEFAULT NOW(),
     CONSTRAINT MIDPOINT_GROUPS_PK PRIMARY KEY (GROUP_NAME, SUBJECT_ID)
);

CREATE TRIGGER set_timestamp_midpoint_groups
    BEFORE UPDATE ON MIDPOINT_GROUPS
    FOR EACH ROW EXECUTE PROCEDURE trigger_set_timestamp();
--End base Grouper-midPoint integration table setup

--If using option 3 (the database table multiaccount) then the following may be required due to the datbasetable connector.
-- This exposes the composite primary key to midPoint as "id".
CREATE VIEW MIDPOINT_GROUPS_V AS
SELECT CONCAT(GROUP_NAME, SUBJECT_ID) AS id,
       GROUP_NAME,
       SUBJECT_ID,
       CREATED,
       UPDATED
FROM MIDPOINT_GROUPS;
--End optional view for option 3

--If using option 4 (the database table aggregate) then the following is required
CREATE VIEW MIDPOINT_GROUPS_V AS
SELECT SUBJECT_ID,
       string_agg(GROUP_NAME::text, ','::text) AS GROUP_MEMBERSHIP_LIST,
       min(CREATED) AS last_created,
       max(UPDATED) AS last_updated
FROM MIDPOINT_GROUPS
GROUP BY SUBJECT_ID;
--End required view for option 4

--End midpoint-Grouper SQL Integration