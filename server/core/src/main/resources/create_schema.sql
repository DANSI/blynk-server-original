CREATE DATABASE blynk;

\connect blynk

CREATE TABLE users (
  username text PRIMARY KEY,
  region text,
  json text
);

CREATE TABLE redeem (
  token character(32) PRIMARY KEY,
  company text,
  isRedeemed boolean DEFAULT FALSE,
  username text,
  version integer NOT NULL DEFAULT 1
);

CREATE TABLE reporting_average_minute (
  username text,
  project_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_hourly (
  username text,
  project_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_daily (
  username text,
  project_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, pin, pinType, ts)
);

create user test with password 'test';
GRANT CONNECT ON DATABASE blynk TO test;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;