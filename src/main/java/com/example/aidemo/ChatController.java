package com.example.aidemo;

import com.example.aidemo.service.DatabaseService;
import com.example.aidemo.service.VectorStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final DatabaseService databaseService;
    private final VectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are an e-commerce data analyst.
            
            You can use these tools:
            - getProductsByPriceAsc(): Get products sorted by price (ascending). Use for "cheapest", "lowest price" questions.
            - getProductsBySales(): Get products sorted by sales (descending). Use for "best selling", "most popular" questions.
            - getSalesSummary(): Get sales statistics.

            When user asks questions requiring precise data, you should proactively call these tools.
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                          DatabaseService databaseService,
                          VectorStoreService vectorStoreService) {
        // Register tools using defaultFunction
        this.chatClient = chatClientBuilder
                .defaultFunction("getProductsByPriceAsc", 
                    "Get products sorted by price (ascending)",
                    (String input) -> {
                        var products = databaseService.getProductsByPriceDesc();
                        Collections.reverse(products);
                        try { 
                            return objectMapper.writeValueAsString(products); 
                        } catch (Exception e) { 
                            return "Error: " + e.getMessage(); 
                        }
                    })
                .defaultFunction("getProductsBySales",
                    "Get products sorted by sales (descending)",
                    (String input) -> {
                        var products = databaseService.getProductsBySales();
                        try { 
                            return objectMapper.writeValueAsString(products); 
                        } catch (Exception e) { 
                            return "Error: " + e.getMessage(); 
                        }
                    })
                .defaultFunction("getSalesSummary", 
                    "Get sales statistics",
                    (String input) -> {
                        var summary = databaseService.getSalesSummary();
                        try { 
                            return objectMapper.writeValueAsString(summary); 
                        } catch (Exception e) { 
                            return "Error: " + e.getMessage(); 
                        }
                    })
                .build();

        this.databaseService = databaseService;
        this.vectorStoreService = vectorStoreService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // Direct call - AI will automatically decide to use tools or not
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/init-knowledge-base")
    public String initKnowledgeBase() {
        initKnowledgeBaseAsync();
        return "Knowledge base update started...";
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

    @GetMapping("/test-search")
    public String testSearch(@RequestParam String query, 
                            @RequestParam(defaultValue = "10") int topK,
                            @RequestParam(defaultValue = "0.0") float threshold) {
        try {
            List<String> results = vectorStoreService.search(query, topK, threshold);
            StringBuilder response = new StringBuilder();
            response.append("Query: ").append(query).append("\n");
            response.append("Results: ").append(results.size()).append("\n");
            response.append(String.join("\n---\n", results));
            return response.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
