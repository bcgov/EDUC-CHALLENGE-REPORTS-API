CREATE TABLE CHALLENGE_REPORTS_SAGA
(
    SAGA_ID                      RAW(16)              NOT NULL,
    CHALLENGE_REPORTS_SESSION_ID RAW(16),
    SAGA_NAME                    VARCHAR2(50)         NOT NULL,
    SAGA_STATE                   VARCHAR2(100)        NOT NULL,
    PAYLOAD                      VARCHAR2(4000)       NOT NULL,
    STATUS                       VARCHAR2(20)         NOT NULL,
    CREATE_USER                  VARCHAR2(32)         NOT NULL,
    CREATE_DATE                  DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                  VARCHAR2(32)         NOT NULL,
    UPDATE_DATE                  DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT CHALLENGE_REPORTS_SAGA_PK PRIMARY KEY (SAGA_ID)
);
CREATE INDEX CHALLENGE_REPORTS_SAGA_STATUS_IDX ON CHALLENGE_REPORTS_SAGA (STATUS);
CREATE INDEX CHALLENGE_REPORTS_SAGA_CHALLENGE_REPORTS_SESSION_ID_IDX ON CHALLENGE_REPORTS_SAGA (CHALLENGE_REPORTS_SESSION_ID);

CREATE TABLE CHALLENGE_REPORTS_SAGA_EVENT_STATES
(
    SAGA_EVENT_ID       RAW(16)              NOT NULL,
    SAGA_ID             RAW(16)              NOT NULL,
    SAGA_EVENT_STATE    VARCHAR2(100)        NOT NULL,
    SAGA_EVENT_OUTCOME  VARCHAR2(100)        NOT NULL,
    SAGA_STEP_NUMBER    NUMBER(4)            NOT NULL,
    SAGA_EVENT_RESPONSE VARCHAR2(4000)       NOT NULL,
    CREATE_USER         VARCHAR2(32)         NOT NULL,
    CREATE_DATE         DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER         VARCHAR2(32)         NOT NULL,
    UPDATE_DATE         DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT CHALLENGE_REPORTS_SAGA_EVENT_STATES_PK PRIMARY KEY (SAGA_EVENT_ID)
);
ALTER TABLE CHALLENGE_REPORTS_SAGA_EVENT_STATES
    ADD CONSTRAINT CHALLENGE_REPORTS_SAGA_EVENT_STATES_SAGA_ID_FK FOREIGN KEY (SAGA_ID) REFERENCES CHALLENGE_REPORTS_SAGA (SAGA_ID);
