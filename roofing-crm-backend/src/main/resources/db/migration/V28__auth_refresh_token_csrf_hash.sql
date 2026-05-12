-- Adds a per-session CSRF token hash so we can validate the X-CSRF-Refresh
-- header against a value bound to the current refresh session.
ALTER TABLE auth_refresh_token_sessions
    ADD COLUMN csrf_token_hash VARCHAR(64);
