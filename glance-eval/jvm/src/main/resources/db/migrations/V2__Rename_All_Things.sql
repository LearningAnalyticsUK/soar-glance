ALTER TABLE surveys RENAME CONSTRAINT surveys_pkey TO survey_pkey;
ALTER TABLE surveys RENAME survey;

ALTER TABLE students RENAME CONSTRAINT students_pkey TO student_pkey;
ALTER TABLE students RENAME student;

ALTER TABLE surveys_students
  RENAME CONSTRAINT surveys_students_survey_id_fkey TO survey_student_survey_id_fkey;
ALTER TABLE surveys_students
  RENAME CONSTRAINT surveys_students_student_num_fkey TO survey_student_student_num_fkey;
ALTER TABLE surveys_students
  RENAME CONSTRAINT surveys_students_pkey TO survey_student_pkey;
ALTER TABLE surveys_students RENAME survey_student;

ALTER TABLE queries
  RENAME CONSTRAINT queries_student_num_fkey TO rankable_student_num_fkey;
ALTER TABLE queries
  RENAME CONSTRAINT queries_pkey TO query_pkey;
ALTER TABLE queries RENAME query;

ALTER TABLE survey_queries
  RENAME CONSTRAINT survey_queries_survey_id_fkey TO survey_query_survey_id_fkey;
ALTER TABLE survey_queries
  RENAME CONSTRAINT survey_queries_pkey TO survey_query_pkey;
ALTER TABLE survey_queries
  RENAME CONSTRAINT survey_queries_student_num_fkey TO survey_query_student_num_fkey;
ALTER TABLE survey_queries RENAME survey_query;

ALTER TABLE surveys_respondents
  RENAME CONSTRAINT surveys_respondents_pkey TO survey_respondent_pkey;
ALTER TABLE surveys_respondents
  RENAME CONSTRAINT surveys_respondents_survey_id_fkey TO survey_respondent_survey_id_fkey;
ALTER TABLE surveys_respondents
  RENAME CONSTRAINT surveys_respondents_respondent_key TO survey_respondent_respondent_key;
ALTER TABLE surveys_respondents RENAME survey_respondent;





