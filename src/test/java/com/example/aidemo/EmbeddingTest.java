package com.example.aidemo;

import com.example.aidemo.utils.EmbeddingUtil;
import com.example.aidemo.utils.TextChunkUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EmbeddingTest {

    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Autowired
    private TextChunkUtil textChunkUtil;

    /**
     * 测试1：基本的Embedding功能
     */
    @Test
    public void testEmbed() {
        float[] vector = embeddingUtil.embed("你好");
        
        System.out.println("=== Embedding 测试 ===");
        System.out.println("向量长度: " + vector.length);
        System.out.println("向量前5位: " + vector[0] + ", " + vector[1] + ", " + vector[2] + ", " + vector[3] + ", " + vector[4]);
        
        // 阿里云百炼 text-embedding-v2 是 1536 维
        assertEquals(1536, vector.length, "向量长度应该是1536");
    }

    /**
     * 测试2：余弦相似度
     */
    @Test
    public void testSimilarity() {
        System.out.println("=== 余弦相似度测试 ===");
        
        float[] v1 = embeddingUtil.embed("苹果");
        float[] v2 = embeddingUtil.embed("香蕉");
        float[] v3 = embeddingUtil.embed("电脑");
        
        double sim1 = embeddingUtil.cosineSimilarity(v1, v2);  // 苹果 vs 香蕉
        double sim2 = embeddingUtil.cosineSimilarity(v1, v3);  // 苹果 vs 电脑
        
        System.out.println("苹果 vs 香蕉 相似度: " + sim1);
        System.out.println("苹果 vs 电脑 相似度: " + sim2);
        
        // 语义相近的应该相似度更高
        assertTrue(sim1 > sim2, "苹果和香蕉应该比苹果和电脑更相似");
    }

    /**
     * 测试3：文本分块
     */
    @Test
    public void testTextChunk() {
        System.out.println("=== 文本分块测试 ===");
        
        String text = "这是第一段内容。\n\n这是第二段内容。\n\n这是第三段内容。";
        
        // 1. 按段落分块
        List<String> chunks = textChunkUtil.chunkByParagraph(text);
        System.out.println("按段落分块数量: " + chunks.size());
        assertEquals(3, chunks.size());
        
        // 2. 按固定字数分块 (测试死循环是否修复)
        String longText = "这是一段很长的文本，我们需要把它分成小块。每块大约50字。";
        List<String> sizeChunks = textChunkUtil.chunkBySize(longText, 10, 5);
        
        System.out.println("按字数分块数量: " + sizeChunks.size());
        assertTrue(sizeChunks.size() > 0);
        
        // 打印分块内容（不考虑乱码，仅验证逻辑）
        for (int i = 0; i < sizeChunks.size(); i++) {
            System.out.println("分块 " + i + ": " + sizeChunks.get(i));
        }
    }
}
