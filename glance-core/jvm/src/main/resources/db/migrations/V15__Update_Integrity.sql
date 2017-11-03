ALTER TABLE ranking
  DROP CONSTRAINT ranking_response_id_fkey,
  ADD CONSTRAINT ranking_response_id_fkey
  FOREIGN KEY (response_id)
  REFERENCES survey_response ON DELETE CASCADE;

ALTER TABLE rank_change
  DROP CONSTRAINT rank_change_ranking_id_fkey,
  ADD CONSTRAINT rank_change_ranking_id_fkey
  FOREIGN KEY (ranking_id)
  REFERENCES ranking ON DELETE CASCADE;

ALTER TABLE student_rank
  DROP CONSTRAINT student_rank_ranking_id_fkey,
  ADD CONSTRAINT student_rank_ranking_id_fkey
  FOREIGN KEY (ranking_id)
  REFERENCES ranking ON DELETE CASCADE;

