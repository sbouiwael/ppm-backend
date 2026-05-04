-- ==============================================================
-- PPM BIAT — V3: Add performance indexes
-- InnoDB auto-creates single-column indexes for FK columns.
-- These composite indexes target the most frequent multi-column
-- WHERE clauses observed in Gantt, dashboard and WBS queries.
-- ==============================================================

-- Tasks filtered by project + active flag (Gantt, WBS, task-list)
CREATE INDEX idx_tasks_project_active ON tasks (project_id, active);

-- Tasks filtered by parent (WBS hierarchy traversal)
CREATE INDEX idx_tasks_parent_active ON tasks (parent_task_id, active);

-- Projects filtered by active flag (dashboard active-project count)
CREATE INDEX idx_projects_active ON projects (active);

-- Assignments: lookup all active assignments for a user (dashboard workload, capacity)
CREATE INDEX idx_assignments_user_active ON task_assignments (user_id, active);
