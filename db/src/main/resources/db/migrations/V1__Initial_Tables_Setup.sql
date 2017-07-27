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

CREATE TABLE IF NOT EXISTS school
(
  code VARCHAR(16) NOT NULL
    CONSTRAINT school_pkey
    PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  faculty_code VARCHAR(16)
)
;

CREATE TABLE IF NOT EXISTS programme
(
  code VARCHAR(16) NOT NULL
    CONSTRAINT programme_pkey
    PRIMARY KEY,
  title VARCHAR(128) NOT NULL,
  type VARCHAR(16) NOT NULL,
  school_code VARCHAR(16)
    CONSTRAINT programme_school_code_fkey
    REFERENCES school
)
;

CREATE TABLE IF NOT EXISTS module
(
  code VARCHAR(16) NOT NULL
    CONSTRAINT module_pkey
    PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  stage_id VARCHAR(40) NOT NULL
    CONSTRAINT module_stage_id_fkey
    REFERENCES stage,
  programme_code VARCHAR(16) NOT NULL
    CONSTRAINT module_programme_code_fkey
    REFERENCES programme
)
;

CREATE TABLE IF NOT EXISTS module_programme
(
  module_code VARCHAR(16) NOT NULL
    CONSTRAINT module_programme_module_code_fkey
    REFERENCES module
    ON DELETE CASCADE,
  programme_code VARCHAR(16) NOT NULL
    CONSTRAINT module_programme_programme_code_fkey
    REFERENCES programme
    ON DELETE CASCADE,
  PRIMARY KEY (module_code, programme_code)
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

-- Check if composite UNIQUE requirement on table definition has the same semantics and if so change.
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
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
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
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  end_time TIMESTAMP WITH TIME ZONE NOT NULL,
  machine_name VARCHAR(32),
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


