package com.agi.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录认证接口
 * 账号配置于 application.yml(auth.username / auth.password)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${auth.username}")
    private String username;

    @Value("${auth.password}")
    private String password;

    /** 登录:校验账号密码,签发 JWT */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String u = body.getOrDefault("username", "");
        String p = body.getOrDefault("password", "");
        if (!username.equals(u) || !password.equals(p)) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        return ResponseEntity.ok(Map.of(
                "token", jwtUtil.generate(u),
                "username", u
        ));
    }
}
