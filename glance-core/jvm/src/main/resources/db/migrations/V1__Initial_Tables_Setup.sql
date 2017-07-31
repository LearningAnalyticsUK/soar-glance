create table if not exists surveys
(
  id varchar(40) not null
    constraint surveys_pkey
    primary key
)
;

create table if not exists students
(
  num varchar(10) not null
    constraint students_pkey
    primary key
)
;

create table if not exists surveys_students
(
  survey_id varchar(40) not null
    constraint surveys_students_survey_id_fkey
    references surveys
    on delete cascade,
  student_num varchar(10) not null
    constraint surveys_students_student_num_fkey
    references students
    on delete cascade,
  constraint surveys_students_pkey
  primary key (survey_id, student_num)
)
;

create table if not exists queries
(
  student_num varchar(10) not null
    constraint queries_student_num_fkey
    references students
    on delete cascade,
  module_num varchar(8) not null,
  constraint queries_pkey
  primary key (student_num, module_num)
)
;

create table if not exists survey_queries
(
  survey_id varchar(40) not null
    constraint survey_queries_survey_id_fkey
    references surveys
    on delete cascade,
  student_num varchar(10) not null,
  module_num varchar(8) not null,
  constraint survey_queries_pkey
  primary key (survey_id, student_num, module_num),
  constraint survey_queries_student_num_fkey
  foreign key (student_num, module_num) references queries
  on delete cascade
)
;

create table if not exists module_score
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
  module_num varchar(8) not null
)
;

create table if not exists surveys_respondents
(
  id varchar not null
    constraint surveys_respondents_pkey
    primary key,
  survey_id varchar
    constraint surveys_respondents_survey_id_fkey
    references surveys
    on delete cascade,
  respondent varchar not null
    constraint surveys_respondents_respondent_key
    unique,
  submitted timestamp with time zone not null
)
;

create table if not exists survey_response
(
  id varchar not null
    constraint survey_response_pkey
    primary key,
  respondent_id varchar
    constraint survey_response_respondent_id_fkey
    references surveys_respondents
    on delete cascade,
  student_num varchar(10) not null,
  module_num varchar not null,
  predicted_score numeric(5,2) not null
    constraint survey_response_predicted_score_check
    check (predicted_score > 0.00)
    constraint survey_response_predicted_score_check1
    check (predicted_score < 100.00),
  constraint survey_response_student_num_fkey
  foreign key (student_num, module_num) references queries
  on delete cascade
)
;

