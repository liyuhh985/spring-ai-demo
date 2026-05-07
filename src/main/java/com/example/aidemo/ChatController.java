package com.example.aidemo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    // System Prompt：告诉 AI 它的角色
    private static final String SYSTEM_PROMPT = """
            你是一个资深的电商运营专家，擅长数据分析、用户增长和营销策略。
            你的回答要专业、简洁、有数据支撑。
            如果用户问的是销售数据，要主动给出分析和建议。
            """;

    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)  // 添加系统提示词
                .user(message)
                .call()
                .content();
    }
}