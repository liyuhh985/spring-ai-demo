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
            当用户问销售或价格相关问题时，必须先调用相关工具获取真实数据，再回答。
            如果知识库中有相关文档，优先根据文档内容回答，不要编造数据。
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
