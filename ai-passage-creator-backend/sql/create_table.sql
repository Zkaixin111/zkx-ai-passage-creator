# 数据库初始化

-- 创建库
create database if not exists ai_passage;

-- 切换库
use ai_passage;

-- 用户表（含会员与配额字段，全新建库时直接包含）
create table if not exists user
(
    id             bigint auto_increment comment 'id' primary key,
    userAccount    varchar(256)                            not null comment '账号',
    userPassword   varchar(512)                            not null comment '密码',
    userName       varchar(256)                            null comment '用户昵称',
    userAvatar     varchar(1024)                           null comment '用户头像',
    userProfile    varchar(512)                            null comment '用户简介',
    userRole       varchar(256) default 'user'             not null comment '用户角色：user/admin',
    vipType        varchar(32)  default 'NONE'             not null comment '会员类型：NONE免费/PERMANENT永久VIP(预留MONTHLY/YEARLY限时)',
    vipStartTime   datetime                                null comment '成为会员时间',
    vipExpireTime  datetime                                null comment 'VIP到期时间(永久会员为NULL表示长期有效)',
    quotaTotal     int          default 5                  not null comment '创作配额总数(免费用户5次，VIP/管理员不限)',
    quotaUsed      int          default 0                  not null comment '已使用创作配额',
    editTime       datetime     default CURRENT_TIMESTAMP  not null comment '编辑时间',
    createTime     datetime     default CURRENT_TIMESTAMP  not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint      default 0                  not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 初始化数据（INSERT IGNORE：存量库已存在相同主键时跳过，避免批处理中断）
-- 密码是 12345678（MD5 加密 + 盐值 yupi）
INSERT IGNORE INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole) VALUES
(1, 'admin', 'b0ae9cc0c38011f4e6e0ed7db21bbf8a', '管理员', 'https://www.codefather.cn/logo.png', '系统管理员', 'admin'),
(2, 'user', 'b0ae9cc0c38011f4e6e0ed7db21bbf8a', '普通用户', 'https://www.codefather.cn/logo.png', '我是一个普通用户', 'user'),
(3, 'test', 'b0ae9cc0c38011f4e6e0ed7db21bbf8a', '测试账号', 'https://www.codefather.cn/logo.png', '这是一个测试账号', 'user');

-- 文章表
create table if not exists article
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '任务ID（UUID）',
    userId          bigint                             not null comment '用户ID',
    topic           varchar(500)                       not null comment '选题',
    mainTitle       varchar(200)                       null comment '主标题',
    subTitle        varchar(300)                       null comment '副标题',
    outline         json                               null comment '大纲（JSON格式）',
    content         text                               null comment '正文（Markdown格式）',
    fullContent     text                               null comment '完整图文（Markdown格式，含配图）',
    coverImage      varchar(512)                       null comment '封面图 URL',
    images          json                               null comment '配图列表（JSON数组）',
    status          varchar(20) default 'PENDING'      not null comment '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    errorMessage    text                               null comment '错误信息',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    completedTime   datetime                           null comment '完成时间',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint     default 0              not null comment '是否删除',
    UNIQUE KEY uk_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    INDEX idx_userId_status (userId, status)
) comment '文章表' collate = utf8mb4_unicode_ci;

-- 文章生成模块扩展字段（幂等：列不存在才加，可重复执行）
DROP PROCEDURE IF EXISTS sp_upgrade_article;

DELIMITER $$
CREATE PROCEDURE sp_upgrade_article()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'userDescription') THEN
        ALTER TABLE article ADD COLUMN userDescription varchar(500) NULL COMMENT '用户补充描述' AFTER topic;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'titleOptions') THEN
        ALTER TABLE article ADD COLUMN titleOptions json NULL COMMENT '标题方案列表(JSON)' AFTER subTitle;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'phase') THEN
        ALTER TABLE article ADD COLUMN phase varchar(32) NULL COMMENT '当前阶段' AFTER status;
    END IF;
END$$
DELIMITER ;

CALL sp_upgrade_article();
DROP PROCEDURE IF EXISTS sp_upgrade_article;


-- =====================================================================
-- VIP 会员体系 + 微信/支付宝支付（增量段，在已存在的库上执行一次即可）
-- 说明：
--   1) 若提示 "Duplicate column name" / "Column 'xxx' already exists"，说明字段已存在，忽略即可；
--   2) 若此前执行过旧版 Stripe 会员 SQL（含 vipTime 列、旧 payment_record），
--      下方会自动清理 vipTime 列；旧 payment_record 表如需重建请先手动 DROP 或改表名；
--   3) 幂等设计说明见各唯一索引注释。
-- =====================================================================

-- 0. 清理旧版 Stripe 会员字段 vipTime + 1. user 表扩展会员/配额字段
--    用存储过程逐列判断"列不存在才加"，整段可重复执行（幂等）
DROP PROCEDURE IF EXISTS sp_upgrade_user_vip;

DELIMITER $$
CREATE PROCEDURE sp_upgrade_user_vip()
BEGIN
    -- 清理旧版 Stripe 字段（存在才删）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'vipTime') THEN
        ALTER TABLE user DROP COLUMN vipTime;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'vipType') THEN
        ALTER TABLE user ADD COLUMN vipType varchar(32) DEFAULT 'NONE' NOT NULL COMMENT '会员类型：NONE免费/PERMANENT永久VIP(预留MONTHLY/YEARLY限时)';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'vipStartTime') THEN
        ALTER TABLE user ADD COLUMN vipStartTime datetime NULL COMMENT '成为会员时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'vipExpireTime') THEN
        ALTER TABLE user ADD COLUMN vipExpireTime datetime NULL COMMENT 'VIP到期时间(永久会员为NULL表示长期有效)';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'quotaTotal') THEN
        ALTER TABLE user ADD COLUMN quotaTotal int DEFAULT 5 NOT NULL COMMENT '创作配额总数(免费用户5次，VIP/管理员不限)';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'ai_passage' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'quotaUsed') THEN
        ALTER TABLE user ADD COLUMN quotaUsed int DEFAULT 0 NOT NULL COMMENT '已使用创作配额';
    END IF;
END$$
DELIMITER ;

CALL sp_upgrade_user_vip();
DROP PROCEDURE IF EXISTS sp_upgrade_user_vip;

-- 2. 支付订单表（微信 Native/JSAPI + 支付宝，双通道统一记录）
--    幂等设计：
--      - uk_orderNo 唯一：同一业务订单只能存在一条，防重复下单
--      - uk_channelOrderNo 唯一（允许 NULL）：同一渠道支付单号(微信 transaction_id / 支付宝 trade_no)只入账一次，
--        配合回调处理时"先查后更/INSERT ON DUPLICATE"实现回调幂等
--      - 状态机：PENDING(待支付) -> PAID(已支付) 单向流转，已 PAID 的重复回调直接忽略
CREATE TABLE IF NOT EXISTS payment_record (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    orderNo          VARCHAR(64)    NOT NULL COMMENT '业务订单号(幂等唯一，格式建议: 时间戳+随机串)',
    userId           BIGINT         NOT NULL COMMENT '用户ID',
    channel          VARCHAR(16)    NOT NULL COMMENT '支付渠道：WECHAT/ALIPAY',
    productType      VARCHAR(32)    NOT NULL COMMENT '商品类型：VIP_PERMANENT(永久VIP)',
    subject          VARCHAR(128)   NULL COMMENT '商品标题，如：VIP 永久会员',
    amount           DECIMAL(10,2)  NOT NULL COMMENT '实付金额（元）',
    currency         VARCHAR(8)     DEFAULT 'CNY' NOT NULL COMMENT '货币',
    status           VARCHAR(32)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待支付/PAID已支付/FAILED失败/CLOSED关闭/REFUNDED已退款',
    channelOrderNo   VARCHAR(64)    NULL COMMENT '渠道单号(微信transaction_id/支付宝trade_no)，幂等唯一',
    prepayId         VARCHAR(128)   NULL COMMENT '微信预支付交易会话标识(支付宝不用)',
    openid           VARCHAR(128)   NULL COMMENT '微信JSAPI支付openid(微信Native/支付宝不用)',
    notifyData       TEXT           NULL COMMENT '支付回调原始报文JSON(排查对账用)',
    payTime          DATETIME       NULL COMMENT '支付成功时间',
    refundTime       DATETIME       NULL COMMENT '退款时间',
    refundReason     VARCHAR(512)   NULL COMMENT '退款原因',
    createTime       DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
    isDelete         TINYINT        DEFAULT 0 NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_orderNo (orderNo),
    UNIQUE KEY uk_channelOrderNo (channelOrderNo),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT '支付订单表(微信/支付宝)' COLLATE = utf8mb4_unicode_ci;


-- 智能体执行日志表
create table if not exists agent_log
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '任务ID',
    agentName       varchar(50)                        not null comment '智能体名称',
    startTime       datetime                           not null comment '开始时间',
    endTime         datetime                           null comment '结束时间',
    durationMs      int                                null comment '耗时（毫秒）',
    status          varchar(20)                        not null comment '状态：SUCCESS/FAILED',
    errorMessage    text                               null comment '错误信息',
    prompt          text                               null comment '使用的Prompt',
    inputData       json                               null comment '输入数据（JSON格式）',
    outputData      json                               null comment '输出数据（JSON格式）',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint     default 0              not null comment '是否删除',
    INDEX idx_taskId (taskId),
    INDEX idx_agentName (agentName),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) comment '智能体执行日志表' collate = utf8mb4_unicode_ci;
