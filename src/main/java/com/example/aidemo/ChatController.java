package com.example.aidemo;

import com.example.aidemo.service.DatabaseService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final DatabaseService databaseService;

    // System Prompt：告诉 AI 它的角色和能力
    private static final String SYSTEM_PROMPT = """
            你是一个资深的电商数据分析师，擅长数据分析、用户增长和营销策略。
            你的回答要专业、简洁、有数据支撑。
            
            你可以调用以下工具来获取数据：
            - getProductsBySales(): 获取按销售额排序的商品列表
            - getAllProducts(): 获取所有商品
            - getProductsByCategory(category): 按分类查询商品
            - getSalesSummary(): 获取销售统计汇总
            
            当用户问销售相关问题时，必须先调用相关工具获取真实数据，再回答。
            回答时要用真实数据，并给出简单的分析建议。
            """;

    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder, DatabaseService databaseService) {
        this.chatClient = chatClientBuilder.build();
        this.databaseService = databaseService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // 根据用户问题，自动调用数据库
        String lowerMessage = message.toLowerCase();
        String dataContext = "";
        
        try {
            if (lowerMessage.contains("销售") || lowerMessage.contains("top") || lowerMessage.contains("最好")) {
                var products = databaseService.getProductsBySales();
                dataContext = "按销售额排序的商品数据：" + products;
            } else if (lowerMessage.contains("统计") || lowerMessage.contains("汇总")) {
                var summary = databaseService.getSalesSummary();
                dataContext = "销售统计数据：" + summary;
            } else if (lowerMessage.contains("电子") || lowerMessage.contains("服装") || lowerMessage.contains("家电")) {
                String category = lowerMessage.contains("电子") ? "电子产品" : 
                                 lowerMessage.contains("服装") ? "服装" : "家电";
                var products = databaseService.getProductsByCategory(category);
                dataContext = category + "分类的商品数据：" + products;
            }
        } catch (Exception e) {
            dataContext = "数据库查询失败: " + e.getMessage();
        }
        
        // 构建完整的问题（包含数据库上下文）
        String fullMessage = message + "\n\n参考数据：" + dataContext;
        
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullMessage)
                .call()
                .content();
    }
}