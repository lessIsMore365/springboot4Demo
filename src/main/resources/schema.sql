-- 创建用户表 sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    age INTEGER,
    password VARCHAR(255),
    roles VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    last_login_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    remark TEXT
);

-- 创建唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_sys_user_email ON sys_user(email);

-- =============================================
-- RBAC (Role-Based Access Control) 权限管理表
-- =============================================

-- 创建角色表 sys_role
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0
);

-- 角色表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_code ON sys_role(code);
CREATE INDEX IF NOT EXISTS idx_sys_role_name ON sys_role(name);

-- 创建权限表 sys_permission
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    url VARCHAR(500),
    method VARCHAR(10),
    parent_id BIGINT,
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE
);

-- 权限表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_permission_code ON sys_permission(code);
CREATE INDEX IF NOT EXISTS idx_sys_permission_type ON sys_permission(type);
CREATE INDEX IF NOT EXISTS idx_sys_permission_parent_id ON sys_permission(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_permission_url_method ON sys_permission(url, method);

-- 创建用户角色关联表 sys_user_role
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

-- 用户角色关联表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_role_unique ON sys_user_role(user_id, role_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role(role_id);

-- 创建角色权限关联表 sys_role_permission
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

-- 角色权限关联表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_permission_unique ON sys_role_permission(role_id, permission_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_role_permission_role_id ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_permission_permission_id ON sys_role_permission(permission_id);