-- ============================================================================
-- ServiceFlow 企业售后工单管理系统 — 业务演示数据
-- 内容：客户档案、设备档案，并把客户演示账号绑定到对应客户档案
-- 说明：
--   1. 与结构脚本（schema.sql）、基础数据脚本（data.sql）分离，可重复执行。
--   2. 仅用于本地开发与演示，生产环境不要执行本脚本。
--   3. 客户演示账号 customer01 / customer02 的 customer_id 在此绑定，
--      用于验证"客户只能看到自己的数据"这一数据隔离规则。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. 客户档案
-- ---------------------------------------------------------------------------
INSERT INTO customer (id, customer_code, customer_name, contact_name, contact_phone, contact_email,
                      province, city, address, industry, level, source, status, remark, deleted, create_by)
VALUES (1, 'CUS-0001', '星海智造有限公司', '张伟', '13910000001', 'zhangwei@xinghai.local',
        '江苏省', '苏州市', '苏州工业园区星湖街 88 号', '智能制造', 'A', 'WEB', 1, '重点客户，设备保有量高', 0, 1),
       (2, 'CUS-0002', '云岭科技有限公司', '李娜', '13910000002', 'lina@yunling.local',
        '浙江省', '杭州市', '杭州市滨江区江南大道 100 号', '软件与信息服务', 'B', 'PHONE', 1, '年度维保客户', 0, 1),
       (3, 'CUS-0003', '北方物流集团', '王强', '13910000003', 'wangqiang@beifang.local',
        '山东省', '青岛市', '青岛市黄岛区物流大道 1 号', '仓储物流', 'A', 'WECHAT', 1, '设备分布多个仓库', 0, 1),
       (4, 'CUS-0004', '清源环保工程有限公司', '赵敏', '13910000004', 'zhaomin@qingyuan.local',
        '广东省', '深圳市', '深圳市南山区科技南十二路 18 号', '环保工程', 'C', 'EMAIL', 1, '新签约客户', 0, 1)
ON DUPLICATE KEY UPDATE customer_name = VALUES(customer_name),
                        contact_name  = VALUES(contact_name),
                        contact_phone = VALUES(contact_phone),
                        contact_email = VALUES(contact_email),
                        province      = VALUES(province),
                        city          = VALUES(city),
                        address       = VALUES(address),
                        industry      = VALUES(industry),
                        level         = VALUES(level),
                        source        = VALUES(source),
                        status        = VALUES(status),
                        remark        = VALUES(remark);

-- ---------------------------------------------------------------------------
-- 2. 设备档案
-- ---------------------------------------------------------------------------
INSERT INTO device (id, device_code, device_name, device_type, brand, model, sn_no, customer_id,
                    purchase_date, warranty_expire_date, install_address, status, remark, deleted, create_by)
VALUES (1, 'DEV-0001', '智能分拣终端 A1', '智能终端', 'ServiceFlow', 'SF-SORT-A1', 'SN20240001', 1,
        '2024-03-15', '2027-03-14', '苏州工业园区星湖街 88 号 1 号厂房', 1, '主力产线设备', 0, 1),
       (2, 'DEV-0002', '智能分拣终端 A2', '智能终端', 'ServiceFlow', 'SF-SORT-A1', 'SN20240002', 1,
        '2024-03-15', '2027-03-14', '苏州工业园区星湖街 88 号 2 号厂房', 1, NULL, 0, 1),
       (3, 'DEV-0003', '工业视觉检测仪 V3', '检测设备', 'ServiceFlow', 'SF-VISION-V3', 'SN20240003', 1,
        '2023-06-01', '2025-05-31', '苏州工业园区星湖街 88 号质检车间', 1, '保修即将到期', 0, 1),
       (4, 'DEV-0004', '数据采集网关 G2', '网络设备', 'ServiceFlow', 'SF-GATE-G2', 'SN20240004', 2,
        '2024-08-20', '2027-08-19', '杭州市滨江区江南大道 100 号机房', 1, NULL, 0, 1),
       (5, 'DEV-0005', '手持扫码终端 H5', '手持终端', 'ServiceFlow', 'SF-HAND-H5', 'SN20240005', 2,
        '2024-09-10', '2026-09-09', '杭州市滨江区江南大道 100 号仓库', 1, NULL, 0, 1),
       (6, 'DEV-0006', '冷链温控记录仪 T8', '监测设备', 'ServiceFlow', 'SF-TEMP-T8', 'SN20240006', 3,
        '2023-11-05', '2026-11-04', '青岛市黄岛区物流大道 1 号冷库', 1, '需定期校准', 0, 1),
       (7, 'DEV-0007', '冷链温控记录仪 T8', '监测设备', 'ServiceFlow', 'SF-TEMP-T8', 'SN20240007', 3,
        '2023-11-05', '2026-11-04', '青岛市黄岛区物流大道 1 号冷库', 2, '已报废待替换', 0, 1),
       (8, 'DEV-0008', '水质在线监测仪 W2', '监测设备', 'ServiceFlow', 'SF-WATER-W2', 'SN20240008', 4,
        '2025-01-18', '2028-01-17', '深圳市南山区科技南十二路 18 号', 1, NULL, 0, 1)
ON DUPLICATE KEY UPDATE device_name          = VALUES(device_name),
                        device_type          = VALUES(device_type),
                        brand                = VALUES(brand),
                        model                = VALUES(model),
                        sn_no                = VALUES(sn_no),
                        customer_id          = VALUES(customer_id),
                        purchase_date        = VALUES(purchase_date),
                        warranty_expire_date = VALUES(warranty_expire_date),
                        install_address      = VALUES(install_address),
                        status               = VALUES(status),
                        remark               = VALUES(remark);

-- ---------------------------------------------------------------------------
-- 3. 绑定客户演示账号与客户档案
-- ---------------------------------------------------------------------------
UPDATE sys_user SET customer_id = 1 WHERE username = 'customer01';
UPDATE sys_user SET customer_id = 2 WHERE username = 'customer02';

-- ---------------------------------------------------------------------------
-- 4. 工单演示数据
-- 说明：编号使用固定的 SO202601010000xx 前缀（与运行日期的当日序列互不冲突），
--       覆盖待分派、待接单、处理中、待确认、已完成、已取消与逾期七种场景。
-- ---------------------------------------------------------------------------
INSERT INTO service_ticket (id, ticket_no, title, description, customer_id, device_id, fault_category_id,
                            priority, status, source, reporter_name, reporter_phone, assignee_id, deadline_time,
                            assign_time, accept_time, start_time, finish_time, confirm_time, closed_time,
                            cancel_time, cancel_reason, reject_count, reopen_count, transfer_count, handle_hours,
                            version, create_by, create_time)
VALUES (1, 'SO20260101000001', '分拣终端无法启动', '设备上电后指示灯不亮，产线停机', 1, 1, 1,
        'URGENT', 'COMPLETED', 'PHONE', '张伟', '13910000001', 3,
        DATE_ADD(NOW(), INTERVAL -10 DAY), DATE_ADD(NOW(), INTERVAL -12 DAY), DATE_ADD(NOW(), INTERVAL -11 DAY),
        DATE_ADD(NOW(), INTERVAL -11 DAY), DATE_ADD(NOW(), INTERVAL -11 DAY) + INTERVAL 6 HOUR,
        DATE_ADD(NOW(), INTERVAL -10 DAY), DATE_ADD(NOW(), INTERVAL -10 DAY), NULL, NULL, 0, 0, 0, 6.00, 0, 2,
        DATE_ADD(NOW(), INTERVAL -12 DAY)),
       (2, 'SO20260101000002', '视觉检测仪误报率升高', '近一周误报率由 1% 上升到 6%', 1, 3, 2,
        'HIGH', 'PENDING_CONFIRM', 'WEB', '张伟', '13910000001', 4,
        DATE_ADD(NOW(), INTERVAL 20 HOUR), DATE_ADD(NOW(), INTERVAL -3 DAY), DATE_ADD(NOW(), INTERVAL -3 DAY),
        DATE_ADD(NOW(), INTERVAL -3 DAY), DATE_ADD(NOW(), INTERVAL -1 DAY), NULL, NULL, NULL, NULL, 0, 1, 1, 48.00, 0, 2,
        DATE_ADD(NOW(), INTERVAL -3 DAY)),
       (3, 'SO20260101000003', '数据采集网关频繁掉线', '每小时掉线 2~3 次，影响数据上报', 2, 4, 3,
        'HIGH', 'PROCESSING', 'WECHAT', '李娜', '13910000002', 3,
        DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -1 DAY),
        DATE_ADD(NOW(), INTERVAL -1 DAY), NULL, NULL, NULL, NULL, NULL, 0, 0, 0, NULL, 0, 2,
        DATE_ADD(NOW(), INTERVAL -1 DAY)),
       (4, 'SO20260101000004', '手持终端电池续航异常', '满电仅可使用 1.5 小时', 2, 5, 4,
        'MEDIUM', 'PENDING_ACCEPT', 'PHONE', '李娜', '13910000002', 4,
        DATE_ADD(NOW(), INTERVAL 12 HOUR), DATE_ADD(NOW(), INTERVAL -6 HOUR),
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        0, 0, 1, NULL, 0, 2, DATE_ADD(NOW(), INTERVAL -8 HOUR)),
       (5, 'SO20260101000005', '冷库温控记录仪数据偏差', '温度读数比实际低 2 摄氏度', 3, 6, 6,
        'MEDIUM', 'PENDING_ASSIGN', 'EMAIL', '王强', '13910000003', NULL,
        DATE_ADD(NOW(), INTERVAL 16 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, NULL, 0, 2,
        DATE_ADD(NOW(), INTERVAL -8 HOUR)),
       (6, 'SO20260101000006', '水质监测仪无法联网', '设备离线，无法推送监测数据', 4, 8, 3,
        'LOW', 'PENDING_ASSIGN', 'WEB', '赵敏', '13910000004', NULL,
        DATE_ADD(NOW(), INTERVAL 40 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, NULL, 0, 2,
        DATE_ADD(NOW(), INTERVAL -4 HOUR)),
       (7, 'SO20260101000007', '报废设备回收确认', '客户申请报废设备上门回收', 3, 7, 8,
        'LOW', 'CANCELLED', 'PHONE', '王强', '13910000003', NULL,
        DATE_ADD(NOW(), INTERVAL -2 DAY), NULL, NULL, NULL, NULL, NULL, NULL, DATE_ADD(NOW(), INTERVAL -3 DAY),
        '客户改为自行处理', 0, 0, 0, NULL, 0, 2, DATE_ADD(NOW(), INTERVAL -3 DAY)),
       (8, 'SO20260101000008', '分拣终端急停按钮失灵', '急停按钮按下无响应，存在安全隐患', 1, 2, 1,
        'URGENT', 'PENDING_ACCEPT', 'PHONE', '张伟', '13910000001', 4,
        DATE_ADD(NOW(), INTERVAL -20 HOUR), DATE_ADD(NOW(), INTERVAL -2 DAY),
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        0, 0, 0, NULL, 0, 2, DATE_ADD(NOW(), INTERVAL -2 DAY))
ON DUPLICATE KEY UPDATE title                = VALUES(title),
                        description          = VALUES(description),
                        customer_id          = VALUES(customer_id),
                        device_id            = VALUES(device_id),
                        fault_category_id    = VALUES(fault_category_id),
                        priority             = VALUES(priority),
                        status               = VALUES(status),
                        assignee_id          = VALUES(assignee_id),
                        deadline_time        = VALUES(deadline_time),
                        confirm_time         = VALUES(confirm_time),
                        closed_time          = VALUES(closed_time),
                        cancel_reason        = VALUES(cancel_reason),
                        handle_hours         = VALUES(handle_hours);

-- 工单分派记录（仅保留有效分派，历史流转见处理记录）
INSERT INTO ticket_assignment (id, ticket_id, from_user_id, to_user_id, assign_type, reason, active_flag, create_by, create_time)
VALUES (1, 4, NULL, 4, 'ASSIGN', '就近安排工程师', 1, 2, DATE_ADD(NOW(), INTERVAL -6 HOUR)),
       (2, 8, NULL, 4, 'ASSIGN', '紧急工单优先处理', 1, 2, DATE_ADD(NOW(), INTERVAL -2 DAY))
ON DUPLICATE KEY UPDATE to_user_id = VALUES(to_user_id),
                        reason     = VALUES(reason),
                        active_flag = VALUES(active_flag);

-- 工单处理记录（演示主流程留痕）
INSERT INTO ticket_process_record (id, ticket_id, operator_id, operator_name, operator_role, action, from_status,
                                   to_status, content, used_hours, create_time)
VALUES (1, 1, 2, '客服一号', 'ROLE_SERVICE', 'CREATE', NULL, 'PENDING_ASSIGN', '工单创建成功，等待分派', NULL,
        DATE_ADD(NOW(), INTERVAL -12 DAY)),
       (2, 1, 2, '客服一号', 'ROLE_SERVICE', 'ASSIGN', 'PENDING_ASSIGN', 'PENDING_ACCEPT', '分派给维修工程师一号', NULL,
        DATE_ADD(NOW(), INTERVAL -12 DAY)),
       (3, 1, 3, '维修工程师一号', 'ROLE_ENGINEER', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', '工程师接单，开始处理', NULL,
        DATE_ADD(NOW(), INTERVAL -11 DAY)),
       (4, 1, 3, '维修工程师一号', 'ROLE_ENGINEER', 'PROCESS', 'PROCESSING', 'PROCESSING', '更换电源模块并复测通过', 4.00,
        DATE_ADD(NOW(), INTERVAL -11 DAY) + INTERVAL 4 HOUR),
       (5, 1, 3, '维修工程师一号', 'ROLE_ENGINEER', 'COMPLETE', 'PROCESSING', 'PENDING_CONFIRM', '设备已恢复正常运行，产线复机', 6.00,
        DATE_ADD(NOW(), INTERVAL -11 DAY) + INTERVAL 6 HOUR),
       (6, 1, 5, '客户联系人一', 'ROLE_CUSTOMER', 'CONFIRM', 'PENDING_CONFIRM', 'COMPLETED', '客户确认服务结果', NULL,
        DATE_ADD(NOW(), INTERVAL -10 DAY)),
       (7, 2, 2, '客服一号', 'ROLE_SERVICE', 'CREATE', NULL, 'PENDING_ASSIGN', '工单创建成功，等待分派', NULL,
        DATE_ADD(NOW(), INTERVAL -3 DAY)),
       (8, 2, 2, '客服一号', 'ROLE_SERVICE', 'ASSIGN', 'PENDING_ASSIGN', 'PENDING_ACCEPT', '分派给维修工程师二号', NULL,
        DATE_ADD(NOW(), INTERVAL -3 DAY)),
       (9, 2, 4, '维修工程师二号', 'ROLE_ENGINEER', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', '工程师接单，开始处理', NULL,
        DATE_ADD(NOW(), INTERVAL -3 DAY)),
       (10, 2, 4, '维修工程师二号', 'ROLE_ENGINEER', 'PROCESS', 'PROCESSING', 'PROCESSING', '重新标定视觉模型参数', 8.00,
        DATE_ADD(NOW(), INTERVAL -2 DAY)),
       (11, 2, 4, '维修工程师二号', 'ROLE_ENGINEER', 'REOPEN', 'PENDING_CONFIRM', 'PROCESSING', '退回重做：客户反馈误报仍偏高', NULL,
        DATE_ADD(NOW(), INTERVAL -2 DAY)),
       (12, 4, 2, '客服一号', 'ROLE_SERVICE', 'CREATE', NULL, 'PENDING_ASSIGN', '工单创建成功，等待分派', NULL,
        DATE_ADD(NOW(), INTERVAL -8 HOUR))
ON DUPLICATE KEY UPDATE content    = VALUES(content),
                        action     = VALUES(action),
                        to_status  = VALUES(to_status),
                        operator_id = VALUES(operator_id);

-- 演示工单使用了 2026-01-01 前缀，同步初始化该日期的序列号，避免与真实建单编号冲突
INSERT INTO ticket_sequence (seq_date, current_value) VALUES ('2026-01-01', 8)
ON DUPLICATE KEY UPDATE current_value = GREATEST(current_value, VALUES(current_value));
