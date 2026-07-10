# Environment variables (Phase 0 infrastructure)

Do not commit real secrets. Use your platform's secret store in production.

## Attachment storage

Local development (default):

- `APP_STORAGE_PROVIDER=local` — files under `app.storage.local.base-dir` (default `./uploads`).

S3-compatible production:

- `APP_STORAGE_PROVIDER=s3`
- `APP_STORAGE_S3_BUCKET`
- `APP_STORAGE_S3_REGION` (e.g. `us-east-1`; also used when pointing at custom endpoints)
- `APP_STORAGE_S3_PREFIX` (optional key prefix)
- `APP_STORAGE_S3_ACCESS_KEY`
- `APP_STORAGE_S3_SECRET_KEY`
- `APP_STORAGE_S3_ENDPOINT` (optional; MinIO, R2, etc.)
- `APP_STORAGE_S3_PATH_STYLE_ACCESS` (`true` / `false`)

Existing files in local `./uploads` are **not** migrated when switching providers; enable S3 before collecting production uploads or plan a separate migration.

## Background jobs

- `APP_BACKGROUND_JOBS_ENABLED` (`true` / `false`, default `true`)
- `APP_BACKGROUND_JOBS_POLL_INTERVAL_SECONDS` (default `30`)
- `APP_BACKGROUND_JOBS_BATCH_SIZE` (default `10`)

The worker uses PostgreSQL `FOR UPDATE SKIP LOCKED` and is intended for a **single** app instance unless you add stronger coordination for multiple instances.

## Integration settings (encrypted secrets)

- `APP_INTEGRATIONS_ENCRYPTION_KEY` — long random passphrase; required when Spring profiles include `prod` or `production`. If unset in dev, a **non-production** protector is used and a warning is logged.

YAML equivalents live under `app.storage`, `app.background-jobs`, and `app.integrations` in `application.yml`.
