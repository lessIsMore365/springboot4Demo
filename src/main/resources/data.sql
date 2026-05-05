-- =============================================
-- 初始角色和权限数据
-- 注意：这些ID是示例值，实际使用中可能会使用雪花算法生成
-- =============================================

-- 插入初始角色
INSERT INTO sys_role (id, name, code, description, enabled, sort_order, create_time, update_time, version, deleted) VALUES
(1, '管理员', 'ROLE_ADMIN', '系统管理员，拥有所有权限', TRUE, 1, NOW(), NOW(), 1, 0),
(2, '普通用户', 'ROLE_USER', '普通用户，拥有基本权限', TRUE, 2, NOW(), NOW(), 1, 0)
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

-- 创建一个默认管理员用户（用户名：admin，密码：admin123）
-- 注意：密码需要使用BCrypt加密，实际值应为加密后的密码
-- 你可以使用以下代码生成BCrypt密码：
--   new BCryptPasswordEncoder().encode("admin123")
-- 生成的密码类似于：$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-- 这里使用一个示例加密密码（实际部署时请重新生成）
-- 创建一个默认管理员用户（用户名：admin，密码：password）
-- BCrypt 哈希值：password (强度 10)
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, version, deleted, remark) VALUES
(1000, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@example.com', 30, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 1, 0, '系统管理员')
ON CONFLICT (id) DO NOTHING;

-- 创建一个普通用户（用户名：user，密码：password）
-- BCrypt 哈希值：password (强度 10)
INSERT INTO sys_user (id, username, password, email, age, roles, enabled, account_non_locked, account_non_expired, credentials_non_expired, create_time, update_time, last_login_time, version, deleted, remark) VALUES
(1001, 'user', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'user@example.com', 25, 'ROLE_USER', TRUE, TRUE, TRUE, TRUE, NOW(), NOW(), NOW(), 1, 0, '普通用户')
ON CONFLICT (id) DO NOTHING;

-- 为用户分配角色
INSERT INTO sys_user_role (id, user_id, role_id, create_time, update_time, version, deleted) VALUES
(20000, 1000, 1, NOW(), NOW(), 1, 0), -- 管理员用户拥有管理员角色
(20001, 1001, 2, NOW(), NOW(), 1, 0)  -- 普通用户拥有普通用户角色
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