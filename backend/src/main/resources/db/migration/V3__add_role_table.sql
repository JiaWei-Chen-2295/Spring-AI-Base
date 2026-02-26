-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码(如ADMIN,USER)',
    role_name   VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status      TINYINT DEFAULT 1,
    deleted     TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 初始化角色
INSERT INTO role (role_code, role_name, description) VALUES 
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('USER', '普通用户', '普通用户，基础权限');
