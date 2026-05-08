package com.example.aidemo.utils;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Embedding 工具类
 * 作用：把文字转成向量 + 计算相似度
 */
@Component
public class EmbeddingUtil {

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 把文字转成向量
     * @param text 输入文本
     * @return 向量数组（float[]）
     */
    public float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.embed(text);
        return response.getResult().getVector();
    }

    /**
     * 批量转成向量
     * @param texts 输入文本列表
     * @return 向量数组列表
     */
    public List<float[]> embed(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embed(texts);
        return response.getResults().stream()
                .map(r -> r.getVector())
                .toList();
    }

    /**
     * 计算余弦相似度
     * @param v1 向量1
     * @param v2 向量2
     * @return 相似度（-1到1，越接近1越相似）
     */
    public float cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }

        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        // 防止除以0
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-10));
    }
}