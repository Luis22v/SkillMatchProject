SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS connections;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS saved_jobs;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS certifications;
DROP TABLE IF EXISTS educations;
DROP TABLE IF EXISTS experiences;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    phone             VARCHAR(20),
    headline          VARCHAR(200),
    location          VARCHAR(100),
    bio               VARCHAR(500),
    profile_image_url VARCHAR(255),
    cover_image_url   VARCHAR(255),
    enabled           BIT(1) NOT NULL DEFAULT 1,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE companies (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    industry     VARCHAR(100),
    size         VARCHAR(50),
    location     VARCHAR(100) NOT NULL,
    logo         VARCHAR(500),
    website      VARCHAR(200),
    email        VARCHAR(100) NOT NULL UNIQUE,
    phone        VARCHAR(20),
    founded_year INT,
    benefits     TEXT,
    is_verified  BIT(1) DEFAULT 0,
    active       BIT(1) NOT NULL DEFAULT 1,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skills (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(100) NOT NULL,
    level               VARCHAR(50),
    years_of_experience INT,
    description         TEXT,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME,
    CONSTRAINT fk_skill_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE experiences (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    company     VARCHAR(200) NOT NULL,
    position    VARCHAR(200) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE,
    is_current  BIT(1) DEFAULT 0,
    description TEXT,
    location    VARCHAR(100),
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME,
    CONSTRAINT fk_exp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE educations (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    school         VARCHAR(200) NOT NULL,
    degree         VARCHAR(200) NOT NULL,
    field_of_study VARCHAR(200),
    start_date     DATE NOT NULL,
    end_date       DATE,
    is_current     BIT(1) DEFAULT 0,
    description    TEXT,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME,
    CONSTRAINT fk_edu_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE certifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    issuer          VARCHAR(200),
    issue_date      DATE,
    expiration_date DATE,
    credential_id   VARCHAR(100),
    credential_url  VARCHAR(500),
    description     TEXT,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME,
    CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE jobs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id       BIGINT NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    type             VARCHAR(50) NOT NULL,
    experience_level VARCHAR(50),
    salary_min       DOUBLE,
    salary_max       DOUBLE,
    location         VARCHAR(100),
    modality         VARCHAR(50),
    duration         VARCHAR(100),
    requirements     TEXT,
    responsibilities TEXT,
    skills           TEXT,
    benefits         TEXT,
    status           VARCHAR(50) NOT NULL DEFAULT 'abierta',
    posted_date      DATETIME NOT NULL,
    expiration_date  DATETIME,
    active           BIT(1) NOT NULL DEFAULT 1,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME,
    CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE applications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    job_id        BIGINT NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'pendiente',
    resume        TEXT,
    cover_letter  TEXT,
    applied_date  DATETIME NOT NULL,
    reviewed_date DATETIME,
    notes         TEXT,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME,
    UNIQUE KEY uq_app_user_job (user_id, job_id),
    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_job  FOREIGN KEY (job_id)  REFERENCES jobs(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE saved_jobs (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    job_id   BIGINT NOT NULL,
    saved_at DATETIME NOT NULL,
    notes    TEXT,
    UNIQUE KEY uq_saved_user_job (user_id, job_id),
    CONSTRAINT fk_saved_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_job  FOREIGN KEY (job_id)  REFERENCES jobs(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE messages (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id           BIGINT NOT NULL,
    receiver_id         BIGINT NOT NULL,
    content             TEXT NOT NULL,
    is_read             BIT(1) NOT NULL DEFAULT 0,
    read_at             DATETIME,
    sent_at             DATETIME NOT NULL,
    attachment_url      VARCHAR(500),
    deleted_by_sender   BIT(1) NOT NULL DEFAULT 0,
    deleted_by_receiver BIT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_msg_sender   FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE connections (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    connected_user_id BIGINT NOT NULL,
    status            VARCHAR(50) NOT NULL DEFAULT 'pending',
    message           TEXT,
    requested_at      DATETIME NOT NULL,
    responded_at      DATETIME,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME,
    UNIQUE KEY uq_connection (user_id, connected_user_id),
    CONSTRAINT fk_conn_user   FOREIGN KEY (user_id)           REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conn_target FOREIGN KEY (connected_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       VARCHAR(50) NOT NULL,
    content    TEXT NOT NULL,
    related_id BIGINT,
    action_url VARCHAR(500),
    is_read    BIT(1) NOT NULL DEFAULT 0,
    read_at    DATETIME,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;