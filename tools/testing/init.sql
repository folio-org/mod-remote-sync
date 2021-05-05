select 1;

CREATE USER folio_admin WITH PASSWORD 'folio_admin';
CREATE USER folio WITH PASSWORD 'folio';

DROP DATABASE if exists okapi_modules;
CREATE DATABASE okapi_modules;
GRANT ALL PRIVILEGES ON DATABASE okapi_modules to folio_admin;
GRANT ALL PRIVILEGES ON DATABASE okapi_modules to folio;

DROP DATABASE if exists okapi_modules_test;
CREATE DATABASE okapi_modules_test;
GRANT ALL PRIVILEGES ON DATABASE okapi_modules_test to folio_admin;
GRANT ALL PRIVILEGES ON DATABASE okapi_modules_test to folio;

DROP DATABASE if exists olftest;
CREATE DATABASE olftest;
GRANT ALL PRIVILEGES ON DATABASE olftest to folio_admin;
GRANT ALL PRIVILEGES ON DATABASE olftest to folio;

ALTER USER folio_admin CREATEDB;
ALTER USER folio_admin CREATEROLE;
ALTER USER folio_admin WITH SUPERUSER;
ALTER USER folio CREATEDB;
ALTER USER folio CREATEROLE;
ALTER USER folio WITH SUPERUSER;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
