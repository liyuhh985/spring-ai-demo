package com.example.aidemo.utils;

import org.springframework.ai.embedding.EmbeddingModel;
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
    public List<Double> embed(String text) {
        float[] vector = embeddingModel.embed(text);
        return java.util.stream.IntStream.range(0, vector.length)
                .mapToObj(i -> (double) vector[i])
                .toList();
    }

    /**
     * 批量转成向量
     * @param texts 输入文本列表
     * @return 向量数组列表
     */
    public List<List<Double>> embed(List<String> texts) {
        List<float[]> vectors = embeddingModel.embed(texts);
        return vectors.stream()
                .map(vector -> java.util.stream.IntStream.range(0, vector.length)
                        .mapToObj(i -> (double) vector[i])
                        .toList())
                .toList();
    }

    /**
     * 计算余弦相似度
     * @param v1 向量1
     * @param v2 向量2
     * @return 相似度（-1到1，越接近1越相似）
     */
    public double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            throw new IllegalArgumentException("向量维度不一致");
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < v1.size(); i++) {
            double val1 = v1.get(i);
            double val2 = v2.get(i);
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        // 防止除以0
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-10);
    }
}