Functional Requirements - Use Springboot for backend and UI of your choice but I should be able to run the whole project in IntelliJ with single deployment and as a microservice

1. Sources - Configurable from UI
   •	Local file system (folders + glob pattern).
   •	SFTP server (configurable host, port, user, path).
   . S3 bucket/folder
   •	Supported file formats:
   •	Parquet (large files chunked safely to avoid OOM).
   •	CSV (direct ingest supported).
   . Excel (direct ingest supported).

2. Targets - Configurable from UI
   •	Amazon Redshift
   •	Use COPY command from staged S3 files.
   •	Postgres / MySQL
   •	Use bulk inserts/updates (no COPY). If COPY available, use it.
   •	Multiple customer projects supported, each with:
   •	DB connection details.
   •	Allowed users (per-project access).

3. Job Management
   •	Each ingestion is a Job, persisted in DB.
   •	Tracks:
   •	Job ID, project, target schema/table, source info.
   •	Start/finish time, status, error count.
   •	Total rows processed, rows successfully loaded.
   . Failed records file for each job to download from deployed machine or S3. If file is large, store on S3 and if not on deployed machine
   •	Jobs can be run:
   •	Interactively (UI).
   •	Scheduled/CLI batch job.
   •	Cleanup support:
   •	Configurable retention (per customer) from UI.
   •	Old jobs and artifacts cleaned up from DB + S3 + Deployed machine.

4. Mapping Designer
   •	UI-driven schema mapping (no properties file).
   •	Workflow:
    1.	Pick project → schema → table.
    2.	Fetch DB columns automatically.
    3.	Map file headers → DB columns.
          •	Options: required, default value, transform (date/int/decimal/bool), format string.
    4.	Save mapping for reuse (by filename regex).
          •	Supports:
          •	Upsert keys (single/composite).
          •	Validation rules:
          •	Mandatory fields.
          •	Type conversions.
          •	Default values.
          •	Aggregation to parent tables (optional):
          •	Define parent table.
          •	Group-by keys.
          •	Metrics (SUM/COUNT/MIN/MAX).
          •	Record date = earliest record date or current date.
          •	JSON columns:
          •	Define JSON target column.
          •	Map nested fields (name:source).

5. Options
   •	Configurable in UI (not properties file):
   •	Composite key.
   •	Chunk size (MB).
   •	Max open CSV writers.
   •	Parallelism (# of files).
   •	Append vs overwrite.
   •	Row error limit (per job).
   •	Aggregate keys for dashboards.
   •	Retention days.
   •	S3 bucket/folder.

6. Validation & Error Handling
   •	Validations happen during mapping and should be configurable from UI only
   •	Missing mandatory fields.
   •	Format errors (e.g., bad date).
   •	Detailed row-wise error list:
   •	Available in job results.
   •	If errors > configured limit → dump to file on S3.
   •	Summary in UI:
   •	Total rows (per file + overall).
   •	Successful rows.
   •	Error rows (with S3 error file if large).

7. Authentication & Authorization
   •	SSO integration (Google domain or equivalent).
   •	Admin users can:
   •	Manage customers, projects, and users.
   •	Restrict user access to specific projects.

⸻

Non-Functional Requirements
•	Performance:
•	Use Java 21 virtual threads for concurrency.
•	Stream processing for large files (avoid OOM).
•	Reliability:
•	Data integrity preserved.
•	Append mode doesn’t duplicate headers.
•	Extensibility:
•	UI modularized (jobs.js, mapping.js, options.js, auth.js).
•	Backend structured for new DBs/sources.

⸻

UI Modules

Jobs
•	Start job (LOCAL/SFTP, folder, glob, schema, table).
•	Show recent jobs with status and row counts.
•	View job errors + download error file if applicable.

Mapping
•	Schema/table picker (from DB).
•	Mapping grid (file header → DB column).
•	Aggregation + JSON mapping.
•	Save mapping (per regex).

Options
•	Global/system options editor (keys, chunk size, parallelism, retention, etc.).

Auth
•	Show login/logout.
•	Display signed-in user & role.
•	Restrict project access for non-admins.

Help
•	Usage guide.
•	Troubleshooting steps.
. Project setup and run steps