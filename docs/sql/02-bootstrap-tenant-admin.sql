-- 首次启用登录认证时使用一次：把“已经存在”的用户设为租户管理员。
-- 先修改下面两个值，再在 Navicat 执行。脚本不会创建或修改密码。
SET @tenant_code = 'ACME';
SET @admin_username = 'admin';

SET @tenant_id = (SELECT id FROM tenant WHERE tenant_code = @tenant_code AND status = 'ACTIVE' LIMIT 1);
SET @admin_user_id = (SELECT id FROM user_account
                      WHERE tenant_id = @tenant_id AND username = LOWER(@admin_username) AND status = 'ACTIVE' LIMIT 1);

INSERT INTO role(tenant_id, role_code, role_name, description, built_in, status,
                 created_at, created_by, updated_at, updated_by, version)
VALUES(@tenant_id, 'TENANT_ADMIN', '企业管理员', '租户内全部管理权限', 1, 'ACTIVE',
       NOW(3), @admin_user_id, NOW(3), @admin_user_id, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ACTIVE', updated_at = NOW(3);

SET @admin_role_id = (SELECT id FROM role
                      WHERE tenant_id = @tenant_id AND role_code = 'TENANT_ADMIN' LIMIT 1);

INSERT IGNORE INTO role_permission(tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @admin_role_id, id, @admin_user_id, NOW(3)
FROM permission
WHERE status = 'ACTIVE' AND (scope IS NULL OR scope <> 'PLATFORM');

INSERT IGNORE INTO user_role(tenant_id, user_id, role_id, assigned_by, assigned_at)
VALUES(@tenant_id, @admin_user_id, @admin_role_id, @admin_user_id, NOW(3));

SELECT @tenant_id AS tenant_id, @admin_user_id AS admin_user_id, @admin_role_id AS admin_role_id;
