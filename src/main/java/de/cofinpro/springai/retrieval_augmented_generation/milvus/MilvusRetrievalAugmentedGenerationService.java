package de.cofinpro.springai.retrieval_augmented_generation.milvus;

import com.alibaba.fastjson.JSON;
import de.cofinpro.springai.retrieval_augmented_generation.AbstractRetrievalAugmentedGenerationService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.CreateCollectionRequestOrBuilder;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.bulkinsert.BulkInsertParam;
import com.alibaba.fastjson.JSONObject;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MilvusRetrievalAugmentedGenerationService {

    private static final File VECTORSTORE_FILE = new File("data/vectorstore.json");

    private final OpenAiChatClient openAiChatClient;
    private final EmbeddingClient embeddingClient;
    private final Resource bikesResource;
    private final SystemPromptTemplate systemPromptTemplate;
    private final MilvusServiceClient milvusServiceClient;

    private static final String COLLECTION_NAME = "TEST_COLLECTION";

    @Autowired
    public MilvusRetrievalAugmentedGenerationService(OpenAiChatClient openAiChatClient, EmbeddingClient embeddingClient, @Value("classpath:/bikes.json") Resource bikesResource,
                                                     @Value("classpath:/system-prompt-template") Resource systemPromptTemplateResource, MilvusServiceClient milvusServiceClient) {
        this.openAiChatClient = openAiChatClient;
        this.embeddingClient = embeddingClient;
        this.bikesResource = bikesResource;
        this.milvusServiceClient = milvusServiceClient;
        this.systemPromptTemplate = new SystemPromptTemplate(systemPromptTemplateResource);
    }

    public String retrievalAugmentedGeneration(String message) {
        List<List<Float>> searchVectors = List.of(embeddingClient.embed(message).stream().map(Double::floatValue).toList());
        var searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withOutFields(Arrays.asList("document_content"))
                .withTopK(5)
                .withVectors(searchVectors)
                .withVectorFieldName("document_vectors")
                .withParams("{\"nprobe\":10, \"offset\":0}")
                .build();


        var status = milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());
        System.out.println(status);
        final var similarDocuments = milvusServiceClient.search(searchParam);
        milvusServiceClient.releaseCollection(ReleaseCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());
        final var results = similarDocuments.toString();
        final var systemMessage = systemPromptTemplate.createMessage(Map.of("documents", results));
        final var prompt = new Prompt(List.of(systemMessage, new UserMessage(message)));
        return openAiChatClient.call(prompt).getResult().toString();
    }

    public void ingestDocuments() throws IOException {
        FieldType fieldType1 = FieldType.newBuilder()
                .withName("document_id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        FieldType fieldType2 = FieldType.newBuilder()
                .withName("document_content")
                .withDataType(DataType.VarChar)
                .withMaxLength(5000)
                .build();
        FieldType fieldType3 = FieldType.newBuilder()
                .withName("document_vectors")
                .withDataType(DataType.FloatVector)
                .withDimension(5000)
                .build();
        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("Test document search")
                .withShardsNum(2)
                .addFieldType(fieldType1)
                .addFieldType(fieldType2)
                .addFieldType(fieldType3)
                .withEnableDynamicField(true)
                .build();

        var ret = milvusServiceClient.createCollection(createCollectionReq);
        if (ret.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create collection! Error: " + ret.getMessage());
        }
        ret = milvusServiceClient.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFieldName("document_vectors")
                .withIndexType(IndexType.FLAT)
                .withMetricType(MetricType.L2)
                .build());
        if (ret.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create index on vector field! Error: " + ret.getMessage());
        }
        ret = milvusServiceClient.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFieldName("document_content")
                .withIndexType(IndexType.TRIE)
                .build());
        if (ret.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create index on varchar field! Error: " + ret.getMessage());
        }

        milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());

        final var jsonReader = new JsonReader(bikesResource, "name", "price", "shortDescription");
        final List<Document> documents = jsonReader.get();
        final var documentContent = documents.stream().map(document -> new JSONObject(Map.of("document_content", document.getContent()))).toList();


        var insertRet = milvusServiceClient.bulkInsert(BulkInsertParam.newBuilder().withCollectionName(COLLECTION_NAME).withFiles(List.of("classpath:/bikes.json")).build());

        /*
        R<MutationResult> insertRet = milvusServiceClient.insert(InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withRows(documentContent)
                .build()); */
        if (insertRet.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to insert! Error: " + insertRet.getMessage());
        }

        milvusServiceClient.releaseCollection(ReleaseCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());
    }
}
