package de.cofinpro.springai.retrieval_augmented_generation.elasticsearch;

import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/rag/elasticsearch")
public class ElasticsearchRAGController {

    private final ElasticsearchRetrievalAugmentedGenerationService elasticsearchRetrievalAugmentedGenerationService;

    public ElasticsearchRAGController(ElasticsearchRetrievalAugmentedGenerationService elasticsearchRetrievalAugmentedGenerationService) {
        this.elasticsearchRetrievalAugmentedGenerationService = elasticsearchRetrievalAugmentedGenerationService;
    }

    @PostMapping("/ingest")
    public void ingestDocuments() {
        elasticsearchRetrievalAugmentedGenerationService.ingestDocuments();
    }

    @GetMapping
    public String rag(@RequestParam("message") String message) {
        return elasticsearchRetrievalAugmentedGenerationService.retrievalAugmentedGeneration(message);
    }
}
