package com.example.commons.model.pojo;

import com.example.commons.model.base.BaseModel;
import lombok.Getter;
import lombok.Setter;

/**
 * 食客实体类
 */
@Getter
@Setter
public class Diners extends BaseModel {
    private Integer id;         // 主键
    private String username;    // 用户名
    private String nickname;    // 昵称
    private String password;    // 密码
    private String phone;       // 手机号
    private String email;       // 邮箱
    private String avatarUrl;   // 头像
    private String roles;       // 角色
}
