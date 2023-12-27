package de.cofinpro.springai.chatgpt;

import org.springframework.ai.openai.client.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatgpt")
public class ChatGPTController {

    private final OpenAiChatClient openAiChatClient;

    @Autowired
    public ChatGPTController(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    @GetMapping
    public String queryGpt(@RequestParam("message") String message) {
        return openAiChatClient.generate(message);
    }
}
