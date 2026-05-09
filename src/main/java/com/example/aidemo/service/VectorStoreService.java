package com.example.aidemo.service;

import com.example.aidemo.utils.EmbeddingUtil;
import com.example.aidemo.utils.TextChunkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VectorStoreService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Autowired
    private TextChunkUtil textChunkUtil;

    private static final String VECTOR_PREFIX = "vector:doc:";
    private static final String CONTENT_PREFIX = "vector:content:";

    /**
     * 保存文档到向量库
     * @param docId 文档ID
     * @param content 文档内容
     */
    public void saveDocument(String docId, String content) {
        List<String> chunks = textChunkUtil.chunkByParagraph(content);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] vector = embeddingUtil.embed(chunk);
            redisTemplate.opsForValue().set(VECTOR_PREFIX + docId + ":" + i, floatToString(vector));
            redisTemplate.opsForValue().set(CONTENT_PREFIX + docId + ":" + i, chunk);
        }
    }

    /**
     * 向量检索
     * @param query 查询语句
     * @param topK 返回前K个结果
     * @return 匹配的文本列表
     */
    public List<String> search(String query, int topK) {
        float[] queryVec = embeddingUtil.embed(query);
        Set<String> keys = redisTemplate.keys(VECTOR_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        
        List<Map.Entry<String, Float>> scores = new ArrayList<>();
        for (String key : keys) {
            String vectorStr = redisTemplate.opsForValue().get(key);
            if (vectorStr != null) {
                float[] v = stringToFloat(vectorStr);
                scores.add(new AbstractMap.SimpleEntry<>(key.substring(VECTOR_PREFIX.length()), 
                        embeddingUtil.cosineSimilarity(queryVec, v)));
            }
        }
        
        // 按相似度降序排列
        scores.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        List<String> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scores.size()); i++) {
            String content = redisTemplate.opsForValue().get(CONTENT_PREFIX + scores.get(i).getKey());
            if (content != null) {
                results.add(content);
            }
        }
        return results;
    }

    private String floatToString(float[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private float[] stringToFloat(String str) {
        String[] parts = str.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
