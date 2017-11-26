-- Create the visualisations table
CREATE TABLE IF NOT EXISTS visualisation (
  id VARCHAR(128) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  description TEXT
  CONSTRAINT valid_id CHECK (id ~* '^[a-z]+[a-z_]*[a-z]+$')
);

-- Create the survey_visualisation pivot table
CREATE TABLE IF NOT EXISTS survey_visualisation (
  survey_id VARCHAR(40) NOT NULL CONSTRAINT survey_visualisation_fkey REFERENCES survey ON DELETE CASCADE,
  visualisation_id VARCHAR(128) NOT NULL CONSTRAINT visualisation_survey_fkey REFERENCES visualisation ON DELETE CASCADE,
  PRIMARY KEY (survey_id, visualisation_id)
);

CREATE INDEX survey_visualisation_idx ON survey_visualisation (survey_id);

