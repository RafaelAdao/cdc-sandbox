CREATE DATABASE cdc_sandbox_experiment_1;

\c cdc_sandbox_experiment_1;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE my_table (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  description VARCHAR(255),
  value FLOAT,
  timestamp TIMESTAMP
);

INSERT INTO
  my_table (description, value, timestamp)
VALUES
  ('Product A', 10.50, NOW()),
  ('Product B', 20.75, NOW()),
  ('Product C', 15.00, NOW()),
  ('Product D', 8.99, NOW()),
  ('Product E', 12.25, NOW());