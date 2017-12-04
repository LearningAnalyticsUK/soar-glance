-- Create the collections table
CREATE TABLE IF NOT EXISTS collection (
  id VARCHAR(40) PRIMARY KEY,
  module_num VARCHAR(8),
  num_entries INT
);

-- Create the collection_membership table. Not quite a pivot table as surveys can only have
--  one collection and we only ever want to
CREATE TABLE IF NOT EXISTS collection_membership (
  collection_id VARCHAR(40) NOT NULL CONSTRAINT membership_collection_fkey REFERENCES collection ON DELETE CASCADE,
  survey_id VARCHAR(40) UNIQUE NOT NULL CONSTRAINT collection_survey_fkey REFERENCES survey ON DELETE CASCADE,
  membership_idx INT NOT NULL,
  last BOOLEAN NOT NULL,
  PRIMARY KEY(collection_id, membership_idx)
);

CREATE INDEX collection_membership_idx ON collection_membership (collection_id);