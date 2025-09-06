# DataFit Migrater — README

This repository contains **DataFit Migrater**: a Spring Boot application + modular UI to ingest Parquet/CSV/Excel files from Local/SFTP/S3 into Redshift/Postgres/MySQL with mapping designer, job management, validation, and admin controls.

---

## What's included
- Spring Boot backend (Java 21, Maven)
- Static UI under `src/main/resources/static` (modular JS: `jobs.js`, `mapping.js`, `admin.js`, `auth.js`, etc.)
- Entities and JPA repositories for Projects, Settings, Mappings, Jobs, JobErrors, UserAccess
- SFTP, S3, Parquet chunking, Redshift COPY, Bulk upsert for Postgres/MySQL
- Admin UI: manage projects, paste `known_hosts`, configure SFTP, assign users to projects
- Mapping Designer (drag & drop) with server-side validation
- Unit and integration tests (JUnit + Spring Boot Test)
- Virtual-thread-based job execution (requires JDK 21)

---

## Quick start (IntelliJ / Local)

### Prerequisites
- JDK **21** installed and available to IntelliJ.
- Maven 3.8+ (for command-line builds).
- (Optional) Docker if you want to run Postgres/MySQL containers for integration tests.
- AWS credentials (if you will use S3 / Redshift): set via environment variables or instance profile:
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, optionally `AWS_REGION`.

### Download & open in IntelliJ
1. Extract the ZIP you downloaded into a folder:
   ```bash
   unzip datafit-migrater-full-complete-all.zip -d ~/projects/datafit-migrater
   ```
2. Open IntelliJ → **Open** → select the project root (where `pom.xml` resides).
3. Let IntelliJ import Maven projects and download dependencies.
4. Set **Project SDK** to JDK 21 (File → Project Structure → Project SDK).

### Application properties & environment
The app primarily stores per-project DB credentials and settings in the DB. For local convenience, you may use `application.yml` or environment variables to set defaults (example keys):
- OAuth2 (SSO) — configure Google (or another IdP) client in `application.yml` or environment variables:
  ```yaml
  spring:
    security:
      oauth2:
        client:
          registration:
            google:
              client-id: <CLIENT_ID>
              client-secret: <CLIENT_SECRET>
              scope: openid, email, profile
  ```
- AWS: use standard AWS SDK environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`).
- For development you can use H2 (in-memory) DB — the project is configured to work with JPA defaults for dev.
  You can also create Projects in the UI to point to Postgres/MySQL/Redshift instances.

### Seeding an admin user (bootstrap)
One admin entry must exist in DB for admin operations. You can seed manually in your DB:

```sql
INSERT INTO user_access (email, role_name) VALUES ('you@domain.com', 'ADMIN');
```

Alternatively, create a `UserAccess` record using a simple SQL client or add via a small one-off REST endpoint you create locally.

### Run the application
From IntelliJ, run the Spring Boot main class:
- `com.datafit.migrater.Application` — Run/Debug configuration → Run.

App UI is served at: `http://localhost:8080/`

---

## Usage guide (high level)

### Admin workflow
1. Sign in with SSO (Google or configured IdP).
2. Go to **Admin** tab:
   - Create or edit a Project (admin-only).
   - Paste `known_hosts` contents for that project's SFTP host (important for secure host verification).
   - Configure SFTP host/port/user/password or private key via Project Settings (admin-only).
   - Configure S3 bucket/folder and Redshift IAM role ARN if using Redshift COPY.
   - Add users and assign roles (`ADMIN` / `USER`) to projects — only assigned users can see/start jobs for that project.

### Mapping designer
1. Go to **Mapping** tab.
2. Select Project → enter schema and table name → paste a sample header row (comma-separated) or upload sample file headers.
3. Click **Fetch DB Columns** — the UI will load DB columns from the chosen project's JDBC connection.
4. Drag file headers onto DB columns to map them.
5. Mark required columns, set defaults and transforms (int, date, boolean, etc.), and save mapping.
6. Mapping is saved as JSON and used during jobs (validation & upsert keys derived automatically).

### Running Jobs
1. Go to **Jobs** tab → Start Job:
   - Choose Source Type: `LOCAL`, `SFTP`, or `S3`.
   - Provide folder/path and glob pattern (e.g., `*.parquet`).
   - Choose mapping (if available) and target schema/table.
   - Configure job-level options: chunk size (MB), composite key, max open CSV writers, parallelism, append vs overwrite, error limit, S3 staging folder.
2. Jobs are persisted in DB and run asynchronously using virtual threads. You can monitor status and view row counts and detailed error lists per job.

### Error handling & artifacts
- Row-level errors are recorded and shown in the job details. If error rows exceed configured limit, an error CSV is uploaded to S3 and the job stores the S3 path.
- You can download smaller error files directly from the deployed machine via the job details UI (admin/project user access applies).

---

## Testing & CI

### Run unit & integration tests locally
From command line in project root:
```bash
mvn -DskipTests=false test
```
Or run individual test classes from IntelliJ's test runner.

### Admin endpoint to run `mvn test` on the server
- `POST /api/admin/tests/run-mvn` — admin-only endpoint that executes `mvn test` on the host. Use with caution.

### Recommended CI
- Add a GitHub Actions / GitLab CI pipeline to run `mvn test` and build the jar on push/PR.
- For integration tests use Docker Compose to spin up Postgres/MySQL containers and run tests against them.

---

## Production considerations & hardening (must-read)
- **SFTP host keys**: In production, **do not** use permissive host key verification. Always paste the `known_hosts` content in Admin UI for each project or provide a path to a managed known_hosts file.
- **Secrets management**: Do not store DB passwords or SFTP passwords in plain text in the database for production — integrate with AWS Secrets Manager, HashiCorp Vault, or similar.
- **IAM & Redshift**: Ensure Redshift cluster has IAM role permission to read from the staging S3 bucket for COPY operations.
- **SQL safety**: `BulkUpsertService` validates identifiers but ensure mappings are controlled and users cannot inject arbitrary SQL identifiers. Consider stricter server-side validation or whitelisting.
- **Scale & cleanup**: Configure retention in the UI for each customer (job history, artifacts). S3 cleanup is supported via `S3Service.deletePrefix` for simple cases; consider lifecycle rules in S3.
- **Run as service**: Package as jar and run under a process manager (systemd) or containerize with Docker and run in Kubernetes. Provide health checks and metrics endpoints for monitoring.

---

## Troubleshooting & logs
- Application logs are printed to stdout by default (Spring Boot). Check logs for stack traces for failures.
- DB connectivity: make sure the project's JDBC URL and credentials are correct and reachable from the app host.
- SFTP failures: check `known_hosts` content, host reachability (firewall), and credentials. Use the Admin UI to paste `known_hosts`.
- S3 failures: verify IAM credentials / permissions and S3 bucket name/region.

---