-- Create RecapSessions Table with TS
CREATE TABLE IF NOT EXISTS recap_session
(
  id UUID NOT NULL
    CONSTRAINT recap_session_pkey
    PRIMARY KEY,
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  student_num VARCHAR(16) NOT NULL
    CONSTRAINT recap_session_student_num_fkey
    REFERENCES student
    ON DELETE CASCADE,
  recap_item UUID NOT NULL,
  seconds_listened INTEGER
)
;

-- Create ClusterSessions Table with TS
CREATE TABLE IF NOT EXISTS cluster_session
(
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  end_time TIMESTAMP WITH TIME ZONE NOT NULL,
  machine_name VARCHAR(32),
  student_num VARCHAR(16) NOT NULL
    CONSTRAINT cluster_session_student_num_fkey
    REFERENCES student
    ON DELETE CASCADE,
  cluster_id UUID NOT NULL
)
;
