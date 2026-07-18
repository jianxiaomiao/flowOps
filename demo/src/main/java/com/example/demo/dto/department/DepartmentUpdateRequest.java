package com.example.demo.dto.department;

import lombok.Data;

/**
 * 修改部门基本资料时客户端允许提交的数据。
 *
 * <p>tenantId、departmentCode、parentId、status 和 leaderUserId 分别由专门的业务操作修改。</p>
 */
@Data
public class DepartmentUpdateRequest {

    private Long id;

    private String departmentName;

    private Integer sortOrder;
}
