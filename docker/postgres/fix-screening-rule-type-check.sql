-- Hibernate ddl-auto=update does not refresh enum CHECK constraints when RuleType grows.
-- Run against screening_dev_db after adding new RuleType values, e.g.:
--   docker exec -i codestorykh-postgres psql -U dev_user -d screening_dev_db < docker/postgres/fix-screening-rule-type-check.sql

ALTER TABLE tbl_onboarding_screening_alert
  DROP CONSTRAINT IF EXISTS tbl_onboarding_screening_alert_rule_type_check;

ALTER TABLE tbl_onboarding_screening_alert
  ADD CONSTRAINT tbl_onboarding_screening_alert_rule_type_check
  CHECK (rule_type::text = ANY (ARRAY[
    'ONBOARDING_CLEAR',
    'UNDERAGE_CUSTOMER',
    'INVALID_IDENTITY',
    'HIGH_RISK_PROFILE',
    'COUNTRY_MISMATCH',
    'WATCHLIST_IDENTITY',
    'WATCHLIST_NAME',
    'WATCHLIST_COUNTRY',
    'WATCHLIST_PEP',
    'WATCHLIST_CITY',
    'WATCHLIST_ORGANIZATION',
    'WATCHLIST_ACCOUNT',
    'WATCHLIST_EMAIL_DOMAIN',
    'ADVERSE_MEDIA',
    'NAME_MISMATCH',
    'HIGH_VALUE_TRANSACTION',
    'ACCOUNT_STATUS_RESTRICTED'
  ]::text[]));

ALTER TABLE tbl_transaction_screening_alert
  DROP CONSTRAINT IF EXISTS tbl_transaction_screening_alert_rule_type_check;

ALTER TABLE tbl_transaction_screening_alert
  ADD CONSTRAINT tbl_transaction_screening_alert_rule_type_check
  CHECK (rule_type::text = ANY (ARRAY[
    'ONBOARDING_CLEAR',
    'UNDERAGE_CUSTOMER',
    'INVALID_IDENTITY',
    'HIGH_RISK_PROFILE',
    'COUNTRY_MISMATCH',
    'WATCHLIST_IDENTITY',
    'WATCHLIST_NAME',
    'WATCHLIST_COUNTRY',
    'WATCHLIST_PEP',
    'WATCHLIST_CITY',
    'WATCHLIST_ORGANIZATION',
    'WATCHLIST_ACCOUNT',
    'WATCHLIST_EMAIL_DOMAIN',
    'ADVERSE_MEDIA',
    'NAME_MISMATCH',
    'HIGH_VALUE_TRANSACTION',
    'ACCOUNT_STATUS_RESTRICTED'
  ]::text[]));
