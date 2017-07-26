create table student
(
  num varchar(10) not null
    constraint student_pkey
    primary key
)
;

create table stage
(
  id VARCHAR(40) not null
    CONSTRAINT stage_pkey
    PRIMARY KEY,
  legacy_id INTEGER,
  num INTEGER not null,
  study_description VARCHAR(127) not null
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

create table module
(
  code VARCHAR(8) not null
    CONSTRAINT module_pkey
    PRIMARY KEY,
  name VARCHAR(127) not null,
  stage_id VARCHAR(40) not null
    CONSTRAINT module_stage_id_fkey
    REFERENCES stage
)
;


create table module_score
(
  id varchar(40) not null
    constraint module_score_pkey
    primary key,
  student_num varchar(10)
    constraint module_score_student_num_fkey
    references students
    on delete cascade,
  score numeric(5,2) not null
    constraint module_score_score_check
    check (score >= 0.00)
    constraint module_score_score_check1
    check (score <= 100.00),
  module_code varchar(8) not null
    constraint module_score_module_code_fkey
    references modules
    on delete restrict
)
;

create table recap_item
(
  id varchar(40) not null
    constraint recap_item_pkey
    primary key,
  legacy_id integer,
  name varchar(255),
  module_code varchar(8) not null
    constraint recap_item_module_code_fkey
    references modules
    on delete cascade
)
;

create table recap_session
(
  id VARCHAR(40) not null
    CONSTRAINT recap_session_pkey
    PRIMARY KEY,
  start TIMESTAMP WITH TIME ZONE not null,
  student_num varchar(10) not null
    constraint recap_session_student_num_fkey
    references students
    on delete cascade,
  recap_item varchar(40) not null
    constraint recap_session_recap_item_fkey
    references recap_item
    on delete cascade,
  seconds_listened integer
)


