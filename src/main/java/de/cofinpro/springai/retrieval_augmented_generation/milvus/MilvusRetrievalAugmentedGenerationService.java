package de.cofinpro.springai.retrieval_augmented_generation.milvus;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MilvusRetrievalAugmentedGenerationService {

    private final OpenAiChatClient openAiChatClient;
    private final Resource bikesResource;
    private final SystemPromptTemplate systemPromptTemplate;

    private final VectorStore vectorStore;

    @Autowired
    public MilvusRetrievalAugmentedGenerationService(OpenAiChatClient openAiChatClient,
                                                     VectorStore vectorStore,
                                                     @Value("classpath:/bikes.json") Resource bikesResource,
                                                     @Value("classpath:/system-prompt-template") Resource systemPromptTemplateResource) {
        this.openAiChatClient = openAiChatClient;
        this.vectorStore = vectorStore;
        this.bikesResource = bikesResource;
        this.systemPromptTemplate = new SystemPromptTemplate(systemPromptTemplateResource);
    }

    public void ingestDocuments() {
        final var jsonReader = new JsonReader(bikesResource, "name", "price", "shortDescription");
        vectorStore.add(jsonReader.get());
    }

    public String retrievalAugmentedGeneration(String message) {
        final var test = vectorStore.similaritySearch(SearchRequest.query(message));
        final var systemMessage = systemPromptTemplate.createMessage(Map.of("documents", test));
        final var prompt = new Prompt(List.of(systemMessage, new UserMessage(message)));
        return openAiChatClient.call(prompt).getResult().toString();
    }
}
