package de.cofinpro.springai.retrieval_augmented_generation.simple;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rag/simple")
public class SimpleRetrievalAugmentedGenerationController {

    private final SimpleRetrievalAugmentedGenerationService simpleRetrievalAugmentedGenerationService;

    public SimpleRetrievalAugmentedGenerationController(SimpleRetrievalAugmentedGenerationService simpleRetrievalAugmentedGenerationService) {
        this.simpleRetrievalAugmentedGenerationService = simpleRetrievalAugmentedGenerationService;
    }

    @PostMapping("/ingest")
    public void ingestDocuments() {
        simpleRetrievalAugmentedGenerationService.ingestDocuments();
    }

    @GetMapping
    public String retrievalAugmentedGeneration(@RequestParam("message") String message) {
        return simpleRetrievalAugmentedGenerationService.retrievalAugmentedGeneration(message);
    }
}
