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
    version INTEGER DEFAULT 1,
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
    version INTEGER DEFAULT 1,
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
    version INTEGER DEFAULT 1,
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
    version INTEGER DEFAULT 1
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
    version INTEGER DEFAULT 1
);

-- 角色权限关联表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_permission_unique ON sys_role_permission(role_id, permission_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_role_permission_role_id ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_permission_permission_id ON sys_role_permission(permission_id);

-- =============================================
-- 支付模块 (Payment Module) 表
-- =============================================

-- 创建支付订单表 payment_order
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    subject VARCHAR(256),
    body VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    trade_no VARCHAR(64),
    buyer_id VARCHAR(64),
    paid_time TIMESTAMP,
    refund_amount DECIMAL(10, 2) DEFAULT 0.00,
    notify_data TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_order_no ON payment_order(order_no);
CREATE INDEX IF NOT EXISTS idx_payment_order_trade_no ON payment_order(trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_order_method_status ON payment_order(payment_method, status);
CREATE INDEX IF NOT EXISTS idx_payment_order_create_time ON payment_order(create_time);
CREATE INDEX IF NOT EXISTS idx_payment_order_paid_time ON payment_order(paid_time);

-- 创建对帐记录表 reconciliation_record
CREATE TABLE IF NOT EXISTS reconciliation_record (
    id BIGINT PRIMARY KEY,
    recon_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    local_total_amount DECIMAL(12, 2) DEFAULT 0.00,
    remote_total_amount DECIMAL(12, 2) DEFAULT 0.00,
    local_count INTEGER DEFAULT 0,
    remote_count INTEGER DEFAULT 0,
    diff_amount DECIMAL(12, 2) DEFAULT 0.00,
    diff_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_recon_record_date_method ON reconciliation_record(recon_date, payment_method) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_recon_record_date ON reconciliation_record(recon_date);
CREATE INDEX IF NOT EXISTS idx_recon_record_status ON reconciliation_record(status);

-- 创建对帐明细表 reconciliation_detail
CREATE TABLE IF NOT EXISTS reconciliation_detail (
    id BIGINT PRIMARY KEY,
    recon_record_id BIGINT NOT NULL,
    recon_date DATE NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    trade_no VARCHAR(64),
    local_amount DECIMAL(10, 2) DEFAULT 0.00,
    remote_amount DECIMAL(10, 2) DEFAULT 0.00,
    local_status VARCHAR(20),
    remote_status VARCHAR(20),
    diff_type VARCHAR(20) NOT NULL,
    diff_desc TEXT,
    create_time TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recon_detail_record_id ON reconciliation_detail(recon_record_id);
CREATE INDEX IF NOT EXISTS idx_recon_detail_date ON reconciliation_detail(recon_date);
CREATE INDEX IF NOT EXISTS idx_recon_detail_diff_type ON reconciliation_detail(diff_type);
CREATE INDEX IF NOT EXISTS idx_recon_detail_order_no ON reconciliation_detail(order_no);