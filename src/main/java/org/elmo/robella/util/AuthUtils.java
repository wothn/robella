package org.elmo.robella.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elmo.robella.model.common.Role;



public class AuthUtils {
    private static final List<String> PUBLIC_PATHS = new ArrayList<>();
    private static final Map<String, Role> PATH_ROLE_MAPPING = new HashMap<>();

    static {
        // 公开路径 - 无需认证
        PUBLIC_PATHS.add("/api/users/login"); // 登录
        PUBLIC_PATHS.add("/api/users/register"); // 注册
        PUBLIC_PATHS.add("/api/users/refresh"); // 刷新
        PUBLIC_PATHS.add("/api/health"); // 健康检查
        PUBLIC_PATHS.add("/api/oauth/github"); // GitHub OAuth
        PUBLIC_PATHS.add("/actuator"); // Actuator
        PUBLIC_PATHS.add("/v1"); // OpenAI 兼容路径
        PUBLIC_PATHS.add("/anthropic/v1"); // Anthropic 兼容路径


        // 初始化路径角色映射
        // Provider 路径
        PATH_ROLE_MAPPING.put("/api/providers", Role.ROOT);
    }

    public static boolean isPublicEndpoint(String path) {

        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return false;
    }

}