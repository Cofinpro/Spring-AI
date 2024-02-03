package de.cofinpro.springai.retrieval_augmented_generation;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractRetrievalAugmentedGenerationService {

    private final VectorStore vectorStore;

    private final OpenAiChatClient openAiChatClient;

    private final SystemPromptTemplate systemPromptTemplate;

    private final Resource bikesResource;

    public AbstractRetrievalAugmentedGenerationService(VectorStore vectorStore, OpenAiChatClient openAiChatClient, Resource systemPromptTemplateResource,
                                                       Resource bikesResource) {
        this.vectorStore = vectorStore;
        this.openAiChatClient = openAiChatClient;
        systemPromptTemplate = new SystemPromptTemplate(systemPromptTemplateResource);
        this.bikesResource = bikesResource;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public  void ingestDocuments() {
        final var jsonReader = new JsonReader(bikesResource, "name", "price", "shortDescription");
        vectorStore.add(jsonReader.get());
    }

    public String retrievalAugmentedGeneration(String message) {
        final var similarDocuments = vectorStore.similaritySearch(message);
        final var joinedDocuments = similarDocuments.stream().map(Document::getContent).collect(Collectors.joining("\n"));
        final var systemMessage = systemPromptTemplate.createMessage(Map.of("documents", joinedDocuments));
        final var prompt = new Prompt(List.of(systemMessage, new UserMessage(message)));
        return openAiChatClient.call(prompt).getResult().toString();
    }
}
