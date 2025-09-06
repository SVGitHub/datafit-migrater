-- ================================
-- Customers (tenants)
-- ================================
CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    domain VARCHAR(255), -- for SSO restriction
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- Projects (per-customer)
-- ================================
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    s3_bucket VARCHAR(255),
    retention_days INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- ================================
-- Users & Roles
-- ================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL, -- ADMIN, USER
    password VARCHAR(255),     -- optional if SSO enabled
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Project ↔ User Access (many-to-many)
CREATE TABLE project_users (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ================================
-- Jobs (ingestions)
-- ================================
CREATE TABLE jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    source_type VARCHAR(50),      -- LOCAL, SFTP, S3
    source_info TEXT,             -- JSON with details
    target_type VARCHAR(50),      -- Redshift, Postgres, MySQL
    target_schema VARCHAR(255),
    target_table VARCHAR(255),
    status VARCHAR(50),
    error_count INT DEFAULT 0,
    rows_processed BIGINT DEFAULT 0,
    rows_loaded BIGINT DEFAULT 0,
    error_file VARCHAR(500),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- ================================
-- Job Errors (row-level)
-- ================================
CREATE TABLE job_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    row_number BIGINT,
    error_message VARCHAR(1000),
    error_file VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id)
);

-- ================================
-- Mappings (per file pattern)
-- ================================
CREATE TABLE mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    file_pattern VARCHAR(255),     -- regex for filename
    mapping_json CLOB NOT NULL,    -- header → DB column, types, transforms
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- ================================
-- Options (configurable parameters)
-- ================================
CREATE TABLE options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    composite_key VARCHAR(255),
    chunk_size_mb INT DEFAULT 64,
    max_open_csv_writers INT DEFAULT 4,
    parallelism INT DEFAULT 1,
    mode VARCHAR(20) DEFAULT 'APPEND', -- APPEND or OVERWRITE
    row_error_limit INT DEFAULT 1000,
    aggregate_keys VARCHAR(500),
    retention_days INT DEFAULT 30,
    s3_folder VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);