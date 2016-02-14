DROP KEYSPACE mykeyspace;
CREATE KEYSPACE mykeyspace WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE mykeyspace;

CREATE TABLE report_average_minute (
  username text,
  project_id int,
  pin int,
  pinType ascii,
  ts bigint,
  value double,
  PRIMARY KEY ((username, project_id, pin, pinType), ts)
) WITH CLUSTERING ORDER BY (ts DESC);