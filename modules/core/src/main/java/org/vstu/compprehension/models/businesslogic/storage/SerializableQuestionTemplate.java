package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

@Builder
@Getter
public class SerializableQuestionTemplate {
    SerializableQuestion commonQuestion;
    List<QuestionMetadata> metadataList;

    private static Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(SerializableQuestion.QuestionData.class, new SerializableQuestion.QuestionDataDeserializer())
            .create();

    public static SerializableQuestion deserialize(InputStream stream) {
        return gson.fromJson(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                SerializableQuestion.class);
    }

    public static SerializableQuestion[] deserializeMany(InputStream stream) {
        return gson.fromJson(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                SerializableQuestion[].class);
    }

    public static @Nullable SerializableQuestionTemplate deserialize(String path) {
        try (var stream = new FileInputStream(path);
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, SerializableQuestionTemplate.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void serializeToFile(String path) throws IOException {
        try (var stream = new FileOutputStream(path);
             var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            gson.toJson(this, writer);
        }
    }

    public void serializeToFile(Path path) throws IOException {
        try (var stream = Files.newOutputStream(path);
             var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            gson.toJson(this, writer);
        }
    }

    public void serializeToStream(OutputStream stream) throws IOException {
        try (var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            gson.toJson(this, writer);
        }
    }

    public static SerializableQuestionTemplate deserializeFromString(String data) {
        return gson.fromJson(data, SerializableQuestionTemplate.class);
    }

    public static String serializeToString(SerializableQuestion question) {
        return gson.toJson(question);
    }

    @Builder
    @Getter
    public static class QuestionMetadata {
        private String name;
        private String domainShortname;
        private String templateId;
        private long tagBits;
        private long conceptBits;
        private long lawBits;
        private long violationBits;
        private long traceConceptBits;
        private double solutionStructuralComplexity;
        private double integralComplexity;
        private int solutionSteps;
        private int distinctErrorsCount;
        private int version;
        private String structureHash;
        private String origin;
        private Date dateCreated;
        private String language;
        private int treeHashCode;

        public QuestionMetadataEntity toMetadataEntity() {
            return QuestionMetadataEntity.builder()
                    .name(this.getName())
                    .domainShortname(this.getDomainShortname())
                    .templateId(this.getTemplateId())
                    .tagBits(this.getTagBits())
                    .conceptBits(this.getConceptBits())
                    .lawBits(this.getLawBits())
                    .violationBits(this.getViolationBits())
                    .traceConceptBits(this.getTraceConceptBits())
                    .solutionStructuralComplexity(this.getSolutionStructuralComplexity())
                    .integralComplexity(this.getIntegralComplexity())
                    .solutionSteps(this.getSolutionSteps())
                    .distinctErrorsCount(this.getDistinctErrorsCount())
                    .version(this.getVersion())
                    .structureHash(this.getStructureHash())
                    .origin(this.getOrigin())
                    .build();
        }
    }
}
