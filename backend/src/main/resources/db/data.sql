-- ============================================================================
-- ServiceFlow 企业售后工单管理系统 — 基础数据脚本
-- 内容：角色、权限、角色权限关系、演示账号、故障分类、系统配置
-- 说明：
--   1. 本脚本与结构脚本 db/schema.sql 分离，可单独重复执行（幂等）。
--   2. 演示账号仅用于本地开发环境，密码明文为 Serviceflow@2026，
--      库中存储的是 BCrypt 哈希（$2a$10$...），生产环境不得保留这些账号。
--   3. 业务演示数据（客户/设备/工单/评价）见 db/demo-data.sql，随对应阶段补充。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. 角色（四种）
-- ---------------------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, description, sort, status)
VALUES (1, 'ROLE_ADMIN', '系统管理员', '管理用户、角色、故障分类与系统配置，查看全局运营统计与操作日志', 1, 1),
       (2, 'ROLE_SERVICE', '售后客服', '维护客户与设备信息，创建工单，分配或转派维修任务', 2, 1),
       (3, 'ROLE_ENGINEER', '维修工程师', '查看分配给自己的工单，接单、处理故障、填写维修记录、提交处理结果', 3, 1),
       (4, 'ROLE_CUSTOMER', '客户', '查看自己的设备与工单，确认服务结果并进行评价', 4, 1)
ON DUPLICATE KEY UPDATE role_name   = VALUES(role_name),
                        description = VALUES(description),
                        sort        = VALUES(sort),
                        status      = VALUES(status);

-- ---------------------------------------------------------------------------
-- 2. 权限
-- ---------------------------------------------------------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, module, sort)
VALUES (1, 'user:view', '查看用户', 'API', 'system', 1),
       (2, 'user:manage', '管理用户', 'API', 'system', 2),
       (3, 'role:view', '查看角色权限', 'API', 'system', 3),
       (4, 'role:manage', '管理角色权限', 'API', 'system', 4),
       (5, 'fault-category:view', '查看故障分类', 'API', 'system', 5),
       (6, 'fault-category:manage', '管理故障分类', 'API', 'system', 6),
       (7, 'config:view', '查看系统配置', 'API', 'system', 7),
       (8, 'config:manage', '管理系统配置', 'API', 'system', 8),
       (9, 'log:view', '查看操作日志', 'API', 'system', 9),
       (10, 'customer:view', '查看客户', 'API', 'customer', 10),
       (11, 'customer:manage', '管理客户', 'API', 'customer', 11),
       (12, 'device:view', '查看设备', 'API', 'device', 12),
       (13, 'device:manage', '管理设备', 'API', 'device', 13),
       (14, 'ticket:view', '查看工单', 'API', 'ticket', 14),
       (15, 'ticket:view-all', '查看全部工单', 'API', 'ticket', 15),
       (16, 'ticket:create', '创建工单', 'API', 'ticket', 16),
       (17, 'ticket:assign', '分派工单', 'API', 'ticket', 17),
       (18, 'ticket:transfer', '转派工单', 'API', 'ticket', 18),
       (19, 'ticket:accept', '接单', 'API', 'ticket', 19),
       (20, 'ticket:process', '填写处理记录', 'API', 'ticket', 20),
       (21, 'ticket:complete', '提交处理结果', 'API', 'ticket', 21),
       (22, 'ticket:confirm', '确认服务结果', 'API', 'ticket', 22),
       (23, 'ticket:reopen', '退回重做或重新打开', 'API', 'ticket', 23),
       (24, 'ticket:cancel', '取消工单', 'API', 'ticket', 24),
       (25, 'ticket:evaluate', '提交服务评价', 'API', 'evaluation', 25),
       (26, 'attachment:upload', '上传附件', 'API', 'attachment', 26),
       (27, 'attachment:download', '下载附件', 'API', 'attachment', 27),
       (28, 'attachment:delete', '删除附件', 'API', 'attachment', 28),
       (29, 'dashboard:view', '查看个人统计', 'API', 'dashboard', 29),
       (30, 'dashboard:view-all', '查看全局统计', 'API', 'dashboard', 30)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        module    = VALUES(module),
                        sort      = VALUES(sort);

-- ---------------------------------------------------------------------------
-- 3. 角色权限关系
-- ---------------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, perm_id)
VALUES (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7),
       (8, 1, 8), (9, 1, 9), (10, 1, 10), (11, 1, 11), (12, 1, 12), (13, 1, 13),
       (14, 1, 14), (15, 1, 15), (16, 1, 16), (17, 1, 17), (18, 1, 18), (19, 1, 19),
       (20, 1, 20), (21, 1, 21), (22, 1, 22), (23, 1, 23), (24, 1, 24), (25, 1, 26),
       (26, 1, 27), (27, 1, 28), (28, 1, 29), (29, 1, 30), (30, 2, 5), (31, 2, 10),
       (32, 2, 11), (33, 2, 12), (34, 2, 13), (35, 2, 14), (36, 2, 15), (37, 2, 16),
       (38, 2, 17), (39, 2, 18), (40, 2, 23), (41, 2, 24), (42, 2, 26), (43, 2, 27),
       (44, 2, 28), (45, 2, 29), (46, 2, 30), (47, 3, 5), (48, 3, 12), (49, 3, 14),
       (50, 3, 18), (51, 3, 19), (52, 3, 20), (53, 3, 21), (54, 3, 26), (55, 3, 27),
       (56, 3, 29), (57, 4, 12), (58, 4, 14), (59, 4, 22), (60, 4, 23), (61, 4, 25),
       (62, 4, 26), (63, 4, 27), (64, 4, 10), (65, 1, 25)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ---------------------------------------------------------------------------
-- 4. 演示账号（仅本地开发使用，密码统一为 Serviceflow@2026）
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, phone, email, status, remark)
VALUES (1, 'admin', '$2a$10$TnO2jI.ZOAgsXDyE3OafO.pTsSdBUevlbMeKVLVSJZYPKrf6XgQ.e',
        '系统管理员', '13800000001', 'admin@serviceflow.local', 1, '本地开发演示账号'),
       (2, 'service01', '$2a$10$yrnWbISR8WYlPvYQBGK0Z.xhCcxGfL5zvsBmyIq70rgo7aAR1Tvni',
        '客服一号', '13800000002', 'service01@serviceflow.local', 1, '本地开发演示账号'),
       (3, 'engineer01', '$2a$10$IbjAGHmC76jFbge7HDQyGeCNySCe8NvLV3dtKYIAANsdnIN3JLl6.',
        '维修工程师一号', '13800000003', 'engineer01@serviceflow.local', 1, '本地开发演示账号'),
       (4, 'engineer02', '$2a$10$BWWBHmVjnz2WSb7XeGf0qOwIeqsY8QxChFSk.jTAlhckvFKvR8NRy',
        '维修工程师二号', '13800000004', 'engineer02@serviceflow.local', 1, '本地开发演示账号'),
       (5, 'customer01', '$2a$10$Yz/rv5J3O2zYYuQwJZ3ZAe52G0F9rJf7NcvdREvqwmVYuHV0M644S',
        '客户联系人一', '13800000005', 'customer01@serviceflow.local', 1, '本地开发演示账号'),
       (6, 'customer02', '$2a$10$tl7EaC6ARTiKwikXH1UYJeIy6yegdk5lH3soDIyasxjOwqaK9AIq6',
        '客户联系人二', '13800000006', 'customer02@serviceflow.local', 1, '本地开发演示账号')
ON DUPLICATE KEY UPDATE password  = VALUES(password),
                        real_name = VALUES(real_name),
                        phone     = VALUES(phone),
                        email     = VALUES(email),
                        status    = VALUES(status),
                        remark    = VALUES(remark);

-- ---------------------------------------------------------------------------
-- 5. 用户角色关系
-- ---------------------------------------------------------------------------
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 1),
       (2, 2, 2),
       (3, 3, 3),
       (4, 4, 3),
       (5, 5, 4),
       (6, 6, 4)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ---------------------------------------------------------------------------
-- 6. 故障分类
-- ---------------------------------------------------------------------------
INSERT INTO fault_category (id, category_code, category_name, parent_id, description, sort, status)
VALUES (1, 'FC-HARDWARE', '硬件故障', 0, '主板、电源、接口等硬件损坏', 1, 1),
       (2, 'FC-SOFTWARE', '软件故障', 0, '系统、驱动、应用软件异常', 2, 1),
       (3, 'FC-NETWORK', '网络通信异常', 0, '无法联网、信号弱、连接中断', 3, 1),
       (4, 'FC-BATTERY', '电池与续航', 0, '电池鼓包、续航下降、无法充电', 4, 1),
       (5, 'FC-DISPLAY', '屏幕显示异常', 0, '花屏、黑屏、触控失灵', 5, 1),
       (6, 'FC-SENSOR', '传感器异常', 0, '传感器读数错误或失效', 6, 1),
       (7, 'FC-FIRMWARE', '固件升级', 0, '固件版本升级与回滚', 7, 1),
       (8, 'FC-OTHER', '其他问题', 0, '未归类的故障', 99, 1)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        description   = VALUES(description),
                        sort          = VALUES(sort),
                        status        = VALUES(status);

-- ---------------------------------------------------------------------------
-- 7. 系统配置
-- ---------------------------------------------------------------------------
INSERT INTO sys_config (id, config_key, config_value, config_name, config_group, value_type, remark)
VALUES (1, 'ticket.auto_confirm_enabled', 'true', '启用超期自动确认', 'ticket', 'BOOLEAN',
        '待确认工单超过确认时限后由定时任务自动确认'),
       (2, 'ticket.confirm_timeout_hours', '72', '客户确认时限（小时）', 'ticket', 'NUMBER',
        '超过该时长未确认的工单将被自动确认'),
       (3, 'ticket.reopen_window_days', '7', '重新打开窗口（天）', 'ticket', 'NUMBER',
        '已完成工单允许重新打开的天数'),
       (4, 'ticket.default_sla_hours', '48', '默认 SLA（小时）', 'ticket', 'NUMBER',
        '创建工单时未指定截止时间则按优先级与默认 SLA 计算'),
       (5, 'attachment.max_size_mb', '20', '附件大小上限（MB）', 'attachment', 'NUMBER', '单个附件允许的最大体积'),
       (6, 'attachment.allowed_extensions', 'jpg,jpeg,png,gif,pdf,txt,log,zip,docx,xlsx',
        '附件允许的扩展名', 'attachment', 'STRING', '逗号分隔，上传时校验'),
       (7, 'security.login_fail_limit', '5', '登录失败锁定阈值', 'security', 'NUMBER', '连续失败达到该次数后锁定账号'),
       (8, 'security.login_lock_minutes', '15', '账号锁定时长（分钟）', 'security', 'NUMBER', '锁定后自动解锁的时间')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value),
                        config_name  = VALUES(config_name),
                        config_group = VALUES(config_group),
                        value_type   = VALUES(value_type),
                        remark       = VALUES(remark);
