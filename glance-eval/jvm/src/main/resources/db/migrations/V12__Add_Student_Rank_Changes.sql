ALTER TABLE rank_change
  ADD COLUMN student_num VARCHAR(10)
  CONSTRAINT rank_change_student_num_fkey REFERENCES student;

ALTER TABLE rank_change
  ALTER COLUMN student_num SET NOT NULL;