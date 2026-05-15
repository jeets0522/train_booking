-- =========================================
-- SCHEMAS
-- =========================================
CREATE SCHEMA IF NOT EXISTS account_service;
CREATE SCHEMA IF NOT EXISTS booking_service;
CREATE SCHEMA IF NOT EXISTS payment_service;
CREATE SCHEMA IF NOT EXISTS notification_service;

-- =========================================
-- SEARCH PATH
-- =========================================
SET search_path = account_service, public;

-- =========================================
-- COMMON TRIGGER FUNCTION FOR UPDATED_AT COLUMN 
-- =========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- USERS
-- =========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),

    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),

    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    lock_time TIMESTAMPTZ,

    last_login_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Case-insensitive lookups
CREATE UNIQUE INDEX idx_users_email_lower
    ON users (LOWER(email));

CREATE UNIQUE INDEX idx_users_username_lower
    ON users (LOWER(username));

CREATE INDEX idx_users_uuid
    ON users (uuid);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- ROLES
-- =========================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default roles
INSERT INTO roles (name, description)
VALUES
    ('ROLE_USER', 'Standard user'),
    ('ROLE_ADMIN', 'Administrator'),
    ('ROLE_MANAGER', 'Manager');

-- =========================================
-- PERMISSIONS
-- =========================================
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- =========================================
-- ROLE PERMISSIONS
-- =========================================
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL
        REFERENCES roles(id) ON DELETE CASCADE,

    permission_id BIGINT NOT NULL
        REFERENCES permissions(id) ON DELETE CASCADE,

    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id
    ON role_permissions(role_id);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions(permission_id);

-- =========================================
-- USER ROLES
-- =========================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    role_id BIGINT NOT NULL
        REFERENCES roles(id) ON DELETE CASCADE,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id
    ON user_roles(user_id);

CREATE INDEX idx_user_roles_role_id
    ON user_roles(role_id);

-- =========================================
-- REFRESH TOKENS
-- Stores refresh sessions/tokens
-- =========================================
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,

    token_hash VARCHAR(255) UNIQUE NOT NULL,

    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    device_info VARCHAR(255),
    ip_address INET,
    user_agent VARCHAR(500),

    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);

CREATE INDEX idx_refresh_tokens_active_hash
    ON refresh_tokens(token_hash)
    WHERE revoked_at IS NULL;

-- =========================================
-- VERIFICATION TOKENS
-- =========================================
CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,

    token_hash VARCHAR(255) UNIQUE NOT NULL,

    user_id BIGINT NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    token_type VARCHAR(50) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,

    ip_address INET,
    user_agent VARCHAR(500),

    metadata JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_verification_tokens_token_type
        CHECK (
            token_type IN (
                'EMAIL_VERIFICATION',
                'PASSWORD_RESET',
                'PHONE_VERIFICATION',
                'EMAIL_CHANGE',
                'MAGIC_LINK'
            )
        )
);

CREATE INDEX idx_verification_tokens_user_type
    ON verification_tokens(user_id, token_type)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_verification_tokens_expires_at
    ON verification_tokens(expires_at);