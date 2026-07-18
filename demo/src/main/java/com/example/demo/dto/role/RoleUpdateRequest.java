package com.example.demo.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改自定义角色资料时允许客户端提交的数据。 */
@Data
public class RoleUpdateRequest {

    @NotNull(message = "角色 id 不能为空")
    private Long id;

    @NotBlank(message = "roleName 不能为空")
    @Size(max = 64, message = "roleName 长度不能超过 64")
    private String roleName;

    @Size(max = 255, message = "description 长度不能超过 255")
    private String description;
}
