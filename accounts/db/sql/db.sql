CREATE SCHEMA account_service;
CREATE SCHEMA booking_service;
CREATE SCHEMA payment_service;
CREATE SCHEMA notification_service;
SET search_path TO account_service;

-- =========================================
-- USERS
-- =========================================

CREATE TABLE account_service.users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_phone_verified BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    registration_ip INET,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =========================================
-- PASSENGERS
-- Saved passenger profiles
-- =========================================

CREATE TABLE account_service.passengers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES account_service.users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- =========================================
-- ROLES
-- =========================================

CREATE TABLE account_service.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =========================================
-- USER ROLES (ACCESS TABLE)
-- =========================================


CREATE TABLE account_service.user_roles (
    user_id UUID NOT NULL REFERENCES account_service.users(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES account_service.roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY(user_id, role_id)
);


-- =========================================
-- SESSIONS
-- Stores refresh tokens
-- =========================================

CREATE TABLE account_service.sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES account_service.users(id) ON DELETE CASCADE,
    refresh_token TEXT NOT NULL,
    device_info TEXT,
    ip_address VARCHAR(100),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- =========================================
-- INDEXES
-- =========================================

CREATE INDEX idx_users_phone ON account_service.users(phone);
CREATE INDEX idx_users_email ON account_service.users(email);
CREATE INDEX idx_passengers_user_id ON account_service.passengers(user_id);
CREATE INDEX idx_sessions_user_id ON account_service.sessions(user_id);

-- =========================================
-- DEFAULT ROLES
-- =========================================

INSERT INTO account_service.roles(name)
VALUES
('CUSTOMER'),
('ADMIN'),
('SUPPORT_AGENT');