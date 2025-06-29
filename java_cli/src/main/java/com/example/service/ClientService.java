package com.example.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.example.client.ClientRequest;
import com.example.controller.SseController;

@Service
public class ClientService {

    @Autowired
    private SseController sseController;

    // 新增统计变量
    private final ConcurrentHashMap<String, Long> requestStartTimes = new ConcurrentHashMap<>();

    // 新增成员变量
    private final ConcurrentHashMap<String, Long> pendingRequests = new ConcurrentHashMap<>();
    private final Set<String> completedRequests = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long REQUEST_TIMEOUT_MS = 20000; // 20秒超时

    // 初始化定时任务
    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::checkTimeouts, 0, 10, TimeUnit.SECONDS);
    }

    private ConcurrentHashMap<String, String> requestToUserMap = new ConcurrentHashMap<>();

    public void associateRequestWithUser(String requestId, String username) {
        requestToUserMap.put(requestId, username);
    }

    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > REQUEST_TIMEOUT_MS) {
                System.out.println("请求超时: " + entry.getKey());
                // 这里添加重试逻辑
                // retryRequest(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public void onRequest(ClientRequest clientRequest) {
        String requestId = clientRequest.getRequest().getRequestId();
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        requestStartTimes.put(requestId, startTime);
        // 记录请求开始时间
        pendingRequests.put(clientRequest.getRequest().getRequestId(), System.currentTimeMillis());
        try {
            SocketChannel channel = SocketChannel.open();
            channel.connect(new InetSocketAddress("node1", 7101));
            sendMessage(channel, JSON.toJSONString(clientRequest));
            // totalRequests.incrementAndGet(); // 总请求数+1
        } catch (Exception e) {
            System.out.println("发送消息失败" + e.getMessage());
            requestStartTimes.remove(requestId);
            pendingRequests.remove(requestId);
        }
    }

    // ClientService.java 修改completeRequest方法
    public void completeRequest(String requestId) {
        if (pendingRequests.containsKey(requestId)) {
            pendingRequests.remove(requestId);
            completedRequests.add(requestId); // 新增：记录已完成的请求
            System.out.println("共识达成，取消计时: " + requestId);
            String username = requestToUserMap.remove(requestId);
            if (username != null)
                sseController.sendEvent(username, requestId);
        }
    }

    public boolean isRequestCompleted(String requestId) {
        return completedRequests.contains(requestId);
    }

    // 发送消息到指定连接
    private static boolean sendMessage(SocketChannel channel, String msg) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());

            channel.write(buffer);
            return true;
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
            close(channel);
            return false;
        }
    }

    // 关闭指定连接
    private static void close(SocketChannel channel) {
        try {
            channel.close();
        } catch (Exception e) {
            System.out.println("关闭指定连接 " + e.getMessage());
        }
    }

}
