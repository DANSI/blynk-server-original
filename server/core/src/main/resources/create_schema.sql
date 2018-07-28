CREATE DATABASE blynk;

\connect blynk

CREATE TABLE users (
  email text NOT NULL,
  appName text NOT NULL,
  region text,
  ip text,
  name text,
  pass text,
  last_modified timestamp,
  last_logged timestamp,
  last_logged_ip text,
  is_facebook_user bool,
  is_super_admin bool DEFAULT FALSE,
  energy int,
  json text,
  PRIMARY KEY(email, appName)
);

CREATE TABLE redeem (
  token character(32) PRIMARY KEY,
  company text,
  isRedeemed boolean DEFAULT FALSE,
  reward integer NOT NULL DEFAULT 0,
  email text,
  version integer NOT NULL DEFAULT 1,
  ts timestamp
);

CREATE TABLE flashed_tokens (
  token character(32),
  app_name text,
  email text,
  project_id int4 NOT NULL,
  device_id int4 NOT NULL,
  is_activated boolean DEFAULT FALSE,
  ts timestamp,
  PRIMARY KEY(token, app_name)
);

CREATE TABLE cloned_projects (
  token character(32),
  ts timestamp,
  json text,
  PRIMARY KEY(token)
);

CREATE TABLE purchase (
  email text,
  reward integer NOT NULL,
  transactionId text,
  price float8,
  ts timestamp NOT NULL DEFAULT NOW(),
  PRIMARY KEY (email, transactionId)
);

CREATE TABLE forwarding_tokens (
  token character(32),
  host text,
  email text,
  project_id int4,
  device_id int4,
  ts timestamp DEFAULT NOW(),
  PRIMARY KEY(token, host)
);

create user test with password 'test';
GRANT CONNECT ON DATABASE blynk TO test;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;

-- Create a group
CREATE ROLE readaccess;
-- Grant access to existing tables
GRANT USAGE ON SCHEMA public TO readaccess;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readaccess;
-- Grant access to future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readaccess;