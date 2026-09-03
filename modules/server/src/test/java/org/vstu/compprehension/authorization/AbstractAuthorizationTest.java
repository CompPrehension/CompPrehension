package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.QuestionStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.AnswerObjectRepository;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Transactional
public abstract class AbstractAuthorizationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionMetadataRepository questionMetadataRepository;
    @Autowired private AnswerObjectRepository answerObjectRepository;

    @PersistenceContext private EntityManager entityManager;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").with(user("integration-test")))
                .build();
    }

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    protected void actingAs(long userId) {
        TestUserService.actAs(userId);
    }

    protected ExerciseAttemptEntity createAttempt(long userId, long exerciseId, Long courseId) {
        var attempt = new ExerciseAttemptEntity();
        attempt.setUser(userRepository.findById(userId).orElseThrow());
        attempt.setExercise(exerciseRepository.findById(exerciseId).orElseThrow());
        attempt.setCourse(courseId == null ? null : courseRepository.findById(courseId).orElseThrow());
        attempt.setAttemptStatus(AttemptStatus.INCOMPLETE);
        attempt.setQuestions(new ArrayList<>());

        exerciseAttemptRepository.save(attempt);
        entityManager.flush();
        return attempt;
    }

    /** Незавершённая попытка студента в главном курсе. */
    protected ExerciseAttemptEntity createMainCourseAttempt() {
        return createAttempt(TestData.MAIN_COURSE_STUDENT_ID, TestData.MAIN_COURSE_EXERCISE_ID, TestData.MAIN_COURSE_ID);
    }

    /** Попытка вне курса. */
    protected ExerciseAttemptEntity createGlobalPoolAttempt() {
        return createAttempt(TestData.GLOBAL_STUDENT_ID, TestData.GLOBAL_POOL_EXERCISE_ID, null);
    }

    protected QuestionEntity createQuestion(@Nullable ExerciseAttemptEntity attempt) {
        var metadata = questionMetadataRepository
                .findById(TestData.EXPRESSION_QUESTION_METADATA_ID)
                .orElseThrow(() -> new IllegalStateException("Не найден questions_meta с id " + TestData.EXPRESSION_QUESTION_METADATA_ID));
        var bankQuestion = bankDataOf(metadata);

        var question = new QuestionEntity();
        question.setExerciseAttempt(attempt);
        question.setDomainEntity(domainRepository.findById(TestData.DOMAIN_ID).orElseThrow());
        question.setMetadata(metadata);
        question.setQuestionStatus(QuestionStatus.VIEWED);
        question.setQuestionType(bankQuestion.getQuestionType());
        question.setQuestionText(bankQuestion.getQuestionText());
        question.setQuestionName(bankQuestion.getQuestionName());
        question.setQuestionDomainType(bankQuestion.getQuestionDomainType());
        question.setOptions(bankQuestion.getOptions());
        question.setTags(new ArrayList<>(metadata.getQuestionData().getData().getTags()));
        question.setStatementFacts(bankQuestion.getStatementFacts().stream()
                .map(f -> new BackendFactEntity(
                        f.getSubjectType(), f.getSubject(), f.getVerb(), f.getObjectType(), f.getObject()))
                .collect(Collectors.toCollection(ArrayList::new)));
        question.setSolutionFacts(new ArrayList<>());
        question.setInteractions(new ArrayList<>());
        question.setAnswerObjects(new ArrayList<>());

        questionRepository.save(question);
        entityManager.flush();

        for (SerializableQuestion.AnswerObject source : bankQuestion.getAnswerObjects()) {
            var answer = new AnswerObjectEntity();
            answer.setQuestion(question);
            answer.setAnswerId(source.getAnswerId());
            answer.setHyperText(source.getHyperText());
            answer.setDomainInfo(source.getDomainInfo());
            answer.setRightCol(source.isRightCol());
            answer.setConcept(source.getConcept());
            answerObjectRepository.save(answer);
            question.getAnswerObjects().add(answer);
        }
        entityManager.flush();

        return question;
    }

    protected QuestionEntity createQuestionWithoutAttempt() {
        return createQuestion(null);
    }

    private SerializableQuestion.QuestionData bankDataOf(QuestionMetadataEntity metadata) {
        var questionData = metadata.getQuestionData();
        if (questionData == null || questionData.getData() == null) {
            throw new IllegalStateException(
                    "questions_data для метаданных " + metadata.getId() + " не найдены, проверьте data.sql");
        }
        return questionData.getData().getQuestionData();
    }
}
