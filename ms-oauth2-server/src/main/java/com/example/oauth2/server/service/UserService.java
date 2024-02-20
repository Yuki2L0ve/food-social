package com.example.oauth2.server.service;

import com.example.commons.model.domain.SignInIdentity;
import com.example.commons.model.pojo.Diners;
import com.example.commons.utils.AssertUtil;
import com.example.oauth2.server.mapper.DinersMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 登录校验
 */
@Service
public class UserService implements UserDetailsService {

    @Resource
    private DinersMapper dinersMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AssertUtil.isNotEmpty(s, "请输入用户名");
        Diners diners = dinersMapper.selectByAccountInfo(s);
        if (diners == null) {
            throw new UsernameNotFoundException("用户名或密码错误，请重新输入");
        }
        // 初始化登录认证对象
        SignInIdentity signInIdentity = new SignInIdentity();
        // 拷贝属性  将diners中的属性值 拷贝到 signInIdentity中的属性值
        BeanUtils.copyProperties(diners, signInIdentity);
        return signInIdentity;
    }
}

