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
        List<Double> vector = embeddingUtil.embed("你好");
        
        System.out.println("=== Embedding 测试 ===");
        System.out.println("向量长度: " + vector.size());
        System.out.println("向量前5位: " + vector.get(0) + ", " + vector.get(1) + ", " + vector.get(2) + ", " + vector.get(3) + ", " + vector.get(4));
        
        // DeepSeek Embedding 是 1024 维
        assertEquals(1024, vector.size(), "向量长度应该是1024");
    }

    /**
     * 测试2：余弦相似度
     */
    @Test
    public void testSimilarity() {
        System.out.println("=== 余弦相似度测试 ===");
        
        List<Double> v1 = embeddingUtil.embed("苹果");
        List<Double> v2 = embeddingUtil.embed("香蕉");
        List<Double> v3 = embeddingUtil.embed("电脑");
        
        double sim1 = embeddingUtil.cosineSimilarity(v1, v2);  // 苹果 vs 香蕉
        double sim2 = embeddingUtil.cosineSimilarity(v1, v3);  // 苹果 vs 电脑
        
        System.out.println("苹果 vs 香蕉 相似度: " + sim1);
        System.out.println("苹果 vs 电脑 相似度: " + sim2);
        
        // 语义相近的应该相似度更高
        assertTrue(sim1 > sim2, "苹果和香蕉应该比苹果和电脑更相似");
    }

    /**
     * 测试3：批量Embedding
     */
    @Test
    public void testBatchEmbed() {
        System.out.println("=== 批量Embedding测试 ===");
        
        List<String> texts = List.of("Spring Boot", "Spring Cloud", "Python");
        List<List<Double>> vectors = embeddingUtil.embed(texts);
        
        System.out.println("批量处理数量: " + vectors.size());
        assertEquals(3, vectors.size());
        
        // 计算两两相似度
        double sim = embeddingUtil.cosineSimilarity(vectors.get(0), vectors.get(1));
        System.out.println("Spring Boot vs Spring Cloud 相似度: " + sim);
    }

    /**
     * 测试4：文本分块
     */
    @Test
    public void testTextChunk() {
        System.out.println("=== 文本分块测试 ===");
        
        String text = "这是第一段内容。\n\n这是第二段内容。\n\n这是第三段内容。";
        
        // 按段落分块
        List<String> chunks = textChunkUtil.chunkByParagraph(text);
        System.out.println("分块数量: " + chunks.size());
        chunks.forEach(chunk -> System.out.println("块: " + chunk));
        
        assertEquals(3, chunks.size());
        
        // 按固定字数分块
        String longText = "这是一段很长的文本，我们需要把它分成小块。" +
                "每一块大约200字左右，这样可以更好地进行向量化处理。" +
                "分块的好处是可以提高检索的精度，因为我们总是返回最相关的那个小块，而不是整篇长文档。";
        
        List<String> sizeChunks = textChunkUtil.chunkBySize(longText, 50, 10);
        System.out.println("\n固定字数分块数量: " + sizeChunks.size());
        sizeChunks.forEach(chunk -> System.out.println("块(" + chunk.length() + "字): " + chunk));
    }
}
