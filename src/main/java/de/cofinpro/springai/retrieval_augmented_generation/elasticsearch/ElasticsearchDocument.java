package de.cofinpro.springai.retrieval_augmented_generation.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;
import java.util.Map;

@Document(indexName = "spring-ai-document", writeTypeHint = WriteTypeHint.FALSE)
public record ElasticsearchDocument(

        @Id
        @Field(type = FieldType.Keyword)
        String id,

        @Field(type = FieldType.Object)
        Map<String, Object> metadata,

        @Field(type = FieldType.Text, analyzer = "english")
        String content,

        @Field(type = FieldType.Dense_Vector, dims = 1536, similarity = "cosine")
        List<Double> embedding
) {}
