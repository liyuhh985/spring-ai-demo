package com.example.aidemo.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具
 * 作用：把长文本切成小块，便于embedding和检索
 */
@Component
public class TextChunkUtil {

    /**
     * 按固定字数分块
     * @param text 原始文本
     * @param chunkSize 每块字数（比如500）
     * @param overlap 重叠字数（避免段落被切开）
     * @return 分块后的文本列表
     */
    public List<String> chunkBySize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));

            // 下一次从 end - overlap 开始（确保有重叠）
            start = end - overlap;
            if (start < 0) start = 0;
            if (start >= text.length()) break;
        }

        return chunks;
    }

    /**
     * 按段落分块（简单版本，用换行符分割）
     * @param text 原始文本
     * @return 分块后的文本列表
     */
    public List<String> chunkByParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 按换行符分割
        String[] paragraphs = text.split("\n");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }

        return chunks;
    }
}