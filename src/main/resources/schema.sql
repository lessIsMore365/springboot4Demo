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
    dept_id BIGINT,
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
    sort_order INTEGER DEFAULT 0,
    data_scope VARCHAR(2) DEFAULT '1'
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
-- 菜单管理表
-- =============================================

-- 创建菜单表 sys_menu
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(255),
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    menu_type CHAR(1) DEFAULT 'C',
    permission VARCHAR(100),
    visible INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_sort_order ON sys_menu(sort_order);

-- 创建角色菜单关联表 sys_role_menu
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_menu_unique ON sys_role_menu(role_id, menu_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);

-- =============================================
-- 部门管理表
-- =============================================

-- 创建部门表 sys_dept
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_sys_dept_parent_id ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_dept_sort_order ON sys_dept(sort_order);

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
    biz_type VARCHAR(50),
    remark VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    trade_no VARCHAR(64),
    buyer_id VARCHAR(64),
    paid_time TIMESTAMP,
    refund_amount DECIMAL(10, 2) DEFAULT 0.00,
    notify_data TEXT,
    pay_data TEXT,
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
CREATE INDEX IF NOT EXISTS idx_payment_order_biz_type ON payment_order(biz_type);

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

-- =============================================
-- 支付回调通知日志表 payment_notify_log
-- =============================================

CREATE TABLE IF NOT EXISTS payment_notify_log (
    id BIGINT PRIMARY KEY,
    payment_method VARCHAR(20) NOT NULL,
    order_no VARCHAR(64),
    notify_body TEXT,
    signature_valid BOOLEAN DEFAULT FALSE,
    process_status VARCHAR(20) DEFAULT 'RECEIVED',
    error_msg VARCHAR(500),
    ip_address VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notify_log_order_no ON payment_notify_log(order_no);
CREATE INDEX IF NOT EXISTS idx_notify_log_method_status ON payment_notify_log(payment_method, process_status);
CREATE INDEX IF NOT EXISTS idx_notify_log_create_time ON payment_notify_log(create_time);

-- =============================================
-- 操作日志表 sys_oper_log
-- =============================================

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    business_type VARCHAR(20),
    method VARCHAR(200),
    request_method VARCHAR(10),
    operator_type VARCHAR(10),
    oper_name VARCHAR(100),
    oper_url VARCHAR(500),
    oper_ip VARCHAR(50),
    oper_location VARCHAR(100),
    oper_param TEXT,
    json_result TEXT,
    status INTEGER DEFAULT 0,
    error_msg VARCHAR(500),
    cost_time BIGINT DEFAULT 0,
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_oper_log_oper_name ON sys_oper_log(oper_name);
CREATE INDEX IF NOT EXISTS idx_oper_log_business_type ON sys_oper_log(business_type);
CREATE INDEX IF NOT EXISTS idx_oper_log_status ON sys_oper_log(status);
CREATE INDEX IF NOT EXISTS idx_oper_log_create_time ON sys_oper_log(create_time);

-- =============================================
-- 字典管理表
-- =============================================

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY,
    dict_name VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL,
    status VARCHAR(2) DEFAULT '0',
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dict_type ON sys_dict_type(dict_type);

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY,
    dict_type VARCHAR(100) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    dict_sort INTEGER DEFAULT 0,
    css_class VARCHAR(50),
    list_class VARCHAR(50),
    is_default VARCHAR(2) DEFAULT '0',
    status VARCHAR(2) DEFAULT '0',
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dict_data_type ON sys_dict_data(dict_type);
CREATE INDEX IF NOT EXISTS idx_dict_data_sort ON sys_dict_data(dict_sort);

-- =============================================
-- AI 模块 (AI Module) 表
-- =============================================

-- AI API 用量统计表
CREATE TABLE IF NOT EXISTS ai_api_usage (
    id BIGINT PRIMARY KEY,
    model VARCHAR(50) NOT NULL,
    endpoint VARCHAR(100),
    prompt_tokens INTEGER DEFAULT 0,
    completion_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    cost DECIMAL(10, 6) DEFAULT 0.000000,
    latency_ms INTEGER DEFAULT 0,
    user_id BIGINT,
    username VARCHAR(100),
    success BOOLEAN DEFAULT TRUE,
    error_msg VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_api_usage_model ON ai_api_usage(model);
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_create_time ON ai_api_usage(create_time);
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_username ON ai_api_usage(username);

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    username VARCHAR(100),
    title VARCHAR(200),
    model VARCHAR(50) NOT NULL,
    message_count INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    total_cost DECIMAL(10, 6) DEFAULT 0.000000,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_chat_session_id ON ai_chat_session(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_session_user_id ON ai_chat_session(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_session_create_time ON ai_chat_session(create_time);

-- AI 对话历史表
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_calls TEXT,
    tool_call_id VARCHAR(64),
    token_count INTEGER DEFAULT 0,
    seq INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_history_session_id ON ai_chat_history(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_history_seq ON ai_chat_history(session_id, seq);

-- AI 知识库表
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    doc_count INTEGER DEFAULT 0,
    chunk_count INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

-- AI 知识库文档表
CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
    id BIGINT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    file_name VARCHAR(300) NOT NULL,
    file_type VARCHAR(20),
    file_size BIGINT DEFAULT 0,
    content_text TEXT,
    chunk_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_doc_kb_id ON ai_knowledge_doc(kb_id);

-- AI 知识库分块表
CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    chunk_index INTEGER DEFAULT 0,
    content TEXT NOT NULL,
    embedding JSON,
    token_count INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_doc_id ON ai_knowledge_chunk(doc_id);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_kb_id ON ai_knowledge_chunk(kb_id);

-- AI 模型提供商配置表
CREATE TABLE IF NOT EXISTS ai_provider_config (
    id BIGINT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    display_name VARCHAR(50),
    api_key VARCHAR(200),
    base_url VARCHAR(300),
    model VARCHAR(100),
    max_tokens INTEGER DEFAULT 4096,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    cost_per_million_tokens DECIMAL(10,2) DEFAULT 1.0,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_provider_config_name ON ai_provider_config(name);

-- 创建支付配置表 payment_config（Web 端实时配置生效）
CREATE TABLE IF NOT EXISTS payment_config (
    id BIGINT PRIMARY KEY,
    payment_method VARCHAR(10) NOT NULL,
    app_id VARCHAR(100),
    gateway_url VARCHAR(300),
    notify_url VARCHAR(300),
    sign_type VARCHAR(10) DEFAULT 'RSA2',
    private_key TEXT,
    alipay_public_key TEXT,
    return_url VARCHAR(300),
    mch_id VARCHAR(50),
    api_v3_key VARCHAR(100),
    mch_serial_no VARCHAR(100),
    private_key_path VARCHAR(300),
    order_expire_minutes INTEGER DEFAULT 15,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_config_method ON payment_config(payment_method);

-- =============================================
-- 兼容已有数据库的 ALTER TABLE 语句
-- 当表已存在时，CREATE TABLE IF NOT EXISTS 不会添加新列，以下语句补充新增列
-- =============================================

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS data_scope VARCHAR(2) DEFAULT '1';
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS default_role_id BIGINT;

-- =============================================
-- Phase 1-4 支付模块新增列
-- =============================================
ALTER TABLE payment_config ADD COLUMN IF NOT EXISTS wechat_platform_cert TEXT;
ALTER TABLE payment_config ADD COLUMN IF NOT EXISTS wechat_platform_cert_serial VARCHAR(100);
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'CNY';

-- =============================================
-- Phase 3.1: 支付事件日志表 payment_event_log
-- =============================================
CREATE TABLE IF NOT EXISTS payment_event_log (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    operator VARCHAR(100) DEFAULT 'SYSTEM',
    operator_ip VARCHAR(50),
    event_data TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_log_order_no ON payment_event_log(order_no);
CREATE INDEX IF NOT EXISTS idx_event_log_event_type ON payment_event_log(event_type);
CREATE INDEX IF NOT EXISTS idx_event_log_create_time ON payment_event_log(create_time);

-- =============================================
-- Phase 4.1: 退款记录表 refund_record
-- =============================================
CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    refund_trade_no VARCHAR(64) NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    remote_refund_no VARCHAR(64),
    operator VARCHAR(100),
    operator_ip VARCHAR(50),
    error_msg VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refund_record_order_no ON refund_record(order_no);
CREATE INDEX IF NOT EXISTS idx_refund_record_status ON refund_record(status);
CREATE INDEX IF NOT EXISTS idx_refund_record_create_time ON refund_record(create_time);

-- =============================================
-- Phase 4.3: 支付 Webhook 注册表 payment_webhook
-- =============================================
CREATE TABLE IF NOT EXISTS payment_webhook (
    id BIGINT PRIMARY KEY,
    webhook_url VARCHAR(500) NOT NULL,
    event_types VARCHAR(500),
    secret VARCHAR(200),
    enabled BOOLEAN DEFAULT TRUE,
    max_retries INTEGER DEFAULT 3,
    retry_count INTEGER DEFAULT 0,
    last_status VARCHAR(20),
    last_called_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);