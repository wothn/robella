package org.elmo.robella.config;

import cn.dev33.satoken.stp.StpInterface;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本项目暂时不使用权限码，返回空集合
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 根据loginId查询用户角色
        Long userId = (Long) loginId;
        var user = userService.getUserById(userId);
        List<String> roles = new ArrayList<>();
        if (user != null && user.getRole() != null) {
            roles.add(user.getRole());
        } else {
            // 默认角色
            roles.add(Role.USER.getValue());
        }
        return roles;
    }
}