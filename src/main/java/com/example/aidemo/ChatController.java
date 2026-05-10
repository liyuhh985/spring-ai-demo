package com.example.aidemo;

import com.example.aidemo.service.DatabaseService;
import com.example.aidemo.service.VectorStoreService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final DatabaseService databaseService;
    private final VectorStoreService vectorStoreService;

    private static final String SYSTEM_PROMPT = """
            你是一个资深的电商数据分析师，擅长数据分析、用户增长和营销策略。
            你的回答要专业、简洁、有数据支撑。
            
            重要规则：
            1. 知识库中有所有产品的信息，包括名称、价格、类别、销量
            2. 当用户问关于"最便宜"、"最贵"、"销量最高"、"排名"等比较问题时，
               你必须先从知识库中提取所有相关数据，进行计算比较，再给出答案
            3. 不要凭感觉回答，必须基于知识库数据
            4. 如果知识库中的数据不足以回答，请明确说明
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                          DatabaseService databaseService,
                          VectorStoreService vectorStoreService) {
        this.chatClient = chatClientBuilder.build();
        this.databaseService = databaseService;
        this.vectorStoreService = vectorStoreService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // Step 1: RAG vector search
        String ragContext = "";
        try {
            // 先取 20 个候选，再用 LLM 重排序到 10 个
            List<String> candidates = vectorStoreService.search(message, 20, 0.0f);
            List<String> relevantDocs = rerankWithLLM(message, candidates, 10);
            if (relevantDocs != null && !relevantDocs.isEmpty()) {
                ragContext = "【Knowledge Base】\n" + String.join("\n---\n", relevantDocs);
            }
        } catch (Exception e) {
            ragContext = "【RAG Failed】" + e.getMessage();
        }
        
        // Step 2: Traditional database query
        String lowerMessage = message.toLowerCase();
        String dataContext = "";
        
        try {
            if (lowerMessage.contains("sales") || lowerMessage.contains("top") || 
                lowerMessage.contains("best") || lowerMessage.contains("sold")) {
                var products = databaseService.getProductsBySales();
                dataContext = "【Database】Sales ranking: " + products;
            } 
            else if (lowerMessage.contains("price") || lowerMessage.contains("expensive") || 
                     lowerMessage.contains("cheap") || lowerMessage.contains("cost")) {
                var products = databaseService.getProductsByPriceDesc();
                dataContext = "【Database】Price ranking: " + products;
            }
            else if (lowerMessage.contains("stat") || lowerMessage.contains("summary")) {
                var summary = databaseService.getSalesSummary();
                dataContext = "【Database】Statistics: " + summary;
            }
        } catch (Exception e) {
            dataContext = "Database query failed: " + e.getMessage();
        }
        
        // Step 3: Build context
        StringBuilder fullMessage = new StringBuilder();
        fullMessage.append(message).append("\n\n");
        
        if (!ragContext.isEmpty()) {
            fullMessage.append(ragContext).append("\n\n");
        }
        
        if (!dataContext.isEmpty()) {
            fullMessage.append(dataContext);
        }
        
        // Step 4: Call LLM
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullMessage.toString())
                .call()
                .content();
    }
    
    @GetMapping("/init-knowledge-base")
    public String initKnowledgeBase() {
        initKnowledgeBaseAsync();
        return "知识库更新已启动，请在后台等待完成...";
    }

    @Async
    public void initKnowledgeBaseAsync() {
        try {
            var products = databaseService.getAllProducts();
            for (var product : products) {
                String content = String.format("Product: %s, Price: %.2f, Category: %s, Sales: %d",
                        product.getName(), product.getPrice().doubleValue(), 
                        product.getCategory(), product.getSales());
                vectorStoreService.saveDocument("product_" + product.getId(), content);
            }
            System.out.println("Knowledge base initialized! " + products.size() + " products imported");
        } catch (Exception e) {
            System.out.println("Init failed: " + e.getMessage());
        }
    }
    
    /**
     * 测试向量检索（带阈值过滤）
     * @param query 搜索关键词
     * @param topK 返回结果数量
     * @param threshold 相似度阈值（0.0-1.0），低于此值的结果会被过滤
     */
    @GetMapping("/test-search")
    public String testSearch(@RequestParam String query, 
                            @RequestParam(defaultValue = "10") int topK,
                            @RequestParam(defaultValue = "0.0") float threshold) {
        try {
            List<String> results = vectorStoreService.search(query, topK, threshold);
            
            StringBuilder response = new StringBuilder();
            response.append("Query: ").append(query).append("\n");
            response.append("Threshold: ").append(threshold).append("\n");
            response.append("TopK: ").append(topK).append("\n\n");
            
            if (results.isEmpty()) {
                response.append("No results found (or all results below threshold)");
            } else {
                response.append("Results: \n").append(String.join("\n---\n", results));
            }
            
            return response.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * 用 LLM 对候选结果进行重排序
     */
    private List<String> rerankWithLLM(String query, List<String> candidates, int topK) {
        if (candidates.isEmpty()) return candidates;
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据与用户问题的相关度，对以下文档进行排序。\n\n");
        prompt.append("用户问题：").append(query).append("\n\n");
        prompt.append("候选文档：\n");
        for (int i = 0; i < candidates.size(); i++) {
            prompt.append(i + 1).append(". ").append(candidates.get(i)).append("\n");
        }
        prompt.append("\n请输出前 ").append(topK).append(" 个最相关的文档编号，用逗号分隔（如：1,3,5）：");

        String result = chatClient.prompt()
                .system("你是一个文档相关性排序专家。")
                .user(prompt.toString())
                .call()
                .content();

        return parseRerankResult(result, candidates, topK);
    }

    private List<String> parseRerankResult(String llmResult, List<String> candidates, int topK) {
        List<String> results = new ArrayList<>();
        String[] parts = llmResult.replaceAll("[^0-9,，]", " ").split("\\s+");

        for (String p : parts) {
            try {
                int idx = Integer.parseInt(p.replaceAll("[^0-9]", "")) - 1;
                if (idx >= 0 && idx < candidates.size() && !results.contains(candidates.get(idx))) {
                    results.add(candidates.get(idx));
                }
            } catch (Exception e) {}
            if (results.size() >= topK) break;
        }

        if (results.isEmpty()) {
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
        return results;
    }
}
