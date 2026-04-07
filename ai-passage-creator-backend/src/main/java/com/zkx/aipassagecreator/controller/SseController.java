package com.zkx.aipassagecreator.controller;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SseController {
    // 保存所有在线客户端
    private final Map<String, SseEmitter> clientMap = new ConcurrentHashMap<>();

    // 建立连接
    @GetMapping("/sse/connect/{userId}")
    public SseEmitter connect(@PathVariable String userId) {

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();


        // 永不超时
        SseEmitter emitter = new SseEmitter(0L);
        clientMap.put(userId, emitter);

        // 关闭/异常时移除
        emitter.onCompletion(() -> clientMap.remove(userId));
        emitter.onError((e) -> clientMap.remove(userId));
        emitter.onTimeout(() -> clientMap.remove(userId));

        return emitter;
    }

    // 主动推送接口（其他业务调用）
    @PostMapping("/sse/send/{userId}")
    public String sendMsg(@PathVariable String userId, @RequestBody String msg) throws IOException {
        SseEmitter emitter = clientMap.get(userId);
        if (emitter != null) {
            emitter.send(SseEmitter.event().data(msg));
        }
        return "ok";
    }
}