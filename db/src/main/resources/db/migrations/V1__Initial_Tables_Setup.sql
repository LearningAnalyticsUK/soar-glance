CREATE TABLE IF NOT EXISTS student
(
  num VARCHAR(16) NOT NULL
    CONSTRAINT student_pkey
    PRIMARY KEY
)
;

CREATE TABLE IF NOT EXISTS stage
(
  id UUID NOT NULL
    CONSTRAINT stage_pkey
    PRIMARY KEY,
  legacy_id INTEGER,
  num INTEGER NOT NULL,
  study_description VARCHAR(128) NOT NULL
)
;

-- -- Will add these next two tables in a subsequent migration when they are needed.
-- create table school
-- (
--   code VARCHAR(8) not null
--     CONSTRAINT school_pkey
--     PRIMARY KEY,
--   name VARCHAR(128) not null,
--   faculty_code VARCHAR(8)
-- )
-- ;
--
-- create table programme
-- (
--   code VARCHAR(8) not null
--     CONSTRAINT programme_pkey
--     PRIMARY KEY,
--   title VARCHAR(128) not null,
--   type VARCHAR(8) not null,
--   school_code VARCHAR(8)
--     CONSTRAINT programme_school_code_fkey
--     REFERENCES schools
--     ON DELETE CASCADE
-- )
-- ;

CREATE TABLE IF NOT EXISTS module
(
  code VARCHAR(16) NOT NULL
    CONSTRAINT module_pkey
    PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  stage_id VARCHAR(40) NOT NULL
    CONSTRAINT module_stage_id_fkey
    REFERENCES stage
)
;


CREATE TABLE IF NOT EXISTS module_score
(
  id UUID NOT NULL
    CONSTRAINT module_score_pkey
    PRIMARY KEY,
  student_num VARCHAR(16)
    CONSTRAINT module_score_student_num_fkey
    REFERENCES students
    ON DELETE CASCADE,
  score NUMERIC(5,2) NOT NULL
    CONSTRAINT module_score_score_check
    CHECK (score >= 0.00)
    CONSTRAINT module_score_score_check1
    CHECK (score <= 100.00),
  module_code VARCHAR(16) NOT NULL
    CONSTRAINT module_score_module_code_fkey
    REFERENCES modules
    ON DELETE RESTRICT
)
;

CREATE INDEX student_score_idx ON module_score (student_num);
CREATE INDEX module_score_idx ON module_score (module_code);

CREATE TABLE IF NOT EXISTS recap_item
(
  id UUID NOT NULL
    CONSTRAINT recap_item_pkey
    PRIMARY KEY,
  legacy_id INTEGER,
  name VARCHAR(255),
  module_code VARCHAR(16) NOT NULL
    CONSTRAINT recap_item_module_code_fkey
    REFERENCES modules
    ON DELETE CASCADE
)
;

CREATE TABLE IF NOT EXISTS recap_session
(
  id UUID NOT NULL
    CONSTRAINT recap_session_pkey
    PRIMARY KEY,
  start TIMESTAMP WITH TIME ZONE NOT NULL,
  student_num VARCHAR(16) NOT NULL
    CONSTRAINT recap_session_student_num_fkey
    REFERENCES students
    ON DELETE CASCADE,
  recap_item UUID NOT NULL
    CONSTRAINT recap_session_recap_item_fkey
    REFERENCES recap_item
    ON DELETE CASCADE,
  seconds_listened INTEGER
)
;

CREATE TABLE IF NOT EXISTS cluster
(
  id UUID NOT NULL
    CONSTRAINT cluster_pkey
    PRIMARY KEY,
  legacy_id INTEGER,
  pc_name VARCHAR(32) NOT NULL,
  cluster VARCHAR(128) NOT NULL,
  building VARCHAR(128) NOT NULL
)
;

CREATE TABLE IF NOT EXISTS cluster_session
(
  start TIMESTAMP WITH TIME ZONE NOT NULL,
  end TIMESTAMP WITH TIME ZONE NOT NULL,
  student_num VARCHAR(16) NOT NULL
    CONSTRAINT cluster_session_student_num_fkey
    REFERENCES students
    ON DELETE CASCADE,
  cluster_id UUID NOT NULL
    CONSTRAINT cluster_session_cluster_id_fkey
    REFERENCES cluster
    ON DELETE CASCADE
)
;


