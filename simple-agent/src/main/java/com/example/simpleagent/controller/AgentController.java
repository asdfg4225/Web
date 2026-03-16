// src/main/java/com/example/simpleagent/AgentController.java
package com.example.simpleagent.controller;

import com.example.simpleagent.model.Message;
import com.example.simpleagent.service.AiAgentService;
import com.example.simpleagent.service.ToolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AiAgentService aiAgentService;
    private final ToolService toolService;

    public AgentController(AiAgentService aiAgentService, ToolService toolService) {
        this.aiAgentService = aiAgentService;
        this.toolService = toolService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return aiAgentService.getResponseWithTools(request.getMessages(), toolService);
    }

    static class ChatRequest {
        private List<Message> messages;

        public List<Message> getMessages() {
            return messages;
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
    }
}