ALTER TABLE survey ADD COLUMN module_num VARCHAR(8);

-- Update module_num values for existing records. Should probably be a clean db, but ... times must
UPDATE survey SET module_num = 'CSC3621' WHERE survey.id = '6f2be227-fda6-4232-954b-0afde60ac325';
UPDATE survey SET module_num = 'CSC3222' WHERE survey.id = 'd5eaaef4-d633-4a90-bf7a-1095b395aea9';
UPDATE survey SET module_num = 'CSC2026' WHERE survey.id = '5999acc8-a7a3-4332-838a-d7aefde54733';

-- Add NOT NULL constraint
ALTER TABLE survey
  ALTER COLUMN module_num SET NOT NULL;

-- Drop survey_query constraint on query then drop part of the composite key
ALTER TABLE survey_query
  DROP CONSTRAINT survey_query_student_num_fkey,
  DROP COLUMN module_num;

DROP TABLE query;

DROP TABLE modules;


