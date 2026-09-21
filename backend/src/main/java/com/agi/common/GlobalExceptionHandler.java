package com.agi.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理:统一返回 {error: 消息} 结构,避免向前端暴露堆栈
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务类异常(如状态机校验失败)→ 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** 缺少必填参数/请求体 → 400 */
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "请求参数缺失或格式错误"));
    }

    /** 其余未知异常 → 500,记录日志但不暴露堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleServerError(Exception e) {
        log.error("接口异常: ", e);
        return ResponseEntity.internalServerError().body(Map.of("error", "服务异常,请稍后重试"));
    }
}
