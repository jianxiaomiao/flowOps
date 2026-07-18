package com.example.demo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改用户基本资料时允许客户端提交的数据。 */
@Data
public class UserAccountUpdateRequest {

    @NotNull(message = "用户 id 不能为空")
    private Long id;

    @NotNull(message = "departmentId 不能为空")
    private Long departmentId;

    @NotBlank(message = "displayName 不能为空")
    @Size(max = 64, message = "displayName 长度不能超过 64")
    private String displayName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过 32")
    private String phone;
}
