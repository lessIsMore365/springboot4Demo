package org.example.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private Integer age;

    private String roles;

    private Boolean enabled;

    private Boolean accountNonLocked;

    private Boolean accountNonExpired;

    private Boolean credentialsNonExpired;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime lastLoginTime;

    private String remark;
}