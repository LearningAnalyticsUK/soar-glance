-- Create the ranking table in the first place
CREATE TABLE IF NOT EXISTS ranking (
  id VARCHAR(40) PRIMARY KEY,
  response_id VARCHAR(40) NOT NULL CONSTRAINT ranking_response_id_fkey REFERENCES survey_response,
  detailed BOOLEAN NOT NULL
);

-- Alter survey response to reference two rankings.
ALTER TABLE survey_response
  ADD COLUMN detailed_ranking VARCHAR(40),
  ADD COLUMN simple_ranking VARCHAR(40);

ALTER TABLE survey_response
  ALTER COLUMN detailed_ranking SET NOT NULL,
  ALTER COLUMN simple_ranking SET NOT NULL;

-- Alter student_rank and rank_change to reference ranking, rather than response_id directly
ALTER TABLE rank_change
  DROP CONSTRAINT IF EXISTS rank_change_response_id_fkey,
  RENAME COLUMN response_id TO ranking_id;

ALTER TABLE rank_change
  ADD CONSTRAINT rank_change_ranking_id_fkey
  FOREIGN KEY (ranking_id) REFERENCES ranking;

ALTER TABLE student_rank
  DROP CONSTRAINT IF EXISTS student_rank_response_id_fkey,
  RENAME COLUMN response_id TO ranking_id;

ALTER TABLE student_rank
  ADD CONSTRAINT student_rank_ranking_id_fkey
  FOREIGN KEY (ranking_id) REFERENCES ranking;

DROP INDEX rank_change_response_idx;
CREATE INDEX rank_change_ranking_idx ON rank_change (ranking_id);
CREATE INDEX student_rank_ranking_idx ON student_rank (ranking_id);

