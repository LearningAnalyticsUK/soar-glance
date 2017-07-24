create table students
(
  num varchar(10) not null
    constraint students_pkey
    primary key
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
  module_num varchar(8) not null
)
;


