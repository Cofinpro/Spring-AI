package de.cofinpro.springai.retrieval_augmented_generation.milvus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rag/milvus")
public class MilvusRetrievalAugmentedGenerationController {

    private final MilvusRetrievalAugmentedGenerationService milvusRetrievalAugmentedGenerationService;

    public MilvusRetrievalAugmentedGenerationController(MilvusRetrievalAugmentedGenerationService milvusRetrievalAugmentedGenerationService) {
        this.milvusRetrievalAugmentedGenerationService = milvusRetrievalAugmentedGenerationService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestDocuments() {
        milvusRetrievalAugmentedGenerationService.ingestDocuments();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping
    public String retrievalAugmentedGeneration(@RequestParam("message") String message) {
        return milvusRetrievalAugmentedGenerationService.retrievalAugmentedGeneration(message);
    }
}
