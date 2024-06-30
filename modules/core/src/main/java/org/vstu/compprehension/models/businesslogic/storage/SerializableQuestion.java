package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Модель вопроса для сериализации
 */
@Builder
@Getter
@Log4j2
public class SerializableQuestion {
    private QuestionData questionData;
    private QuestionMetadata metadata;
    private List<String> concepts;
    private List<String> negativeLaws;
    private List<String> tags;

    @Builder
    @Getter
    public static class QuestionData {
        private QuestionType questionType;
        private String questionText;
        private String questionName;
        private String questionDomainType;
        private QuestionOptionsEntity options;
        private List<AnswerObject> answerObjects;
        private List<StatementFact> statementFacts;
    }

    @Builder
    @Getter
    public static class AnswerObject {
        private int answerId;
        private String hyperText;
        private String domainInfo;
        private boolean isRightCol;
        private String concept;
    }

    @Builder
    @Getter @ToString
    public static class StatementFact {
        private String subjectType;
        private String subject;
        private String verb;
        private String objectType;
        private String object;
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
    }
    
    public static SerializableQuestion fromQuestion(Question question) {
        var builder = SerializableQuestion.builder();

        builder.questionData(QuestionData.builder()
                        .questionType(question.getQuestionType())
                        .questionText(question.getQuestionText().getText())
                        .questionName(question.getQuestionName())
                        .questionDomainType(question.getQuestionDomainType())
                        .options(question.getQuestionData().getOptions())
                        .answerObjects(question.getAnswerObjects().stream().map(
                                answerObjectEntity -> AnswerObject.builder()
                                        .answerId(answerObjectEntity.getAnswerId())
                                        .hyperText(answerObjectEntity.getHyperText())
                                        .domainInfo(answerObjectEntity.getDomainInfo())
                                        .isRightCol(answerObjectEntity.isRightCol())
                                        .concept(answerObjectEntity.getConcept())
                                        .build()
                        ).toList())
                        .statementFacts(question.getStatementFacts().stream().map(
                                statementFactEntity -> StatementFact.builder()
                                        .object(statementFactEntity.getObject())
                                        .objectType(statementFactEntity.getObjectType())
                                        .subject(statementFactEntity.getSubject())
                                        .subjectType(statementFactEntity.getSubjectType())
                                        .verb(statementFactEntity.getVerb())
                                        .build()
                        ).toList()).build())
                .concepts(question.getConcepts())
                .negativeLaws(question.getNegativeLaws())
                .tags(question.getTags().stream().toList())
                .build();
        if (question.getMetadata() != null) {
            builder.metadata(QuestionMetadata.builder()
                    .name(question.getMetadata().getName())
                    .domainShortname(question.getDomain().getShortName())
                    .templateId(question.getMetadata().getTemplateId())
                    .tagBits(question.getMetadata().getTagBits())
                    .conceptBits(question.getMetadata().getConceptBits())
                    .lawBits(question.getMetadata().getLawBits())
                    .violationBits(question.getMetadata().getViolationBits())
                    .traceConceptBits(question.getMetadata().getTraceConceptBits())
                    .solutionStructuralComplexity(question.getMetadata().getSolutionStructuralComplexity())
                    .integralComplexity(question.getMetadata().getIntegralComplexity())
                    .solutionSteps(question.getMetadata().getSolutionSteps())
                    .distinctErrorsCount(question.getMetadata().getDistinctErrorsCount())
                    .version(question.getMetadata().getVersion())
                    .structureHash(question.getMetadata().getStructureHash())
                    .origin(question.getMetadata().getOrigin())
                    .dateCreated(question.getMetadata().getCreatedAt())
                    .build());
        }       

        return builder.build();
    }
    
    public QuestionMetadataEntity toMetadataEntity() {
        return QuestionMetadataEntity.builder()
                .name(metadata.getName())
                .domainShortname(metadata.getDomainShortname())
                .templateId(metadata.getTemplateId())
                .tagBits(metadata.getTagBits())
                .conceptBits(metadata.getConceptBits())
                .lawBits(metadata.getLawBits())
                .violationBits(metadata.getViolationBits())
                .traceConceptBits(metadata.getTraceConceptBits())
                .solutionStructuralComplexity(metadata.getSolutionStructuralComplexity())
                .integralComplexity(metadata.getIntegralComplexity())
                .solutionSteps(metadata.getSolutionSteps())
                .distinctErrorsCount(metadata.getDistinctErrorsCount())
                .version(metadata.getVersion())
                .structureHash(metadata.getStructureHash())
                .origin(metadata.getOrigin())
                .build();
    }
    
    public Question toQuestion(@NotNull Domain domain) {
        return toQuestion(domain, null);
    }

    public Question toQuestion(@NotNull Domain domain, @Nullable QuestionMetadataEntity qMeta) {
        if (qMeta != null && !domain.getShortName().equals(qMeta.getDomainShortname())) {
            log.info("Domain mismatch: {} vs {}", qMeta.getDomainShortname(), domain.getShortName());
        }
        
        var questionData = getQuestionData();
        var questionEntity = new QuestionEntity();
        questionEntity.setQuestionType(questionData.getQuestionType());
        questionEntity.setQuestionText(questionData.getQuestionText());
        questionEntity.setQuestionName(questionData.getQuestionName());
        questionEntity.setQuestionDomainType(questionData.getQuestionDomainType());
        questionEntity.setMetadata(qMeta);
        questionEntity.setOptions(questionData.getOptions());
        questionEntity.setAnswerObjects(questionData.getAnswerObjects()
                .stream()
                .map(a -> AnswerObjectEntity.builder()
                        .answerId(a.getAnswerId())
                        .hyperText(a.getHyperText())
                        .domainInfo(a.getDomainInfo())
                        .isRightCol(a.isRightCol())
                        .concept(a.getConcept())
                        .build())
                .toList());
        questionEntity.setInteractions(new ArrayList<>());
        questionEntity.setDomainEntity(domain.getDomainEntity());
        questionEntity.setStatementFacts(questionData.getStatementFacts()
                .stream()
                .map(s -> new BackendFactEntity(
                        s.getSubjectType(),
                        s.getSubject(),
                        s.getVerb(),
                        s.getObjectType(),
                        s.getObject()))
                .toList());
        questionEntity.setSolutionFacts(new ArrayList<>());

        var result = new Question(questionEntity, domain);
        result.setConcepts(new ArrayList<>(getConcepts()));
        result.setTags(new HashSet<>(getTags()));
        result.setNegativeLaws(new ArrayList<>(getNegativeLaws()));
        return result;
    }

    private static Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(QuestionData.class, new QuestionDataDeserializer())
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
    
    public static @Nullable SerializableQuestion deserialize(String path) {
        try (var stream = new FileInputStream(path);
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, SerializableQuestion.class);
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

    public static SerializableQuestion deserializeFromString(String data) {
        return gson.fromJson(data, SerializableQuestion.class);
    }

    public static String serializeToString(SerializableQuestion question) {
        return gson.toJson(question);
    }
    
    private static class QuestionDataDeserializer implements JsonDeserializer<QuestionData> {
        @Override
        public QuestionData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            var questionObject = json.getAsJsonObject();
            
            var questionType = QuestionType.valueOf(questionObject.get("questionType").getAsString());
            var questionText = questionObject.get("questionText").getAsString();
            var questionName = questionObject.has("questionName") ? questionObject.get("questionName").getAsString() : questionText;
            var questionDomainType = questionObject.get("questionDomainType").getAsString();
            var answerObjects = context.<List<AnswerObject>>deserialize(questionObject.get("answerObjects"), new TypeToken<List<AnswerObject>>(){}.getType());
            var statementFacts = context.<List<StatementFact>>deserialize(questionObject.get("statementFacts"), new TypeToken<List<StatementFact>>(){}.getType());
            
            QuestionOptionsEntity options = switch (questionType) {
                case QuestionType.ORDER ->
                        context.deserialize(questionObject.get("options"), OrderQuestionOptionsEntity.class);
                case QuestionType.MATCHING ->
                        context.deserialize(questionObject.get("options"), MatchingQuestionOptionsEntity.class);
                case QuestionType.SINGLE_CHOICE ->
                        context.deserialize(questionObject.get("options"), SingleChoiceOptionsEntity.class);
                case QuestionType.MULTI_CHOICE ->
                        context.deserialize(questionObject.get("options"), MultiChoiceOptionsEntity.class);
            };

            return QuestionData.builder()
                    .questionType(questionType)
                    .questionText(questionText)
                    .questionName(questionName)
                    .questionDomainType(questionDomainType)
                    .options(options)
                    .answerObjects(answerObjects)
                    .statementFacts(statementFacts)
                    .build();
        }
    }
}
