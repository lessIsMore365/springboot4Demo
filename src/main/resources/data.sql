-- =============================================
-- 初始部门数据
-- =============================================

INSERT INTO sys_dept (id, parent_id, name, sort_order, leader, phone, email, status, create_time, update_time, version, deleted) VALUES
(10, 0, '总公司', 1, '张总', '13800000000', 'ceo@example.com', 0, NOW(), NOW(), 1, 0),
(11, 10, '技术部', 1, '李经理', '13800000001', 'tech@example.com', 0, NOW(), NOW(), 1, 0),
(12, 10, '市场部', 2, '王经理', '13800000002', 'market@example.com', 0, NOW(), NOW(), 1, 0),
(13, 10, '财务部', 3, '赵经理', '13800000003', 'finance@example.com', 0, NOW(), NOW(), 1, 0),
(14, 11, '研发组', 1, '陈组长', '13800000004', 'dev@example.com', 0, NOW(), NOW(), 1, 0),
(15, 11, '测试组', 2, '刘组长', '13800000005', 'qa@example.com', 0, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 初始角色和权限数据
-- 注意：这些ID是示例值，实际使用中可能会使用雪花算法生成
-- =============================================

-- 插入初始角色
INSERT INTO sys_role (id, name, code, description, enabled, sort_order, data_scope, create_time, update_time, version, deleted) VALUES
(1, '管理员', 'ROLE_ADMIN', '系统管理员，拥有所有权限', TRUE, 1, '1', NOW(), NOW(), 1, 0),
(2, '普通用户', 'ROLE_USER', '普通用户，拥有基本权限', TRUE, 2, '3', NOW(), NOW(), 1, 0),
-- 部门默认角色（多角色叠加演示）
(3, '技术部默认角色', 'ROLE_DEPT_TECH', '技术部成员默认角色，可查看支付与监控', TRUE, 10, '3', NOW(), NOW(), 1, 0),
(4, '市场部默认角色', 'ROLE_DEPT_MARKET', '市场部成员默认角色，可查看支付', TRUE, 11, '3', NOW(), NOW(), 1, 0),
(5, '财务部默认角色', 'ROLE_DEPT_FINANCE', '财务部成员默认角色，可查看支付/对帐/监控', TRUE, 12, '3', NOW(), NOW(), 1, 0),
-- 特殊额外角色（叠加在部门角色之上）
(6, '项目组长', 'ROLE_PROJECT_LEAD', '项目组长额外权限：创建用户、分配角色', TRUE, 20, '3', NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 插入初始权限
INSERT INTO sys_permission (id, name, code, type, description, url, method, enabled, sort_order, create_time, update_time, version, deleted) VALUES
-- 用户管理权限
(101, '查看用户列表', 'user:read', 'API', '查看用户列表权限', '/api/users', 'GET', TRUE, 1, NOW(), NOW(), 1, 0),
(102, '创建用户', 'user:create', 'API', '创建用户权限', '/api/users', 'POST', TRUE, 2, NOW(), NOW(), 1, 0),
(103, '更新用户', 'user:update', 'API', '更新用户权限', '/api/users/*', 'PUT', TRUE, 3, NOW(), NOW(), 1, 0),
(104, '删除用户', 'user:delete', 'API', '删除用户权限', '/api/users/*', 'DELETE', TRUE, 4, NOW(), NOW(), 1, 0),

-- 角色管理权限
(201, '查看角色列表', 'role:read', 'API', '查看角色列表权限', '/api/roles', 'GET', TRUE, 11, NOW(), NOW(), 1, 0),
(202, '创建角色', 'role:create', 'API', '创建角色权限', '/api/roles', 'POST', TRUE, 12, NOW(), NOW(), 1, 0),
(203, '更新角色', 'role:update', 'API', '更新角色权限', '/api/roles/*', 'PUT', TRUE, 13, NOW(), NOW(), 1, 0),
(204, '删除角色', 'role:delete', 'API', '删除角色权限', '/api/roles/*', 'DELETE', TRUE, 14, NOW(), NOW(), 1, 0),

-- 权限管理权限
(301, '查看权限列表', 'permission:read', 'API', '查看权限列表权限', '/api/permissions', 'GET', TRUE, 21, NOW(), NOW(), 1, 0),
(302, '创建权限', 'permission:create', 'API', '创建权限权限', '/api/permissions', 'POST', TRUE, 22, NOW(), NOW(), 1, 0),
(303, '更新权限', 'permission:update', 'API', '更新权限权限', '/api/permissions/*', 'PUT', TRUE, 23, NOW(), NOW(), 1, 0),
(304, '删除权限', 'permission:delete', 'API', '删除权限权限', '/api/permissions/*', 'DELETE', TRUE, 24, NOW(), NOW(), 1, 0),

-- 分配权限
(401, '分配用户角色', 'user:assign-role', 'API', '为用户分配角色权限', '/api/roles/assign', 'POST', TRUE, 31, NOW(), NOW(), 1, 0),
(402, '分配角色权限', 'role:assign-permission', 'API', '为角色分配权限权限', '/api/roles/permissions/assign', 'POST', TRUE, 32, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 为管理员角色分配所有权限
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
-- 管理员拥有所有权限
(10000, 1, 101, NOW(), NOW(), 1, 0),
(10001, 1, 102, NOW(), NOW(), 1, 0),
(10002, 1, 103, NOW(), NOW(), 1, 0),
(10003, 1, 104, NOW(), NOW(), 1, 0),
(10004, 1, 201, NOW(), NOW(), 1, 0),
(10005, 1, 202, NOW(), NOW(), 1, 0),
(10006, 1, 203, NOW(), NOW(), 1, 0),
(10007, 1, 204, NOW(), NOW(), 1, 0),
(10008, 1, 301, NOW(), NOW(), 1, 0),
(10009, 1, 302, NOW(), NOW(), 1, 0),
(10010, 1, 303, NOW(), NOW(), 1, 0),
(10011, 1, 304, NOW(), NOW(), 1, 0),
(10012, 1, 401, NOW(), NOW(), 1, 0),
(10013, 1, 402, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 为普通用户角色分配基本权限（查看用户列表、查看角色列表、查看权限列表）
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
(10014, 2, 101, NOW(), NOW(), 1, 0),
(10015, 2, 201, NOW(), NOW(), 1, 0),
(10016, 2, 301, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 部门默认角色权限分配
-- ROLE_DEPT_TECH(3): 查看用户/角色/权限（与技术相关的管理查看）
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
(10017, 3, 101, NOW(), NOW(), 1, 0),
(10018, 3, 201, NOW(), NOW(), 1, 0),
(10019, 3, 301, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ROLE_DEPT_MARKET(4): 仅查看用户列表（市场人员只需知道有哪些用户）
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
(10020, 4, 101, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ROLE_DEPT_FINANCE(5): 查看用户/角色/权限（财务需要了解系统结构）
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
(10021, 5, 101, NOW(), NOW(), 1, 0),
(10022, 5, 201, NOW(), NOW(), 1, 0),
(10023, 5, 301, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ROLE_PROJECT_LEAD(6): 特殊额外权限 — 创建用户 + 分配角色
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, update_time, version, deleted) VALUES
(10024, 6, 102, NOW(), NOW(), 1, 0),
(10025, 6, 401, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 创建一个默认管理员用户（用户名：admin，密码：admin123）
-- 注意：密码需要使用BCrypt加密，实际值应为加密后的密码
-- 你可以使用以下代码生成BCrypt密码：
--   new BCryptPasswordEncoder().encode("admin123")
-- 生成的密码类似于：$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-- 这里使用一个示例加密密码（实际部署时请重新生成）
-- 创建一个默认管理员用户（用户名：admin，密码：password）
-- BCrypt 哈希值：password (强度 10)
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1000, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@example.com', 30, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 10, 1, 0, '系统管理员')
ON CONFLICT (id) DO NOTHING;

-- 创建一个普通用户（用户名：user，密码：password）
-- BCrypt 哈希值：password (强度 10)
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1001, 'user', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'user@example.com', 25, 'ROLE_USER', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 14, 1, 0, '普通用户')
ON CONFLICT (id) DO NOTHING;

-- 为用户分配角色
INSERT INTO sys_user_role (id, user_id, role_id, create_time, update_time, version, deleted) VALUES
(20000, 1000, 1, NOW(), NOW(), 1, 0), -- 管理员用户拥有管理员角色
(20001, 1001, 2, NOW(), NOW(), 1, 0)  -- 普通用户拥有普通用户角色
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 多角色叠加演示用户（密码均为 password）
-- =============================================

-- 张三 — 技术部/研发组 (dept=14)，ROLE_USER + ROLE_DEPT_TECH
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1002, 'zhangsan', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'zhangsan@example.com', 28, 'ROLE_USER,ROLE_DEPT_TECH', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 14, 1, 0, '技术部研发组 — 标准成员')
ON CONFLICT (id) DO NOTHING;

-- 李四 — 技术部/研发组 (dept=14)，ROLE_USER + ROLE_DEPT_TECH（同部门=同权限）
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1003, 'lisi', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'lisi@example.com', 26, 'ROLE_USER,ROLE_DEPT_TECH', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 14, 1, 0, '技术部研发组 — 标准成员')
ON CONFLICT (id) DO NOTHING;

-- 王五 — 市场部 (dept=12)，ROLE_USER + ROLE_DEPT_MARKET（不同部门=不同的默认角色）
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1004, 'wangwu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'wangwu@example.com', 30, 'ROLE_USER,ROLE_DEPT_MARKET', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 12, 1, 0, '市场部 — 仅支付相关菜单')
ON CONFLICT (id) DO NOTHING;

-- 赵六 — 技术部/测试组 (dept=15)，ROLE_USER + ROLE_DEPT_TECH + ROLE_PROJECT_LEAD（叠加特殊权限）
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1005, 'zhaoliu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'zhaoliu@example.com', 32, 'ROLE_USER,ROLE_DEPT_TECH,ROLE_PROJECT_LEAD', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 15, 1, 0, '测试组组长 — 叠加项目组长权限')
ON CONFLICT (id) DO NOTHING;

-- 马七 — 财务部 (dept=13)，ROLE_USER + ROLE_DEPT_FINANCE
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, dept_id, version, deleted, remark) VALUES
(1006, 'maqi', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'maqi@example.com', 35, 'ROLE_USER,ROLE_DEPT_FINANCE', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 13, 1, 0, '财务部 — 支付/对帐/监控权限')
ON CONFLICT (id) DO NOTHING;

-- 演示用户角色分配
INSERT INTO sys_user_role (id, user_id, role_id, create_time, update_time, version, deleted) VALUES
-- 张三：普通用户 + 技术部默认
(20002, 1002, 2, NOW(), NOW(), 1, 0),
(20003, 1002, 3, NOW(), NOW(), 1, 0),
-- 李四：普通用户 + 技术部默认
(20004, 1003, 2, NOW(), NOW(), 1, 0),
(20005, 1003, 3, NOW(), NOW(), 1, 0),
-- 王五：普通用户 + 市场部默认
(20006, 1004, 2, NOW(), NOW(), 1, 0),
(20007, 1004, 4, NOW(), NOW(), 1, 0),
-- 赵六：普通用户 + 技术部默认 + 项目组长（三角色叠加）
(20008, 1005, 2, NOW(), NOW(), 1, 0),
(20009, 1005, 3, NOW(), NOW(), 1, 0),
(20010, 1005, 6, NOW(), NOW(), 1, 0),
-- 马七：普通用户 + 财务部默认
(20011, 1006, 2, NOW(), NOW(), 1, 0),
(20012, 1006, 5, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 支付模块初始数据
-- =============================================

-- 插入示例支付订单
INSERT INTO payment_order (id, order_no, payment_method, amount, subject, body, status, trade_no, buyer_id, paid_time, refund_amount, create_time, update_time, version, deleted) VALUES
(5001, 'AL20260503001', 'ALIPAY', 99.00, '测试商品-支付宝', '支付宝支付测试订单', 'SUCCESS', '2026050322001000000000000001', '2088000000000001', NOW(), 0.00, NOW(), NOW(), 1, 0),
(5002, 'WX20260503001', 'WECHAT', 199.00, '测试商品-微信', '微信支付测试订单', 'SUCCESS', '4200000000202605030000000001', 'oUpF8uMuAJO_M2pxb1Q9zNjWeS6o', NOW(), 0.00, NOW(), NOW(), 1, 0),
(5003, 'AL20260503002', 'ALIPAY', 299.00, '会员订阅', '月度会员订阅服务', 'SUCCESS', '2026050322001000000000000002', '2088000000000002', NOW(), 0.00, NOW(), NOW(), 1, 0),
(5004, 'WX20260503002', 'WECHAT', 149.00, '电子礼品卡', '100元电子礼品卡', 'PENDING', NULL, NULL, NULL, 0.00, NOW(), NOW(), 1, 0),
(5005, 'AL20260503003', 'ALIPAY', 599.00, '年度会员', '年度会员订阅服务', 'CLOSED', NULL, NULL, NULL, 0.00, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 插入示例对帐记录
INSERT INTO reconciliation_record (id, recon_date, payment_method, local_total_amount, remote_total_amount, local_count, remote_count, diff_amount, diff_count, status, summary, create_time, update_time, version, deleted) VALUES
(6001, '2026-05-01', 'ALIPAY', 398.00, 398.00, 2, 2, 0.00, 0, 'SUCCESS', '对帐一致: 2笔, 总额¥398.00', NOW(), NOW(), 1, 0),
(6002, '2026-05-01', 'WECHAT', 199.00, 199.00, 1, 1, 0.00, 0, 'SUCCESS', '对帐一致: 1笔, 总额¥199.00', NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 插入示例对帐明细
INSERT INTO reconciliation_detail (id, recon_record_id, recon_date, order_no, trade_no, local_amount, remote_amount, local_status, remote_status, diff_type, diff_desc, create_time, deleted) VALUES
(7001, 6001, '2026-05-01', 'AL20260503001', '2026050322001000000000000001', 99.00, 99.00, 'SUCCESS', 'SUCCESS', 'MATCH', '一致', NOW(), 0),
(7002, 6001, '2026-05-01', 'AL20260503002', '2026050322001000000000000002', 299.00, 299.00, 'SUCCESS', 'SUCCESS', 'MATCH', '一致', NOW(), 0),
(7003, 6002, '2026-05-01', 'WX20260503001', '4200000000202605030000000001', 199.00, 199.00, 'SUCCESS', 'SUCCESS', 'MATCH', '一致', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 字典管理初始数据
-- =============================================

-- 字典类型
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time) VALUES
(8001, '支付方式', 'payment_method', '0', '支付方式字典', NOW(), NOW()),
(8002, '订单状态', 'order_status', '0', '支付订单状态', NOW(), NOW()),
(8003, '对帐状态', 'recon_status', '0', '对帐记录状态', NOW(), NOW()),
(8004, '对帐差异类型', 'recon_diff_type', '0', '对帐明细差异类型', NOW(), NOW()),
(8005, '用户状态', 'user_status', '0', '用户启用/禁用', NOW(), NOW()),
(8006, '通用是否', 'yes_no', '0', '通用是/否字典', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 字典数据
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, css_class, list_class, is_default, status, create_time, update_time) VALUES
-- 支付方式
(8101, 'payment_method', '支付宝', 'ALIPAY', 1, '', 'primary', '0', '0', NOW(), NOW()),
(8102, 'payment_method', '微信支付', 'WECHAT', 2, '', 'success', '0', '0', NOW(), NOW()),
-- 订单状态
(8201, 'order_status', '待支付', 'PENDING', 1, '', 'warning', '0', '0', NOW(), NOW()),
(8202, 'order_status', '已支付', 'SUCCESS', 2, '', 'success', '0', '0', NOW(), NOW()),
(8203, 'order_status', '已关闭', 'CLOSED', 3, '', 'info', '0', '0', NOW(), NOW()),
(8204, 'order_status', '已退款', 'REFUND', 4, '', 'danger', '0', '0', NOW(), NOW()),
-- 对帐状态
(8301, 'recon_status', '对帐一致', 'SUCCESS', 1, '', 'success', '0', '0', NOW(), NOW()),
(8302, 'recon_status', '存在差异', 'DIFF', 2, '', 'warning', '0', '0', NOW(), NOW()),
(8303, 'recon_status', '对帐异常', 'ERROR', 3, '', 'danger', '0', '0', NOW(), NOW()),
-- 对帐差异类型
(8401, 'recon_diff_type', '一致', 'MATCH', 1, '', 'success', '0', '0', NOW(), NOW()),
(8402, 'recon_diff_type', '金额/状态不符', 'MISMATCH', 2, '', 'warning', '0', '0', NOW(), NOW()),
(8403, 'recon_diff_type', '仅本地存在', 'LOCAL_ONLY', 3, '', 'info', '0', '0', NOW(), NOW()),
(8404, 'recon_diff_type', '仅平台存在', 'REMOTE_ONLY', 4, '', 'danger', '0', '0', NOW(), NOW()),
-- 用户状态
(8501, 'user_status', '启用', '0', 1, '', 'success', '0', '0', NOW(), NOW()),
(8502, 'user_status', '禁用', '1', 2, '', 'danger', '0', '0', NOW(), NOW()),
-- 通用是否
(8601, 'yes_no', '是', '1', 1, '', 'success', '0', '0', NOW(), NOW()),
(8602, 'yes_no', '否', '0', 2, '', 'danger', '0', '0', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- AI 模型提供商配置初始数据
-- =============================================
INSERT INTO ai_provider_config (id, name, display_name, api_key, base_url, model, max_tokens, temperature, cost_per_million_tokens, enabled, sort_order, create_time, update_time) VALUES
(9001, 'deepseek', 'DeepSeek', '', 'https://api.deepseek.com', 'deepseek-chat', 4096, 0.7, 1.0, TRUE, 1, NOW(), NOW()),
(9002, 'qwen',    '通义千问',       '', 'https://dashscope.aliyuncs.com/compatible-mode', 'qwen-turbo',     4096, 0.7, 2.0,  TRUE, 2, NOW(), NOW()),
(9003, 'kimi',    'Kimi (月之暗面)', '', 'https://api.moonshot.cn',                         'moonshot-v1-8k', 4096, 0.7, 12.0, TRUE, 3, NOW(), NOW()),
(9004, 'glm',     '智谱 GLM',       '', 'https://open.bigmodel.cn/api/paas/v4',             'glm-4-flash',    4096, 0.7, 5.0,  TRUE, 4, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 菜单管理初始数据
-- =============================================

-- 菜单数据 (M=目录, C=菜单)
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, status, create_time, update_time, version, deleted) VALUES
-- 系统管理
(100, 0, '系统管理', '/system', '', 'system', 1, 'M', '', 0, 0, NOW(), NOW(), 1, 0),
(101, 100, '用户管理', '/system/user', 'system/user/index', 'user', 1, 'C', 'user:read', 0, 0, NOW(), NOW(), 1, 0),
(102, 100, '角色管理', '/system/role', 'system/role/index', 'peoples', 2, 'C', 'role:read', 0, 0, NOW(), NOW(), 1, 0),
(103, 100, '权限管理', '/system/permission', 'system/permission/index', 'lock', 3, 'C', 'permission:read', 0, 0, NOW(), NOW(), 1, 0),
(104, 100, '菜单管理', '/system/menu', 'system/menu/index', 'tree-table', 4, 'C', '', 0, 0, NOW(), NOW(), 1, 0),

-- 支付管理
(200, 0, '支付管理', '/payment', '', 'money', 2, 'M', '', 0, 0, NOW(), NOW(), 1, 0),
(201, 200, '支付订单', '/payment/order', 'payment/order/index', 'list', 1, 'C', '', 0, 0, NOW(), NOW(), 1, 0),
(202, 200, '对帐管理', '/payment/reconciliation', 'payment/reconciliation/index', 'chart', 2, 'C', '', 0, 0, NOW(), NOW(), 1, 0),
(203, 200, '支付统计', '/payment/stats', 'payment/stats/index', 'chart', 3, 'C', '', 0, 0, NOW(), NOW(), 1, 0),

-- 监控管理
(300, 0, '监控管理', '/monitor', '', 'monitor', 3, 'M', '', 0, 0, NOW(), NOW(), 1, 0),
(301, 300, 'JVM 监控', '/monitor/jvm', 'monitor/jvm/index', 'dashboard', 1, 'C', '', 0, 0, NOW(), NOW(), 1, 0),
(302, 300, '数据库监控', '/monitor/db', 'monitor/db/index', 'table', 2, 'C', '', 0, 0, NOW(), NOW(), 1, 0),
(303, 300, '操作日志', '/monitor/operlog', 'monitor/operlog/index', 'log', 3, 'C', '', 0, 0, NOW(), NOW(), 1, 0),
(304, 300, '在线用户', '/monitor/online', 'monitor/online/index', 'online', 4, 'C', '', 0, 0, NOW(), NOW(), 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 角色菜单分配 (sys_role_menu 从 30001 开始)
-- 管理员角色(role_id=1) → 所有 13 个菜单
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30001, 1, 100, NOW(), NOW(), 1, 0),
(30002, 1, 101, NOW(), NOW(), 1, 0),
(30003, 1, 102, NOW(), NOW(), 1, 0),
(30004, 1, 103, NOW(), NOW(), 1, 0),
(30005, 1, 104, NOW(), NOW(), 1, 0),
(30006, 1, 200, NOW(), NOW(), 1, 0),
(30007, 1, 201, NOW(), NOW(), 1, 0),
(30008, 1, 202, NOW(), NOW(), 1, 0),
(30009, 1, 203, NOW(), NOW(), 1, 0),
(30010, 1, 300, NOW(), NOW(), 1, 0),
(30011, 1, 301, NOW(), NOW(), 1, 0),
(30012, 1, 302, NOW(), NOW(), 1, 0),
(30013, 1, 303, NOW(), NOW(), 1, 0),
(30014, 1, 304, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);

-- 普通用户角色(role_id=2) → 支付管理 + 监控管理（不含系统管理）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30015, 2, 200, NOW(), NOW(), 1, 0),
(30016, 2, 201, NOW(), NOW(), 1, 0),
(30017, 2, 202, NOW(), NOW(), 1, 0),
(30018, 2, 203, NOW(), NOW(), 1, 0),
(30019, 2, 300, NOW(), NOW(), 1, 0),
(30020, 2, 301, NOW(), NOW(), 1, 0),
(30021, 2, 302, NOW(), NOW(), 1, 0),
(30022, 2, 303, NOW(), NOW(), 1, 0),
(30023, 2, 304, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);

-- 部门默认角色菜单分配
-- ROLE_DEPT_TECH(3): 支付管理 + 监控管理（与技术部相关的模块）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30024, 3, 200, NOW(), NOW(), 1, 0),
(30025, 3, 201, NOW(), NOW(), 1, 0),
(30026, 3, 202, NOW(), NOW(), 1, 0),
(30027, 3, 203, NOW(), NOW(), 1, 0),
(30028, 3, 300, NOW(), NOW(), 1, 0),
(30029, 3, 301, NOW(), NOW(), 1, 0),
(30030, 3, 302, NOW(), NOW(), 1, 0),
(30031, 3, 303, NOW(), NOW(), 1, 0),
(30032, 3, 304, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);

-- ROLE_DEPT_MARKET(4): 仅支付管理（市场部关注订单和统计，不需要对帐和监控）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30033, 4, 200, NOW(), NOW(), 1, 0),
(30034, 4, 201, NOW(), NOW(), 1, 0),
(30035, 4, 203, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);

-- ROLE_DEPT_FINANCE(5): 支付管理 + 监控管理（财务需要支付/对帐/监控全部）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30036, 5, 200, NOW(), NOW(), 1, 0),
(30037, 5, 201, NOW(), NOW(), 1, 0),
(30038, 5, 202, NOW(), NOW(), 1, 0),
(30039, 5, 203, NOW(), NOW(), 1, 0),
(30040, 5, 300, NOW(), NOW(), 1, 0),
(30041, 5, 301, NOW(), NOW(), 1, 0),
(30042, 5, 302, NOW(), NOW(), 1, 0),
(30043, 5, 303, NOW(), NOW(), 1, 0),
(30044, 5, 304, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);

-- ROLE_PROJECT_LEAD(6): 系统管理目录 + 用户管理菜单（仅管理用户，不碰角色/权限/菜单）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, update_time, version, deleted)
SELECT id, role_id, menu_id, create_time, update_time, version, deleted
FROM (VALUES
(30045, 6, 100, NOW(), NOW(), 1, 0),
(30046, 6, 101, NOW(), NOW(), 1, 0)
) AS t(id, role_id, menu_id, create_time, update_time, version, deleted)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu r
    WHERE r.role_id = t.role_id AND r.menu_id = t.menu_id AND r.deleted = 0
);