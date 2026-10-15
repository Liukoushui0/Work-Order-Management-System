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
