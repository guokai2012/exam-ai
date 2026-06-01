CREATE TABLE exam_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(16) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 VARCHAR(128) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    extracted_text MEDIUMTEXT NULL,
    status VARCHAR(32) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_document_uploaded_by (uploaded_by),
    KEY idx_document_status (status),
    KEY idx_document_sha256 (sha256),
    CONSTRAINT fk_document_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_document_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    model_name VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    raw_json MEDIUMTEXT NULL,
    error_message VARCHAR(1024) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_analysis_document_id (document_id),
    KEY idx_analysis_created_by (created_by),
    CONSTRAINT fk_analysis_document FOREIGN KEY (document_id) REFERENCES exam_document (id),
    CONSTRAINT fk_analysis_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_document_analysis_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text MEDIUMTEXT NOT NULL,
    chunk_hash VARCHAR(128) NOT NULL,
    start_offset INT NOT NULL,
    end_offset INT NOT NULL,
    question_count_estimate INT NOT NULL,
    oversized TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    raw_json MEDIUMTEXT NULL,
    error_message VARCHAR(1024) NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_analysis_chunk_index (analysis_id, chunk_index),
    KEY idx_chunk_document_status (document_id, status),
    KEY idx_chunk_hash (chunk_hash),
    CONSTRAINT fk_chunk_analysis FOREIGN KEY (analysis_id) REFERENCES exam_document_analysis (id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES exam_document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_question_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_category_name (category_name),
    KEY idx_question_category_status (status),
    CONSTRAINT fk_question_category_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_question_bank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    normalized_stem TEXT NOT NULL,
    stem_hash VARCHAR(128) NOT NULL,
    options_json TEXT NULL,
    standard_answer TEXT NOT NULL,
    explanation TEXT NULL,
    difficulty_stars TINYINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_reason VARCHAR(512) NULL,
    tag_error_message VARCHAR(1024) NULL,
    tag_retry_count INT NOT NULL DEFAULT 0,
    tag_notified_at DATETIME NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_owner_category_stem_hash (created_by, category_id, stem_hash),
    KEY idx_question_category_id (category_id),
    KEY idx_question_type (question_type),
    KEY idx_question_state (state),
    CONSTRAINT fk_question_category FOREIGN KEY (category_id) REFERENCES exam_question_category (id),
    CONSTRAINT fk_question_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES sys_user (id),
    CONSTRAINT fk_question_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_question_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_tag_name (tag_name),
    KEY idx_question_tag_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_question_tag_relation (
    question_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, tag_id),
    CONSTRAINT fk_tag_relation_question FOREIGN KEY (question_id) REFERENCES exam_question_bank (id),
    CONSTRAINT fk_tag_relation_tag FOREIGN KEY (tag_id) REFERENCES exam_question_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_question_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    analysis_id BIGINT NOT NULL,
    chunk_id BIGINT NULL,
    confidence DECIMAL(5,4) NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_question_source_question_id (question_id),
    KEY idx_question_source_document_id (document_id),
    KEY idx_question_source_analysis_id (analysis_id),
    KEY idx_question_source_chunk_id (chunk_id),
    CONSTRAINT fk_question_source_question FOREIGN KEY (question_id) REFERENCES exam_question_bank (id),
    CONSTRAINT fk_question_source_document FOREIGN KEY (document_id) REFERENCES exam_document (id),
    CONSTRAINT fk_question_source_analysis FOREIGN KEY (analysis_id) REFERENCES exam_document_analysis (id),
    CONSTRAINT fk_question_source_chunk FOREIGN KEY (chunk_id) REFERENCES exam_document_analysis_chunk (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value VARCHAR(512) NOT NULL,
    config_name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    value_type VARCHAR(32) NOT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_config_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1024) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NULL,
    business_id BIGINT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notification_recipient (recipient_id, read_at, created_at),
    KEY idx_notification_business (business_type, business_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_config (config_key, config_value, config_name, description, value_type)
VALUES ('ai.tagging.max-retries', '3', 'AI 标签最大重试次数', '题目 AI 标签首次失败后允许继续重试的次数', 'INTEGER');

INSERT INTO sys_config (config_key, config_value, config_name, description, value_type)
VALUES ('ai.document-analysis.max-retries', '1', '文档 AI 解析最大重试次数', '文档 AI 解析首次失败后允许继续重试的次数，建议不超过 3 次', 'INTEGER');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:upload', '上传文档'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:upload');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:analyze', '分析文档'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:analyze');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:list', '查询文档列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:list');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:detail', '查询文档详情'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:detail');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:content', '查询文档解析文本'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:content');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'document:analysis-latest', '查询最新分析结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document:analysis-latest');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'system-config:page', '系统配置页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system-config:page');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'system-config:list', '查询系统配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system-config:list');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'system-config:update', '更新系统配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system-config:update');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'notification:page', '站内通知页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:page');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'notification:list', '查询系统通知'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:list');

INSERT INTO sys_permission (permission_code, permission_name)
SELECT 'notification:mark-read', '标记通知已读'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:mark-read');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'document:upload', 'document:list', 'document:detail',
    'document:content', 'document:analyze', 'document:analysis-latest'
)
WHERE r.role_code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'system-config:page', 'system-config:list', 'system-config:update',
    'notification:page', 'notification:list', 'notification:mark-read'
)
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('notification:page', 'notification:list', 'notification:mark-read')
WHERE r.role_code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
