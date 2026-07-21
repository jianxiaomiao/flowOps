-- FlowOps Postman 测试前置数据，只能在开发数据库执行。
-- 根据当前 flowops_dev.sql：ACME=tenant 1、xiaomiao=user 1、TENANT_ADMIN=role 2。

SET @tenant_id = (SELECT id FROM tenant WHERE tenant_code = 'ACME' LIMIT 1);
SET @admin_user_id = (SELECT id FROM user_account
                      WHERE tenant_id = @tenant_id AND username = 'xiaomiao' LIMIT 1);
SET @admin_role_id = (SELECT id FROM role
                      WHERE tenant_id = @tenant_id AND role_code = 'TENANT_ADMIN' LIMIT 1);

-- 删除错误的 user_id=0 关系，再补上真正管理员关系。
DELETE FROM user_role WHERE tenant_id = @tenant_id AND user_id = 0;
INSERT IGNORE INTO user_role(tenant_id, user_id, role_id, assigned_by, assigned_at)
VALUES(@tenant_id, @admin_user_id, @admin_role_id, @admin_user_id, NOW(3));

-- 企业管理员拥有全部非 PLATFORM 权限。
INSERT IGNORE INTO role_permission(tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @admin_role_id, id, @admin_user_id, NOW(3)
FROM permission
WHERE status = 'ACTIVE' AND (scope IS NULL OR scope <> 'PLATFORM');

SELECT @tenant_id AS tenant_id, @admin_user_id AS admin_user_id, @admin_role_id AS admin_role_id;
