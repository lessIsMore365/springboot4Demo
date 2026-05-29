package org.example.service;

import java.util.List;
import java.util.Map;

public interface OnlineUserService {

    /** 获取当前在线用户列表 */
    List<Map<String, Object>> getOnlineUsers();

    /** 强制下线（删除 token） */
    void forceLogout(String authorizationId);
}
