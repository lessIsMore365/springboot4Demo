package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.OnlineUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor/online")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    /** 获取在线用户列表 */
    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> users = onlineUserService.getOnlineUsers();
        return Map.of(
                "success", true,
                "data", users,
                "total", users.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 强制下线 */
    @DeleteMapping("/{authorizationId}")
    public Map<String, Object> forceLogout(@PathVariable String authorizationId) {
        onlineUserService.forceLogout(authorizationId);
        return Map.of(
                "success", true,
                "message", "已强制下线",
                "timestamp", System.currentTimeMillis()
        );
    }
}
