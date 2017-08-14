ALTER TABLE survey_response
  RENAME COLUMN respondent_id TO respondent_email;

ALTER TABLE survey_response
  ALTER COLUMN id TYPE VARCHAR(40),
  DROP CONSTRAINT survey_response_respondent_id_fkey,
  ALTER COLUMN respondent_email TYPE TEXT,
  ADD COLUMN survey_id VARCHAR(40) CONSTRAINT survey_response_survey_id_fkey REFERENCES survey ON DELETE CASCADE,
  DROP CONSTRAINT survey_response_student_num_fkey,
  DROP COLUMN student_num,
  DROP COLUMN module_num,
  DROP CONSTRAINT survey_response_predicted_score_check,
  DROP CONSTRAINT survey_response_predicted_score_check1,
  DROP COLUMN predicted_score,
  ADD COLUMN time_started TIMESTAMP WITH TIME ZONE NOT NULL,
  ADD COLUMN time_finished TIMESTAMP WITH TIME ZONE NOT NULL,
  ADD COLUMN notes TEXT;

DROP TABLE survey_respondent;

-- TODO: work out if we want an additional index on the response_id fk or whether the composite pk does this for us?
CREATE TABLE IF NOT EXISTS student_rank
(
  student_num VARCHAR(10) CONSTRAINT student_rank_student_num_fkey REFERENCES student,
  response_id VARCHAR(40) CONSTRAINT student_rank_response_id_fkey REFERENCES survey_response ON DELETE CASCADE,
  rank INT,
  CONSTRAINT student_rank_response_pkey PRIMARY KEY (response_id, rank)
)
;