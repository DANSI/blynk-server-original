DROP KEYSPACE IF EXISTS reporting;
CREATE KEYSPACE reporting WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy' };

USE reporting;

CREATE TABLE average_minute (
  username text,
  project_id int,
  pin int,
  pinType ascii,
  ts bigint,
  value double,
  PRIMARY KEY ((username, project_id, pin, pinType), ts)
) WITH CLUSTERING ORDER BY (ts DESC);

CREATE TABLE average_hourly (
  username text,
  project_id int,
  pin int,
  pinType ascii,
  ts bigint,
  value double,
  PRIMARY KEY ((username, project_id, pin, pinType), ts)
) WITH CLUSTERING ORDER BY (ts DESC);

CREATE TABLE average_daily (
  username text,
  project_id int,
  pin int,
  pinType ascii,
  ts bigint,
  value double,
  PRIMARY KEY ((username, project_id, pin, pinType), ts)
) WITH CLUSTERING ORDER BY (ts DESC);