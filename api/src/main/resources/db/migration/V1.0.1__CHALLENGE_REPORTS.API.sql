ALTER TABLE CHALLENGE_REPORTS_SESSION
    ADD FUNDING_RATE VARCHAR2(10);

ALTER TABLE CHALLENGE_REPORTS_SESSION
    ADD FINAL_DATE_FOR_CHANGES TIMESTAMP;

ALTER TABLE CHALLENGE_REPORTS_SESSION
    ADD EXECUTIVE_DIRECTOR_NAME VARCHAR2(255);
