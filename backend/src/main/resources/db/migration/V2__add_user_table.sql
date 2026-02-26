-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password     VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    name         VARCHAR(50) NOT NULL COMMENT '姓名',
    gender       TINYINT DEFAULT NULL COMMENT '性别 1男 2女',
    birthday     DATE DEFAULT NULL COMMENT '出生日期',
    phone        VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email        VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    address      VARCHAR(200) DEFAULT NULL COMMENT '地址',
    avatar       VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    status       TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    deleted      TINYINT DEFAULT 0,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
