-- FlowOps 平台权限初始化数据。
-- 当前尚未接入 Flyway：请在 Navicat 对 flowops_dev 执行一次。
-- 接入 Flyway 后，应迁移到 src/main/resources/db/migration/ 中统一执行。

INSERT INTO permission
    (permission_code, permission_name, resource, action, scope, description, status, created_at, updated_at)
VALUES
    ('ticket:create', '创建工单', 'ticket', 'create', 'OWN', '提交新的工单', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:read', '查看租户全部工单', 'ticket', 'read', 'TENANT', '租户管理员查看全部工单', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:read:own', '查看本人工单', 'ticket', 'read', 'OWN', '查看自己创建的工单', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:read:department', '查看部门工单', 'ticket', 'read', 'DEPARTMENT', '查看本部门负责的工单', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:assign', '分派工单', 'ticket', 'assign', 'DEPARTMENT', '将工单分派给部门或处理人', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:accept', '接单', 'ticket', 'accept', 'OWN', '接收分派给自己的工单', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:update', '处理工单', 'ticket', 'update', 'OWN', '开始处理工单、添加处理过程', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:resolve', '提交解决方案', 'ticket', 'resolve', 'OWN', '提交工单解决方案', 'ACTIVE', NOW(3), NOW(3)),
    ('ticket:confirm', '确认或关闭工单', 'ticket', 'confirm', 'OWN', '确认、驳回或关闭本人创建的工单', 'ACTIVE', NOW(3), NOW(3)),
    ('user:manage', '管理用户', 'user', 'manage', 'TENANT', '管理本租户用户', 'ACTIVE', NOW(3), NOW(3)),
    ('role:manage', '管理角色', 'role', 'manage', 'TENANT', '管理本租户角色与授权关系', 'ACTIVE', NOW(3), NOW(3)),
    ('sla:manage', '管理 SLA', 'sla', 'manage', 'TENANT', '管理本租户 SLA 策略', 'ACTIVE', NOW(3), NOW(3)),
    ('platform:tenant:manage', '平台租户管理', 'tenant', 'manage', 'PLATFORM', '仅平台运营账号管理租户', 'ACTIVE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    scope = VALUES(scope),
    description = VALUES(description),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
