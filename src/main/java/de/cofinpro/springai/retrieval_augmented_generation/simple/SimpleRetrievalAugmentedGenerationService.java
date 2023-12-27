package de.cofinpro.springai.retrieval_augmented_generation.simple;

import de.cofinpro.springai.retrieval_augmented_generation.AbstractRetrievalAugmentedGenerationService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.client.OpenAiChatClient;
import org.springframework.ai.prompt.Prompt;
import org.springframework.ai.prompt.SystemPromptTemplate;
import org.springframework.ai.prompt.messages.UserMessage;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.retriever.VectorStoreRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SimpleRetrievalAugmentedGenerationService extends AbstractRetrievalAugmentedGenerationService {

    private static final File VECTORSTORE_FILE = new File("data/vectorstore.json");

    public SimpleRetrievalAugmentedGenerationService(OpenAiChatClient openAiChatClient, EmbeddingClient embeddingClient, @Value("classpath:/bikes.json") Resource bikesResource,
                                                     @Value("classpath:/system-prompt-template") Resource systemPromptTemplateResource) {
        super(new SimpleVectorStore(embeddingClient), openAiChatClient, systemPromptTemplateResource, bikesResource);
        if(VECTORSTORE_FILE.exists()) {
            ((SimpleVectorStore) getVectorStore()).load(VECTORSTORE_FILE);
        }
    }

    @Override
    public void ingestDocuments() {
        super.ingestDocuments();
        ((SimpleVectorStore)getVectorStore()).save(VECTORSTORE_FILE);
    }
}
