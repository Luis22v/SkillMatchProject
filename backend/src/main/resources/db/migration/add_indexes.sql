-- Indexes for SkillMatch — run manually against MySQL (ddl-auto=validate)
-- Safe to re-run: each statement uses CREATE INDEX IF NOT EXISTS

CREATE INDEX IF NOT EXISTS idx_application_user_id    ON applications (user_id);
CREATE INDEX IF NOT EXISTS idx_application_job_id     ON applications (job_id);
CREATE INDEX IF NOT EXISTS idx_application_status     ON applications (status);
CREATE INDEX IF NOT EXISTS idx_application_created_at ON applications (created_at);

CREATE INDEX IF NOT EXISTS idx_job_company_id         ON jobs (company_id);
CREATE INDEX IF NOT EXISTS idx_job_status             ON jobs (status);
CREATE INDEX IF NOT EXISTS idx_job_active             ON jobs (active);
CREATE INDEX IF NOT EXISTS idx_job_created_at         ON jobs (created_at);

CREATE INDEX IF NOT EXISTS idx_notification_user_id    ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read    ON notifications (is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications (created_at);

CREATE INDEX IF NOT EXISTS idx_saved_job_user_id      ON saved_jobs (user_id);
CREATE INDEX IF NOT EXISTS idx_saved_job_job_id       ON saved_jobs (job_id);
