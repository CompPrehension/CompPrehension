package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * Модель вопроса для сериализации
 */
@Builder
@Getter
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
        private long tagBits;
        private long conceptBits;
        private long lawBits;
        private long violationBits;
        private long traceConceptBits;
        private double solutionStructuralComplexity;
        private double integralComplexity;
        private int solutionSteps;
        private int distinctErrorsCount;
        private int stage;
        private int version;
        private String structureHash;
        private boolean isDraft;
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
                    .tagBits(question.getMetadata().getTagBits())
                    .conceptBits(question.getMetadata().getConceptBits())
                    .lawBits(question.getMetadata().getLawBits())
                    .violationBits(question.getMetadata().getViolationBits())
                    .traceConceptBits(question.getMetadata().getTraceConceptBits())
                    .solutionStructuralComplexity(question.getMetadata().getSolutionStructuralComplexity())
                    .integralComplexity(question.getMetadata().getIntegralComplexity())
                    .solutionSteps(question.getMetadata().getSolutionSteps())
                    .distinctErrorsCount(question.getMetadata().getDistinctErrorsCount())
                    .stage(question.getMetadata().getStage())
                    .version(question.getMetadata().getVersion())
                    .structureHash(question.getMetadata().getStructureHash())
                    .isDraft(question.getMetadata().isDraft())
                    .origin(question.getMetadata().getOrigin())
                    .dateCreated(question.getMetadata().getDateCreated())
                    .build());
        }       

        return builder.build();
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
    public void serializeToFile(String path) throws IOException {
        gson.toJson(this, new FileWriter(path));
    }
    public void serializeToFile(Path path) throws IOException {
        gson.toJson(this, new FileWriter(path.toString()));
    }
    public void serializeToStream(OutputStream stream) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
        gson.toJson(this, writer);
    }
    
    private static class QuestionDataDeserializer implements JsonDeserializer<QuestionData> {
        @Override
        public QuestionData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            var questionObject = json.getAsJsonObject();
            
            var questionType = QuestionType.valueOf(questionObject.get("questionType").getAsString());
            var questionText = questionObject.get("questionText").getAsString();
            var questionName = questionObject.get("questionName").getAsString();
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
