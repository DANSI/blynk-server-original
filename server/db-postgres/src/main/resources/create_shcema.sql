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