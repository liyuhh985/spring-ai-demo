package com.example.aidemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek 大模型服务
 * 使用 OpenAI 兼容 API
 */
@Service
public class DeepSeekService {

    @Value("${deepseek.api-key:}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // DeepSeek API 地址
    private static final String CHAT_URL = "https://api.deepseek.com/v1/chat/completions";

    /**
     * 对话补全
     * @param message 用户消息
     * @return AI 回复
     */
    public String chat(String message) throws IOException {
        // 构建请求体（OpenAI 格式）
        String requestBody = String.format(
                "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7}",
                message.replace("\"", "\\\"")
        );

        RequestBody body = RequestBody.create(
                requestBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(CHAT_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("choices") && json.get("choices").size() > 0) {
                return json.get("choices").get(0).get("message").get("content").asText();
            } else if (json.has("error")) {
                throw new IOException("API 错误: " + json.get("error").get("message").asText());
            } else {
                throw new IOException("未知响应: " + responseBody);
            }
        }
    }
}