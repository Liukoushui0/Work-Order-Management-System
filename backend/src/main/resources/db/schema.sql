-- ============================================================================
-- ServiceFlow 企业售后工单管理系统 — 数据库结构脚本
-- 适用：MySQL 8.x，字符集 utf8mb4，引擎 InnoDB
-- 说明：
--   1. 本脚本只负责结构，不插入任何演示数据（演示数据见 db/data.sql）。
--   2. 可重复执行：使用 CREATE TABLE IF NOT EXISTS。
--   3. 逻辑删除约定：主数据表使用 deleted BIGINT DEFAULT 0，删除时写入本行 id，
--      配合唯一索引 (业务唯一列, deleted) 保证"未删除时唯一、删除后可重建"。
--   4. 需要并发保护的业务表使用 version INT 乐观锁字段。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. sys_user 用户表（员工与客户登录账号）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)     NOT NULL COMMENT '登录账号',
    password        VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希，禁止明文',
    real_name       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    avatar          VARCHAR(255)    DEFAULT NULL COMMENT '头像地址',
    customer_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '客户角色账号绑定的客户档案 ID',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    login_fail_count INT            NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_until      DATETIME        DEFAULT NULL COMMENT '锁定截止时间',
    last_login_time DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(45)     DEFAULT NULL COMMENT '最后登录 IP',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted         BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，否则为本行 id',
    create_by       BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username, deleted),
    UNIQUE KEY uk_user_customer (customer_id),
    KEY idx_user_status (status),
    KEY idx_user_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- ----------------------------------------------------------------------------
-- 2. sys_role 角色表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(50)     NOT NULL COMMENT '角色编码，如 ROLE_ADMIN',
    role_name   VARCHAR(50)     NOT NULL COMMENT '角色名称',
    description VARCHAR(255)    DEFAULT NULL COMMENT '角色说明',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    deleted     BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- ----------------------------------------------------------------------------
-- 3. sys_user_role 用户角色关系表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关系表';

-- ----------------------------------------------------------------------------
-- 4. sys_permission 权限表（菜单/按钮/接口权限）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    perm_code   VARCHAR(80)     NOT NULL COMMENT '权限编码，如 ticket:create',
    perm_name   VARCHAR(80)     NOT NULL COMMENT '权限名称',
    perm_type   VARCHAR(16)     NOT NULL DEFAULT 'API' COMMENT '类型：MENU/BUTTON/API',
    module      VARCHAR(50)     DEFAULT NULL COMMENT '所属模块',
    description VARCHAR(255)    DEFAULT NULL COMMENT '说明',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY idx_perm_module (module)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表';

-- ----------------------------------------------------------------------------
-- 5. sys_role_permission 角色权限关系表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    perm_id     BIGINT UNSIGNED NOT NULL COMMENT '权限 ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    KEY idx_role_perm_perm (perm_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色权限关系表';

-- ----------------------------------------------------------------------------
-- 6. fault_category 故障分类表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fault_category (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_code VARCHAR(50)     NOT NULL COMMENT '分类编码',
    category_name VARCHAR(50)     NOT NULL COMMENT '分类名称',
    parent_id     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级 ID，0 表示顶级',
    description   VARCHAR(255)    DEFAULT NULL COMMENT '说明',
    sort          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    deleted       BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (category_code, deleted),
    KEY idx_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '故障分类表';

-- ----------------------------------------------------------------------------
-- 7. sys_config 系统配置表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(80)     NOT NULL COMMENT '配置键',
    config_value VARCHAR(500)    NOT NULL COMMENT '配置值',
    config_name  VARCHAR(80)     NOT NULL COMMENT '配置名称',
    config_group VARCHAR(50)     NOT NULL DEFAULT 'default' COMMENT '配置分组',
    value_type   VARCHAR(16)     NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING/NUMBER/BOOLEAN/JSON',
    remark       VARCHAR(255)    DEFAULT NULL COMMENT '说明',
    status       TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_config_group (config_group)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表';

-- ----------------------------------------------------------------------------
-- 8. customer 客户表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    customer_code  VARCHAR(32)     NOT NULL COMMENT '客户编号',
    customer_name  VARCHAR(100)    NOT NULL COMMENT '客户名称',
    contact_name   VARCHAR(50)     NOT NULL COMMENT '联系人',
    contact_phone  VARCHAR(20)     NOT NULL COMMENT '联系电话',
    contact_email  VARCHAR(100)    DEFAULT NULL COMMENT '联系邮箱',
    province       VARCHAR(50)     DEFAULT NULL COMMENT '省份',
    city           VARCHAR(50)     DEFAULT NULL COMMENT '城市',
    address        VARCHAR(255)    DEFAULT NULL COMMENT '详细地址',
    industry       VARCHAR(50)     DEFAULT NULL COMMENT '所属行业',
    level          VARCHAR(8)      NOT NULL DEFAULT 'B' COMMENT '客户等级：S/A/B/C',
    source         VARCHAR(16)     DEFAULT NULL COMMENT '来源：PHONE/WEB/WECHAT/EMAIL',
    status         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
    remark         VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    deleted        BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by      BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (customer_code, deleted),
    KEY idx_customer_name (customer_name),
    KEY idx_customer_status (status),
    KEY idx_customer_phone (contact_phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客户表';

-- ----------------------------------------------------------------------------
-- 9. device 设备表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS device (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_code          VARCHAR(32)     NOT NULL COMMENT '设备编号',
    device_name          VARCHAR(100)    NOT NULL COMMENT '设备名称',
    device_type          VARCHAR(50)     DEFAULT NULL COMMENT '设备类型',
    brand                VARCHAR(50)     DEFAULT NULL COMMENT '品牌',
    model                VARCHAR(50)     DEFAULT NULL COMMENT '型号',
    sn_no                VARCHAR(64)     DEFAULT NULL COMMENT '设备序列号',
    customer_id          BIGINT UNSIGNED NOT NULL COMMENT '所属客户 ID',
    purchase_date        DATE            DEFAULT NULL COMMENT '购买日期',
    warranty_expire_date DATE            DEFAULT NULL COMMENT '保修截止日期',
    install_address      VARCHAR(255)    DEFAULT NULL COMMENT '安装地址',
    status               TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 在用，0 停用，2 报废',
    remark               VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    deleted              BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by            BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by            BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code, deleted),
    UNIQUE KEY uk_device_sn (sn_no, deleted),
    KEY idx_device_customer (customer_id),
    KEY idx_device_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客户设备档案表';

-- ----------------------------------------------------------------------------
-- 10. service_ticket 工单表（核心业务表）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_ticket (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no         VARCHAR(32)     NOT NULL COMMENT '工单编号，唯一',
    title             VARCHAR(200)    NOT NULL COMMENT '工单标题',
    description       TEXT            DEFAULT NULL COMMENT '故障描述',
    customer_id       BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    device_id         BIGINT UNSIGNED NOT NULL COMMENT '设备 ID',
    fault_category_id BIGINT UNSIGNED DEFAULT NULL COMMENT '故障分类 ID',
    priority          VARCHAR(16)     NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW/MEDIUM/HIGH/URGENT',
    status            VARCHAR(24)     NOT NULL DEFAULT 'PENDING_ASSIGN' COMMENT '状态：PENDING_ASSIGN/PENDING_ACCEPT/PROCESSING/PENDING_CONFIRM/COMPLETED/CANCELLED',
    source            VARCHAR(16)     NOT NULL DEFAULT 'WEB' COMMENT '来源：PHONE/WEB/WECHAT/EMAIL',
    reporter_name     VARCHAR(50)     DEFAULT NULL COMMENT '报修人',
    reporter_phone    VARCHAR(20)     DEFAULT NULL COMMENT '报修人电话',
    assignee_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '当前负责工程师 ID',
    deadline_time     DATETIME        DEFAULT NULL COMMENT 'SLA 截止时间',
    assign_time       DATETIME        DEFAULT NULL COMMENT '最近分派时间',
    accept_time       DATETIME        DEFAULT NULL COMMENT '接单时间',
    start_time        DATETIME        DEFAULT NULL COMMENT '开始处理时间',
    finish_time       DATETIME        DEFAULT NULL COMMENT '提交处理结果时间',
    confirm_time      DATETIME        DEFAULT NULL COMMENT '客户确认时间',
    cancel_time       DATETIME        DEFAULT NULL COMMENT '取消时间',
    closed_time       DATETIME        DEFAULT NULL COMMENT '关闭（完成）时间',
    cancel_reason     VARCHAR(255)    DEFAULT NULL COMMENT '取消原因',
    reject_count      INT             NOT NULL DEFAULT 0 COMMENT '退回次数',
    reopen_count      INT             NOT NULL DEFAULT 0 COMMENT '重新打开次数',
    transfer_count    INT             NOT NULL DEFAULT 0 COMMENT '转派次数',
    handle_hours      DECIMAL(10, 2)  DEFAULT NULL COMMENT '处理总时长（小时）',
    version           INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by         BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by         BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_status_create (status, create_time),
    KEY idx_ticket_assignee_status (assignee_id, status),
    KEY idx_ticket_customer_status (customer_id, status),
    KEY idx_ticket_deadline_status (deadline_time, status),
    KEY idx_ticket_device (device_id),
    KEY idx_ticket_priority (priority)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '售后工单表';

-- ----------------------------------------------------------------------------
-- 11. ticket_assignment 工单分派记录表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_assignment (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id    BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    from_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '原负责人 ID',
    to_user_id   BIGINT UNSIGNED NOT NULL COMMENT '新负责人 ID',
    assign_type  VARCHAR(24)     NOT NULL COMMENT '类型：ASSIGN/TRANSFER/REJECT_TO_POOL',
    reason       VARCHAR(255)    DEFAULT NULL COMMENT '分派/转派/退回原因',
    active_flag  BIGINT          NOT NULL DEFAULT 1 COMMENT '有效标记：1 表示当前有效，否则为本行 id',
    create_by    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_active (ticket_id, active_flag),
    KEY idx_assignment_to_user (to_user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工单分派记录表';

-- ----------------------------------------------------------------------------
-- 12. ticket_process_record 工单处理记录表（状态流转与历史）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_process_record (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    operator_id   BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人 ID，系统操作时为 NULL',
    operator_name VARCHAR(50)     DEFAULT NULL COMMENT '操作人名称（含 SYSTEM）',
    operator_role VARCHAR(50)     DEFAULT NULL COMMENT '操作人角色编码',
    action        VARCHAR(24)     NOT NULL COMMENT '动作：CREATE/ASSIGN/ACCEPT/TRANSFER/REJECT/PROCESS/COMPLETE/CONFIRM/REOPEN/CANCEL/AUTO_CONFIRM',
    from_status   VARCHAR(24)     DEFAULT NULL COMMENT '变更前状态',
    to_status     VARCHAR(24)     DEFAULT NULL COMMENT '变更后状态',
    content       TEXT            DEFAULT NULL COMMENT '处理说明',
    used_hours    DECIMAL(10, 2)  DEFAULT NULL COMMENT '本次处理耗时（小时）',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_record_ticket (ticket_id, create_time),
    KEY idx_record_operator (operator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工单处理记录表';

-- ----------------------------------------------------------------------------
-- 13. ticket_comment 工单评论表（内部备注与客户沟通）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_comment (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '评论人 ID',
    user_type   VARCHAR(16)     NOT NULL DEFAULT 'STAFF' COMMENT '类型：STAFF 员工，CUSTOMER 客户',
    content     VARCHAR(1000)   NOT NULL COMMENT '评论内容',
    is_internal TINYINT         NOT NULL DEFAULT 0 COMMENT '是否内部备注：1 仅员工可见',
    deleted     BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_comment_ticket (ticket_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工单评论表';

-- ----------------------------------------------------------------------------
-- 14. ticket_evaluation 服务评价表（一单一评）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_evaluation (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    customer_id   BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    evaluator_id  BIGINT UNSIGNED NOT NULL COMMENT '评价人用户 ID',
    score         TINYINT         NOT NULL COMMENT '综合评分 1~5',
    satisfaction  VARCHAR(24)     NOT NULL DEFAULT 'SATISFIED' COMMENT '满意度：VERY_SATISFIED/SATISFIED/NEUTRAL/UNSATISFIED/VERY_UNSATISFIED',
    speed_score   TINYINT         DEFAULT NULL COMMENT '响应速度评分 1~5',
    quality_score TINYINT         DEFAULT NULL COMMENT '维修质量评分 1~5',
    attitude_score TINYINT        DEFAULT NULL COMMENT '服务态度评分 1~5',
    tags          VARCHAR(255)    DEFAULT NULL COMMENT '评价标签，逗号分隔',
    content       VARCHAR(1000)   DEFAULT NULL COMMENT '评价内容',
    is_anonymous  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否匿名',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_evaluation_ticket (ticket_id),
    KEY idx_evaluation_customer (customer_id),
    KEY idx_evaluation_create (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '服务评价表';

-- ----------------------------------------------------------------------------
-- 15. ticket_attachment 工单附件表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_attachment (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    file_name     VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    stored_name   VARCHAR(255)    NOT NULL COMMENT '存储文件名（重命名后）',
    file_path     VARCHAR(500)    NOT NULL COMMENT '相对存储目录的路径',
    file_ext      VARCHAR(20)     NOT NULL COMMENT '扩展名',
    file_size     BIGINT          NOT NULL COMMENT '文件大小（字节）',
    content_type  VARCHAR(100)    DEFAULT NULL COMMENT 'MIME 类型',
    sha256        VARCHAR(64)     DEFAULT NULL COMMENT '文件 SHA-256',
    biz_type      VARCHAR(24)     NOT NULL DEFAULT 'FAULT_PHOTO' COMMENT '业务类型：FAULT_PHOTO/REPAIR_PHOTO/REPORT/OTHER',
    uploader_id   BIGINT UNSIGNED NOT NULL COMMENT '上传人用户 ID',
    uploader_type VARCHAR(16)     NOT NULL DEFAULT 'STAFF' COMMENT '上传人类型：STAFF/CUSTOMER',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 已删除',
    deleted       BIGINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_attachment_ticket (ticket_id, deleted),
    KEY idx_attachment_uploader (uploader_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工单附件表';

-- ----------------------------------------------------------------------------
-- 16. sys_operation_log 操作日志表（审计）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人 ID',
    username       VARCHAR(50)     DEFAULT NULL COMMENT '操作人账号',
    module         VARCHAR(50)     NOT NULL COMMENT '模块',
    operation      VARCHAR(100)    NOT NULL COMMENT '操作描述',
    request_method VARCHAR(10)     DEFAULT NULL COMMENT 'HTTP 方法',
    request_uri    VARCHAR(255)    DEFAULT NULL COMMENT '请求路径',
    request_params VARCHAR(2000)   DEFAULT NULL COMMENT '请求参数（已脱敏并截断）',
    response_code  INT             DEFAULT NULL COMMENT '业务响应码',
    success        TINYINT         NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_msg      VARCHAR(500)    DEFAULT NULL COMMENT '错误信息',
    ip             VARCHAR(45)     DEFAULT NULL COMMENT '客户端 IP',
    user_agent     VARCHAR(255)    DEFAULT NULL COMMENT 'User-Agent',
    duration_ms    BIGINT          DEFAULT NULL COMMENT '耗时（毫秒）',
    trace_id       VARCHAR(64)     DEFAULT NULL COMMENT '链路追踪 ID',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_log_create_time (create_time),
    KEY idx_log_user (user_id, create_time),
    KEY idx_log_module (module, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表';
