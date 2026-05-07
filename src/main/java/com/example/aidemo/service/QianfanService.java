package com.example.aidemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 百度千帆大模型服务
 * 支持两种认证方式：
 * 1. 新版：只需要 API Key
 * 2. 旧版：API Key + Secret Key
 */
@Service
public class QianfanService {

    // 新版认证：只需要 API Key
    @Value("${qianfan.api-key:}")
    private String apiKey;

    // 旧版认证：需要 Secret Key（可选）
    @Value("${qianfan.secret-key:}")
    private String secretKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 千帆 API 地址
    private static final String AUTH_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String CHAT_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/%s";

    private String accessToken;

    /**
     * 获取 Access Token
     * 支持新版（只有API Key）和旧版（API Key + Secret Key）
     */
    public String getAccessToken() throws IOException {
        if (accessToken != null) {
            return accessToken;
        }

        // 新版认证：直接用 API Key 获取 Token
        if (secretKey == null || secretKey.isEmpty()) {
            // 这种方式可能需要不同的 API，让我先尝试旧版方式
        }

        // 旧版认证方式
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", apiKey)
                .add("client_secret", secretKey)
                .build();

        Request request = new Request.Builder()
                .url(AUTH_URL)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("access_token")) {
                accessToken = json.get("access_token").asText();
                return accessToken;
            } else {
                throw new IOException("获取 Access Token 失败: " + responseBody);
            }
        }
    }

    /**
     * 对话补全 API
     * @param model 模型名称: ernie-bot-turbo, ernie-bot-4
     * @param message 用户消息
     * @return AI 回复
     */
    public String chat(String model, String message) throws IOException {
        String accessToken = getAccessToken();
        String url = String.format(CHAT_URL, model) + "?access_token=" + accessToken;

        // 构建请求体
        String requestBody = String.format(
                "{\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                message.replace("\"", "\\\"")
        );

        RequestBody body = RequestBody.create(
                requestBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("result")) {
                return json.get("result").asText();
            } else if (json.has("error_code")) {
                throw new IOException("API 错误: " + json.get("error_msg").asText());
            } else {
                throw new IOException("未知响应: " + responseBody);
            }
        }
    }

    /**
     * 使用默认模型对话
     */
    public String chat(String message) throws IOException {
        return chat("ernie-bot-turbo", message);
    }
}