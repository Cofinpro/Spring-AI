package de.cofinpro.springai.retrieval_augmented_generation.milvus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/rag/milvus")
public class MilvusRetrievalAugmentedGenerationController {

    private final MilvusRetrievalAugmentedGenerationService milvusRetrievalAugmentedGenerationService;

    public MilvusRetrievalAugmentedGenerationController(MilvusRetrievalAugmentedGenerationService milvusRetrievalAugmentedGenerationService) {
        this.milvusRetrievalAugmentedGenerationService = milvusRetrievalAugmentedGenerationService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestDocuments() {
        try {
            milvusRetrievalAugmentedGenerationService.ingestDocuments();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public String retrievalAugmentedGeneration(@RequestParam("message") String message) {
        return milvusRetrievalAugmentedGenerationService.retrievalAugmentedGeneration(message);
    }
}
