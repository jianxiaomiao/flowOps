package com.example.demo.dto.department;

import lombok.Data;

/**
 * 部门分页查询条件。
 */
@Data
public class DepartmentPageQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 查询指定父部门的直接子部门；为空时不限制层级。 */
    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private String status;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
