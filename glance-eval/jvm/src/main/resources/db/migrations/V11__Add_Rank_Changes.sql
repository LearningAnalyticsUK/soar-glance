CREATE TABLE IF NOT EXISTS rank_change
(
  start_rank INT NOT NULL,
  end_rank INT NOT NULL,
  time TIMESTAMP WITH TIME ZONE NOT NULL,
  response_id VARCHAR(40) CONSTRAINT rank_change_response_id_fkey REFERENCES survey_response ON DELETE CASCADE
);

CREATE INDEX rank_change_response_idx ON rank_change (response_id);
-- No need for a primary key or other indices here as all we're ever going to do is fetch all rank changes for a
--  particular response.