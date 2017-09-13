ALTER TABLE survey_response
    ADD COLUMN detailed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN precedes VARCHAR(40) CONSTRAINT survey_response_response_id_fkey REFERENCES survey_response;