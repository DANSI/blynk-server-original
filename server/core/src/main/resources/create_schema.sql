CREATE DATABASE blynk;

\connect blynk

CREATE TABLE users (
  username text NOT NULL,
  appName text NOT NULL,
  region text,
  json text,
  PRIMARY KEY(username, appName)
);

CREATE TABLE redeem (
  token character(32) PRIMARY KEY,
  company text,
  isRedeemed boolean DEFAULT FALSE,
  reward integer NOT NULL DEFAULT 0,
  username text,
  version integer NOT NULL DEFAULT 1,
  ts timestamp
);

CREATE TABLE purchase (
  username text,
  reward integer NOT NULL,
  transactionId text,
  price float8,
  ts timestamp NOT NULL DEFAULT NOW(),
  PRIMARY KEY (username, transactionId)
);

CREATE TABLE reporting_average_minute (
  username text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_hourly (
  username text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_daily (
  username text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts int8,
  value float8,
  PRIMARY KEY (username, project_id, device_id, pin, pinType, ts)
);

create user test with password 'test';
GRANT CONNECT ON DATABASE blynk TO test;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;