package com.example.aidemo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class EmbeddingUtil {

    @Value("${dashscope.api.key:}")
    private String apiKey;

    private final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    public float[] embed(String text) {
        try {
            // 阿里云百炼要求input是数组格式
            String jsonBody = String.format("{\"input\": [\"%s\"], \"model\": \"text-embedding-v2\"}", text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""));
            System.out.println("请求体: " + jsonBody);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(BASE_URL).addHeader("Content-Type", "application/json").addHeader("Authorization", "Bearer " + apiKey).post(body).build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    System.out.println("错误响应: " + responseBody);
                    throw new RuntimeException("API failed: " + response + " body: " + responseBody);
                }
                return parseEmbeddingResponse(responseBody);
            }
        } catch (Exception e) { throw new RuntimeException("Embedding failed: " + e.getMessage(), e); }
    }

    private float[] parseEmbeddingResponse(String json) throws IOException {
        // 打印响应以便调试
        System.out.println("Embedding响应: " + json);
        JsonNode root = objectMapper.readTree(json);
        
        // 检查是否有错误
        if (root.has("code")) {
            String code = root.get("code").asText();
            String message = root.has("message") ? root.get("message").asText() : "Unknown error";
            throw new RuntimeException("API error: " + code + " - " + message);
        }
        
        JsonNode embedding = root.get("output").get("embeddings").get(0).get("embedding");
        List<Float> list = new ArrayList<>();
        for (JsonNode node : embedding) list.add((float) node.asDouble());
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) result[i] = list.get(i);
        return result;
    }

    public float cosineSimilarity(float[] v1, float[] v2) {
        float dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.length; i++) { dot += v1[i] * v2[i]; n1 += v1[i] * v1[i]; n2 += v2[i] * v2[i]; }
        return (float) (dot / (Math.sqrt(n1) * Math.sqrt(n2) + 1e-10));
    }
}