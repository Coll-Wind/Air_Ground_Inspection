package com.agi.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具 - 生成与校验登录令牌(HS256)
 */
@Component
public class JwtUtil {

    @Value("${auth.secret}")
    private String secret;

    @Value("${auth.expire-hours:12}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token,subject 为用户名 */
    public String generate(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireHours * 3600_000L))
                .signWith(key())
                .compact();
    }

    /** 校验 token,合法返回用户名,非法/过期抛异常 */
    public String verify(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
