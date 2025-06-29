package com.example.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.model.Block;

@Component
public class Server implements ApplicationRunner {
    @Autowired
    private BlockProcessorService blockProcessorService;

    @Autowired
    private ClientService clientService;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    private final int f = 1;

    public void start() {
        Thread thread = new Thread(new connection());
        thread.start();
    }

    private class connection implements Runnable {
        @Override
        public void run() {
            try (Selector selector = Selector.open();
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

                serverSocketChannel.socket().bind(new InetSocketAddress(7000));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("app启动 " + ":" + 7000);

                while (true) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();

                        if (key.isAcceptable()) {
                            handleAccept(key, selector);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                        keyIterator.remove();
                    }
                }
            } catch (IOException e) {
                System.out.println("监听异常" + e.getMessage());
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverchannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverchannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("客户端已接受连接来自: " + clientChannel.getRemoteAddress());
    }

    // private void handleRead(SelectionKey key) throws IOException {
    // SocketChannel socketChannel = (SocketChannel) key.channel();
    // ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
    // int bytesRead = socketChannel.read(buffer);

    // if (bytesRead > 0) {
    // buffer.flip();
    // byte[] data = new byte[buffer.remaining()];
    // buffer.get(data);
    // buffer.clear();
    // String message = new String(data);
    // System.out.println("接收 " + socketChannel.getRemoteAddress() + ": " +
    // message);
    // // 接收到了消息
    // // 解析所有requestId
    // // 解析区块数据
    // Block block = JSON.parseObject(message, Block.class);
    // // 处理区块数据，保存到数据库
    // blockProcessorService.processConsensusBlock(block);
    // Set<String> ids = parseRequestIds(message);
    // ids.forEach(requestId -> {
    // // 原子增加计数器（正确返回AtomicInteger）
    // AtomicInteger count = requestCounts.compute(requestId,
    // (k, v) -> (v == null) ? new AtomicInteger(1) : new
    // AtomicInteger(v.incrementAndGet()));

    // // 获取当前计数值
    // int currentCount = count.get();
    // // 达到f+1个响应时触发
    // if (currentCount >= f + 1) {
    // System.out.println("达成共识: " + requestId);
    // clientService.completeRequest(requestId);
    // requestCounts.remove(requestId); // 清理计数器
    // }

    // });
    // socketChannel.close();
    // }
    // }
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.clear();
            String message = new String(data);
            System.out.println("接收 " + socketChannel.getRemoteAddress() + ": " + message);

            Block block = null;
            try {
                block = JSON.parseObject(message, Block.class);
            } catch (Exception e) {
                System.err.println("解析区块时出错: " + e.getMessage());
                socketChannel.close();
                return;
            }

            if (block == null) {
                System.err.println("解析区块失败，消息内容无效");
                socketChannel.close();
                return;
            }

            // 处理区块数据
            blockProcessorService.processConsensusBlock(block);

            Set<String> ids = parseRequestIds(message);
            ids.forEach(requestId -> {
                // 新增过滤逻辑：如果请求已完成，直接跳过
                if (clientService.isRequestCompleted(requestId)) {
                    System.out.println("请求 " + requestId + " 已处理，跳过");
                    return;
                }

                AtomicInteger count = requestCounts.compute(requestId,
                        (k, v) -> (v == null) ? new AtomicInteger(1) : new AtomicInteger(v.incrementAndGet()));

                int currentCount = count.get();
                if (currentCount >= f + 1) {
                    System.out.println("达成共识: " + requestId);
                    clientService.completeRequest(requestId);
                    requestCounts.remove(requestId);
                }
            });

            socketChannel.close();
        } else if (bytesRead == -1) {
            socketChannel.close();
        }
    }

    // // parseRequestIds方法
    // private Set<String> parseRequestIds(String response) {
    // Set<String> requestIds = new HashSet<>();
    // try {
    // JSONObject block = JSON.parseObject(response);
    // JSONArray blockInfo = block.getJSONArray("blockInfo");
    // if (blockInfo != null) {
    // for (int i = 0; i < blockInfo.size(); i++) {
    // JSONObject entry = blockInfo.getJSONObject(i);
    // String requestId = entry.getString("requestId");
    // if (requestId != null) {
    // requestIds.add(requestId);
    // }
    // }
    // }
    // } catch (Exception e) {
    // System.err.println("解析响应失败: " + e.getMessage());
    // }
    // return requestIds;
    // }
    // 在parseRequestIds方法中添加错误日志
    private Set<String> parseRequestIds(String response) {
        Set<String> requestIds = new HashSet<>();
        try {
            JSONObject block = JSON.parseObject(response);
            JSONArray blockInfo = block.getJSONArray("blockInfo");
            if (blockInfo != null) {
                for (int i = 0; i < blockInfo.size(); i++) {
                    JSONObject entry = blockInfo.getJSONObject(i);
                    String requestId = entry.getString("requestId");
                    if (requestId != null)
                        requestIds.add(requestId);
                    else
                        System.err.println("条目中缺少requestId: " + entry);
                }
            }
        } catch (Exception e) {
            System.err.println("解析响应失败: " + e.getMessage());
            System.err.println("原始响应内容: " + response); // 记录原始消息
        }
        return requestIds;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        start();
    }
}
