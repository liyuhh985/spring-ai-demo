package com.example.aidemo;

import com.example.aidemo.service.DatabaseService;
import com.example.aidemo.service.VectorStoreService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

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
            List<String> relevantDocs = vectorStoreService.search(message, 3);
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
        try {
            var products = databaseService.getAllProducts();
            for (var product : products) {
<<<<<<< HEAD
                String content = String.format("Product: %s, Price: %d, Category: %s, Sales: %d",
                        product.getName(), product.getPrice(), 
=======
                String content = String.format("Product: %s, Price: %.2f, Category: %s, Sales: %d",
                        product.getName(), product.getPrice().doubleValue(), 
>>>>>>> 42305bdabf259d5b83121e07bf001985f52e7357
                        product.getCategory(), product.getSales());
                vectorStoreService.saveDocument("product_" + product.getId(), content);
            }
            return "Knowledge base initialized! " + products.size() + " products imported";
        } catch (Exception e) {
            return "Init failed: " + e.getMessage();
        }
    }
}
