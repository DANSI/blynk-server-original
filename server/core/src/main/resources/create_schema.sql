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

CREATE TABLE reporting_raw_data (
  email text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts timestamp,
  stringValue text,
  doubleValue float8,

  PRIMARY KEY (email, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_minute (
  email text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts timestamp,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_hourly (
  email text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts timestamp,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_daily (
  email text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts timestamp,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_app_stat_minute (
  region text,
  ts timestamp,
  active int4,
  active_week int4,
  active_month int4,
  minute_rate int4,
  connected int4,
  online_apps int4,
  online_hards int4,
  total_online_apps int4,
  total_online_hards int4,
  registrations int4,
  PRIMARY KEY (region, ts)
);

CREATE TABLE reporting_app_command_stat_minute (
  region text,
  ts timestamp,
  response int4,
  register int4,
  login int4,
  load_profile int4,
  app_sync int4,
  sharing int4,
  get_token int4,
  ping int4,
  activate int4,
  deactivate int4,
  refresh_token int4,
  get_graph_data int4,
  export_graph_data int4,
  set_widget_property int4,
  bridge int4,
  hardware int4,
  get_share_dash int4,
  get_share_token int4,
  refresh_share_token int4,
  share_login int4,
  create_project int4,
  update_project int4,
  delete_project int4,
  hardware_sync int4,
  internal int4,
  sms int4,
  tweet int4,
  email int4,
  push int4,
  add_push_token int4,
  create_widget int4,
  update_widget int4,
  delete_widget int4,
  create_device int4,
  update_device int4,
  delete_device int4,
  get_devices int4,
  create_tag int4,
  update_tag int4,
  delete_tag int4,
  get_tags int4,
  add_energy int4,
  get_energy int4,
  get_server int4,
  connect_redirect int4,
  web_sockets int4,
  eventor int4,
  webhooks int4,
  appTotal int4,
  hardTotal int4,

  PRIMARY KEY (region, ts)
);

CREATE TABLE reporting_http_command_stat_minute (
  region text,
  ts timestamp,
  is_hardware_connected int4,
  is_app_connected int4,
  get_pin_data int4,
  update_pin int4,
  email int4,
  push int4,
  get_project int4,
  qr int4,
  get_history_pin_data int4,
  total int4,
  PRIMARY KEY (region, ts)
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