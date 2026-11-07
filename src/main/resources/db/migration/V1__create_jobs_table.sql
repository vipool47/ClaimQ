CREATE TABLE jobs (
                      id            BIGSERIAL PRIMARY KEY,
                      task_type     VARCHAR(100) NOT NULL,
                      payload       JSONB        NOT NULL,
                      status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                      attempts      INTEGER      NOT NULL DEFAULT 0,
                      max_attempts  INTEGER      NOT NULL DEFAULT 3,
                      scheduled_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      locked_by     VARCHAR(100),
                      locked_at     TIMESTAMPTZ,
                      claim_token   UUID,
                      last_error    TEXT,
                      created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

                      CONSTRAINT chk_jobs_attempts_non_negative
                          CHECK (attempts >= 0),
                      CONSTRAINT chk_jobs_max_attempts_positive
                          CHECK (max_attempts > 0),
                      CONSTRAINT chk_jobs_attempts_within_max
                          CHECK (attempts <= max_attempts),
                      CONSTRAINT chk_jobs_status
                          CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
                      CONSTRAINT chk_jobs_claim_consistency
                          CHECK (
                              (status = 'PROCESSING'
                                  AND locked_by IS NOT NULL
                                  AND locked_at IS NOT NULL
                                  AND claim_token IS NOT NULL)
                                  OR
                              (status <> 'PROCESSING'
                                  AND locked_by IS NULL
                                  AND locked_at IS NULL
                                  AND claim_token IS NULL)
                              )
);

CREATE INDEX idx_jobs_pending
    ON jobs (scheduled_at, id)
    WHERE status = 'PENDING';