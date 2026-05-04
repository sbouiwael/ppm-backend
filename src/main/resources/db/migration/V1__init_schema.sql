-- ==============================================================
-- PPM BIAT — Initial Schema (Flyway V1)
-- Compatible with MySQL 8.0
-- Column names derived from JPA entity definitions (Hibernate snake_case)
-- ==============================================================

-- ── Work Calendars ────────────────────────────────────────────
-- Entity: WorkCalendar.java
CREATE TABLE IF NOT EXISTS work_calendars (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(500),
    work_hours_per_day  DOUBLE          NOT NULL DEFAULT 8.0,
    work_days           VARCHAR(255)    NOT NULL DEFAULT '1,2,3,4,5',
    created_at          DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_work_calendars_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Calendar Exceptions ───────────────────────────────────────
-- Entity: CalendarException.java
CREATE TABLE IF NOT EXISTS calendar_exceptions (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    calendar_id  BIGINT          NOT NULL,
    date         DATE            NOT NULL,
    name         VARCHAR(255)    NOT NULL,
    working      BOOLEAN         NOT NULL DEFAULT FALSE,
    work_hours   DOUBLE          NOT NULL DEFAULT 0.0,
    PRIMARY KEY (id),
    CONSTRAINT fk_cal_exc_calendar FOREIGN KEY (calendar_id) REFERENCES work_calendars (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Users ─────────────────────────────────────────────────────
-- Entity: User.java (firstName -> first_name, lastName -> last_name, weeklyCapacity -> weekly_capacity)
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    first_name       VARCHAR(255)    NOT NULL,
    last_name        VARCHAR(255)    NOT NULL,
    email            VARCHAR(255)    NOT NULL,
    password         VARCHAR(255)    NOT NULL,
    role             VARCHAR(50)     NOT NULL DEFAULT 'DEV',
    weekly_capacity  INT             NOT NULL DEFAULT 0,
    active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at       DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Portefeuilles (Portfolios) ────────────────────────────────
-- Entity: Portefeuille.java (field 'nom' -> column 'nom', no owner_id, no created_at)
CREATE TABLE IF NOT EXISTS portefeuilles (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    nom          VARCHAR(255)    NOT NULL,
    description  VARCHAR(2000),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Projects ──────────────────────────────────────────────────
-- Entity: Project.java (projectManager -> project_manager_id FK)
CREATE TABLE IF NOT EXISTS projects (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(255)    NOT NULL,
    description          VARCHAR(2000),
    start_date           DATE            NOT NULL,
    end_date             DATE,
    active               BOOLEAN         NOT NULL DEFAULT TRUE,
    progress             INT             NOT NULL DEFAULT 0,
    project_manager_id   BIGINT          NOT NULL,
    portefeuille_id      BIGINT,
    portfolio_name       VARCHAR(255),
    program_name         VARCHAR(255),
    sub_program_name     VARCHAR(255),
    objective            VARCHAR(255),
    calendar_name        VARCHAR(255),
    calendar_id          BIGINT,
    baseline_start_date  DATE,
    baseline_end_date    DATE,
    created_at           DATETIME        NOT NULL,
    updated_at           DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_manager      FOREIGN KEY (project_manager_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_portefeuille FOREIGN KEY (portefeuille_id)    REFERENCES portefeuilles (id) ON DELETE SET NULL,
    CONSTRAINT fk_project_calendar     FOREIGN KEY (calendar_id)        REFERENCES work_calendars (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Tasks ─────────────────────────────────────────────────────
-- Entity: Task.java
CREATE TABLE IF NOT EXISTS tasks (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(2000),
    project_id              BIGINT          NOT NULL,
    parent_task_id          BIGINT,
    wbs_number              VARCHAR(255)    NOT NULL DEFAULT '1',
    mode                    VARCHAR(50)     NOT NULL DEFAULT 'TASK',
    duration_days           DOUBLE          NOT NULL DEFAULT 1.0,
    work_hours              DOUBLE          NOT NULL DEFAULT 8.0,
    baseline_duration_days  DOUBLE,
    baseline_start_date     DATE,
    baseline_end_date       DATE,
    actual_work_hours       DOUBLE,
    calendar_name           VARCHAR(255),
    sort_order              INT             NOT NULL DEFAULT 0,
    start_date              DATE,
    end_date                DATE,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'NOT_STARTED',
    progress                INT             NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              DATETIME        NOT NULL,
    updated_at              DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id)     REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_parent  FOREIGN KEY (parent_task_id) REFERENCES tasks (id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Task Assignments ──────────────────────────────────────────
-- Entity: TaskAssignment.java (added created_at)
CREATE TABLE IF NOT EXISTS task_assignments (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    task_id         BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    assigned_hours  INT         NOT NULL DEFAULT 0,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_assignment_task_user (task_id, user_id),
    CONSTRAINT fk_assignment_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Task Dependencies ─────────────────────────────────────────
-- Entity: TaskDependency.java (predecessor_task_id, successor_task_id, type column, created_at)
CREATE TABLE IF NOT EXISTS task_dependencies (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    predecessor_task_id  BIGINT      NOT NULL,
    successor_task_id    BIGINT      NOT NULL,
    type                 VARCHAR(50) NOT NULL DEFAULT 'FS',
    created_at           DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dependency_pair (predecessor_task_id, successor_task_id),
    CONSTRAINT fk_dep_predecessor FOREIGN KEY (predecessor_task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_successor   FOREIGN KEY (successor_task_id)   REFERENCES tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;