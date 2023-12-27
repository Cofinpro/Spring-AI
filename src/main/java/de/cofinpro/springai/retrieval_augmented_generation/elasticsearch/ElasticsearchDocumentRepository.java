package de.cofinpro.springai.retrieval_augmented_generation.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticsearchDocumentRepository extends ElasticsearchRepository<ElasticsearchDocument, String> { }
