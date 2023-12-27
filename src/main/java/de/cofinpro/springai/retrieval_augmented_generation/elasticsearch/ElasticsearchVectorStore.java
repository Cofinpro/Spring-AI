package de.cofinpro.springai.retrieval_augmented_generation.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.KnnSearchRequest;
import co.elastic.clients.elasticsearch.core.knn_search.KnnSearchQuery;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class ElasticsearchVectorStore implements VectorStore {

    private final EmbeddingClient embeddingClient;

    private final ElasticsearchDocumentRepository elasticsearchDocumentRepository;

    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchVectorStore(EmbeddingClient embeddingClient, ElasticsearchDocumentRepository elasticsearchDocumentRepository,
                                    ElasticsearchClient elasticsearchClient) {
        this.embeddingClient = embeddingClient;
        this.elasticsearchDocumentRepository = elasticsearchDocumentRepository;
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public void add(List<Document> documents) {
        for(Document document : documents) {
            final var embedding = this.embeddingClient.embed(document);
            final var elasticsearchDocument = new ElasticsearchDocument(document.getId(), document.getMetadata(), document.getContent(), embedding);
            elasticsearchDocumentRepository.save(elasticsearchDocument);
        }
    }

    @Override
    public Optional<Boolean> delete(List<String> idList) {
        elasticsearchDocumentRepository.deleteAllById(idList);
        return Optional.of(true);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (request.getFilterExpression() != null) {
            throw new UnsupportedOperationException("The [" + this.getClass() + "] doesn't support metadata filtering!");
        }
        final var userQueryEmbedding = embeddingClient.embed(request.getQuery()).stream().map(Double::floatValue).toList();
        try {
            final var knnSearchResponse = elasticsearchClient.knnSearch(
                    KnnSearchRequest.of(knnSearchRequestBuilder ->
                            knnSearchRequestBuilder.index("spring-ai-document").knn(KnnSearchQuery.of(knnSearchQueryBuilder ->
                                    knnSearchQueryBuilder.field("embedding").k(request.getTopK()).numCandidates(100).queryVector(userQueryEmbedding)))),
                    ElasticsearchDocument.class);
            return knnSearchResponse.hits().hits().stream().map(Hit::source).map(this::elasticsearchDocumentToSpringAiDocument).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Document elasticsearchDocumentToSpringAiDocument(ElasticsearchDocument elasticsearchDocument) {
        final var springAiDocument = new Document(elasticsearchDocument.id(), elasticsearchDocument.content(), elasticsearchDocument.metadata());
        springAiDocument.setEmbedding(elasticsearchDocument.embedding());
        return springAiDocument;
    }
}
