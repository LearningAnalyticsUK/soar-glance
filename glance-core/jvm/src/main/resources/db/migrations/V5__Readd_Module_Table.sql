-- Re create module table - needed it after all
CREATE TABLE IF NOT EXISTS module
(
  num VARCHAR(8) NOT NUlL CONSTRAINT module_pkey PRIMARY KEY,
  start_date DATE,
  length INTERVAL DAY
);

-- Add module entries from module_score table
INSERT INTO module(num)
  SELECT ms.module_num FROM module_score ms ON CONFLICT DO NOTHING;

-- Add foreign key constraint to module_score
ALTER TABLE module_score
  ADD CONSTRAINT module_score_module_num_fkey FOREIGN KEY(module_num) REFERENCES module (num);

-- Add foreign key constraint to survey
ALTER TABLE survey
  ADD CONSTRAINT survey_module_num_fkey FOREIGN KEY(module_num) REFERENCES module (num);




