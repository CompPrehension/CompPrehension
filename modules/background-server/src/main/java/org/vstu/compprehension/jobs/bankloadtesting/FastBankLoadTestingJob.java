package org.vstu.compprehension.jobs.bankloadtesting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.date.DateTimeProvider;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;

import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Log4j2
@Service
public class FastBankLoadTestingJob {
    private final FrontendService frontendService;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final RandomProvider randomProvider;
    private final QuestionGenerationRequestRepository questionGenerationRequestRepository;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final DateTimeProvider dateTimeProvider;
    private final BankLoadTestingJobConfig config;
    private final BankLoadTestingJobBatchConfig batchConfig;
    private final DomainFactory domainFactory;
    private final QuestionBank questionBank;
    private final InteractionRepository interactionRepository;
    private final QuestionMetadataSearchRequestRepository questionMetadataSearchRequestRepository;

    public FastBankLoadTestingJob(FrontendService frontendService, ExerciseRepository exerciseRepository, UserRepository userRepository, RandomProvider randomProvider, QuestionMetadataRepository questionMetadataRepository, QuestionGenerationRequestRepository questionGenerationRequestRepository, DateTimeProvider dateTimeProvider, BankLoadTestingJobConfig config, BankLoadTestingJobBatchConfig batchConfig, DomainFactory domainFactory, QuestionBank questionBank, InteractionRepository interactionRepository, QuestionMetadataSearchRequestRepository questionMetadataSearchRequestRepository) {
        this.frontendService = frontendService;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.randomProvider = randomProvider;
        this.questionGenerationRequestRepository = questionGenerationRequestRepository;
        this.questionMetadataRepository = questionMetadataRepository;
        this.dateTimeProvider = dateTimeProvider;
        this.config = config;
        this.batchConfig = batchConfig;
        this.domainFactory = domainFactory;
        this.questionBank = questionBank;
        this.interactionRepository = interactionRepository;
        this.questionMetadataSearchRequestRepository = questionMetadataSearchRequestRepository;
    }

    public void run() {
        try {
            runImpl(config);
        } catch (Exception e) {
            log.error("Bank loading test exception - {}", e.getMessage(), e);
            throw e;
        }
    }

    public void runBatch() {
        try {
            runBatchImpl(batchConfig);
        } catch (Exception e) {
            log.error("Bank loading test exception - {}", e.getMessage(), e);
            throw e;
        }
    }

    @SneakyThrows
    synchronized void runBatchImpl(BankLoadTestingJobBatchConfig batchConfig) {

        for(int genThreshold = batchConfig.getGeneratorThresholdFrom(); genThreshold <= batchConfig.getGeneratorThresholdTo(); genThreshold += batchConfig.getGeneratorThresholdStep()) {
            for (int safeMargin = batchConfig.getGeneratorAdditionalQuestionsToGenerateFrom(); safeMargin <= batchConfig.getGeneratorAdditionalQuestionsToGenerateTo(); safeMargin += batchConfig.getGeneratorAdditionalQuestionsToGenerateStep()) {
                
                log.info("Generating experiment starts with generatorThreshold: {} and additionalQuestionsToGenerate: {}", genThreshold, safeMargin);
                var config = new BankLoadTestingJobConfig();
                config.setRandomSeed(1111);
                config.setGeneratorThreshold(genThreshold);
                config.setGeneratorAdditionalQuestionsToGenerate(safeMargin);
                config.setExerciseId(batchConfig.getExerciseId());
                config.setUsersCount(batchConfig.getUsersCount());
                config.setExerciseStartDelayMin(batchConfig.getExerciseStartDelayMin());
                config.setExerciseStartDelayMax(batchConfig.getExerciseStartDelayMax());
                config.setPostQuestionDelayMin(batchConfig.getPostQuestionDelayMin());
                config.setPostQuestionDelayMax(batchConfig.getPostQuestionDelayMax());
                config.setSkipDelayForQuestionsWithoutGeneration(batchConfig.isSkipDelayForQuestionsWithoutGeneration());
                config.setQuestionDelayRandomFactorization(batchConfig.getQuestionDelayRandomFactorization());

                var retryNumber = 0;
                Exception lastException = null;
                while (++retryNumber <= 3) {
                    lastException  = null;

                    try {
                        runImpl(config);
                        break;
                    } catch (Exception e) {
                        log.error("Generating experiment exception - {}", e.getMessage(), e);
                        lastException = e;
                    }
                }

                if (lastException == null) {
                    log.info("Generating experiment finished successfully with generatorThreshold: {} and additionalQuestionsToGenerate: {}", genThreshold, safeMargin);
                } else {
                    log.error("Generating experiment finished with errors with generatorThreshold: {} and additionalQuestionsToGenerate: {}", genThreshold, safeMargin);
                }

                // завершаем все открытые запросы на генерацию
                var cancelledCount = questionGenerationRequestRepository.cancelAllActiveRequests();
            }
        }
    }

    public void runImpl(BankLoadTestingJobConfig config) {
        dbCleanup();

        PriorityQueue<SimulationEvent> queue = new PriorityQueue<>();

        Instant realStart = Instant.now();
        Instant virtualStart = realStart;
        dateTimeProvider.setTime(virtualStart);

        var random = randomProvider.getRandom();

        // 1. Подготовка данных
        var questionsNumber = exerciseRepository.findById(config.getExerciseId()).orElseThrow()
                .getStages().stream()
                .map(ExerciseStageEntity::getNumberOfQuestions)
                .mapToInt(Integer::intValue)
                .sum();

        var users = IteratorUtils.toList(userRepository.findAll().iterator());
        var userIds = users.stream()
                .map(UserEntity::getId)
                .sorted()
                .limit(config.getUsersCount())
                .toList();

        if (userIds.isEmpty()) throw new IllegalArgumentException("Users count is 0");

        log.info("Starting simulation for {} users. Total questions per user: {}", userIds.size(), questionsNumber);

        // 2. Планирование первых событий (Start Attempt) для каждого студента
        Set<Long> runningAttempts = new HashSet<>(); // Множество активных попыток
        for (Long userId : userIds) {
            long startDelaySeconds = random.nextInt(config.getExerciseStartDelayMin(), config.getExerciseStartDelayMax() + 1);
            Instant startTime = dateTimeProvider.now().plusSeconds(startDelaySeconds);
            queue.add(new SimulationEvent(
                    startTime,
                    () -> handleStartAttempt(queue, config, userId, questionsNumber, runningAttempts)
            ));
        }

        // Запускаем Генератор       
        Set<Integer> processingRequests = new HashSet<>();  // Множество для отслеживания запросов, которые генератор уже "взял в работу"
        queue.add(new SimulationEvent(
                dateTimeProvider.now(),
                () -> handleGeneratorHeartbeat(queue, processingRequests, runningAttempts)
        ));

        // Запускаем цикл прогнозирования спроса (каждые 30 секунд)
        if (config.isDemandPredictorEnabled()) {
            var questionRequests = getAllStagesQuestionRequests(config.getExerciseId());
            queue.add(new SimulationEvent(
                    dateTimeProvider.now(),
                    () -> handleDemandPrediction(queue, questionRequests, processingRequests, runningAttempts)
            ));
        }

        // Event Loop
        long eventsProcessed = 0;
        while (!queue.isEmpty()) {
            SimulationEvent event = queue.poll();

            // Перемещаем глобальные часы симуляции в момент события
            dateTimeProvider.setTime(event.executionTime);

            // Выполняем логику события (БД запросы и планирование следующих шагов)
            event.task.run();

            eventsProcessed++;
            if (eventsProcessed % 100 == 0) {
                log.info("Events: {}, VirtualTime: {}", eventsProcessed, dateTimeProvider.now());
            }
        }

        Instant virtualEnd = dateTimeProvider.now();
        Duration virtualDuration = Duration.between(virtualStart, virtualEnd);
        Duration realDuration = Duration.between(realStart, Instant.now());

        log.info("=== SIMULATION FINISHED ===");
        log.info("Processed Events: {}", eventsProcessed);
        log.info("Simulation covered: {} virtual days ({} hours)", virtualDuration.toDays(), virtualDuration.toHours());
        log.info("Real Execution Time: {} ms", realDuration.toMillis());
        log.info("Ratio (Speedup): x{}", virtualDuration.toMillis() / Math.max(1, realDuration.toMillis()));
        dbCleanup();
    }
    
    private void dbCleanup() {
        questionMetadataSearchRequestRepository.deleteAll();
        questionGenerationRequestRepository.cancelAllActiveRequests();
        log.info("Start cleaning bank from previous attempts");
        var deletedMetadatas = questionMetadataRepository.deleteMetadataFromDate(LocalDate.now().minusDays(2));
        log.info("Finish cleaning bank from previous attempts with {} deleted metadatas", deletedMetadatas);
    }
    
    private ArrayList<QuestionRequest> getAllStagesQuestionRequests(long exerciseId) {
        var exerciseSettings = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found with id: " + exerciseId));
        var stages = exerciseSettings.getStages();

        var domain = domainFactory.getDomain("expression_dt");
        
        var questionRequests = new ArrayList<QuestionRequest>(stages.size());
        for (var stage : stages) {
            var targetConcepts = stage.getConcepts().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedConcepts = stage.getConcepts().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetLaws = stage.getLaws().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .map(c -> domain.getLaw(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedLaws = stage.getLaws().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .map(c -> domain.getLaw(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetTags = exerciseSettings.getTags().stream()
                    .map(domain::getTag)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetSkills = stage.getSkills().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .map(c -> domain.getSkill(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedSkills = stage.getSkills().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .map(c -> domain.getSkill(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            var qr = QuestionRequest.builder()
                    .targetConcepts(targetConcepts)
                    .deniedConcepts(deniedConcepts)
                    .targetLaws(targetLaws)
                    .deniedLaws(deniedLaws)
                    .targetSkills(targetSkills)
                    .deniedSkills(deniedSkills)
                    .complexity(stage.getComplexity())
                    .targetTags(targetTags)
                    .domainShortname(domain.getShortnameForQuestionSearch())
                    .build();
            qr = domain.ensureQuestionRequestValid(qr);

            questionRequests.add(qr);
        }
        
        return questionRequests;
    }

    private void handleDemandPrediction(PriorityQueue<SimulationEvent> queue, List<QuestionRequest> questionRequests, Set<Integer> processingRequests, Set<Long> runningAttempts) {
        try {
            if (processingRequests.isEmpty() || runningAttempts.isEmpty()) {
                return;
            }
            
            log.debug("Starting demand prediction cycle at {}", dateTimeProvider.now());

            for (QuestionRequest qr : questionRequests) {
                var searchRequest = questionBank.createBankSearchRequest(qr);

                // 2. Готовим данные для ML
                // TODO: Здесь стоит брать реальные метрики из симуляции, пока стоят заглушки (0)
                List<FeatureItem> features = new ArrayList<>(6);
                features.add(new FeatureItem("feature_students_currently_solving", 0));
                features.add(new FeatureItem("feature_active_students_global", 0));
                features.add(new FeatureItem("feature_avg_completion_sec", 0));
                features.add(new FeatureItem("feature_demand_rolling_5min", 0));
                features.add(new FeatureItem("delta_rolling_demand", 0));
                features.add(new FeatureItem("delta_active_solving", 0));

                // 3. Прогноз
                var predictedDemand = predict(features);
                var predictedDemandRounded = Math.round(predictedDemand);

                // 4. Генерация при необходимости
                if (predictedDemandRounded > 0) {
                    var currentlyGeneratingQuestions = questionGenerationRequestRepository.findNumberOfCurrentlyGeneratingQuestions(qr.getDomainShortname(), searchRequest);
                    var toGenerate = (int) (predictedDemandRounded - currentlyGeneratingQuestions);
                    if (toGenerate > 0) {
                        log.info("Prediction triggered generation of {} questions", toGenerate);
                        var generationRequest = new QuestionGenerationRequestEntity(searchRequest, toGenerate, qr.getExerciseAttemptId());
                        questionGenerationRequestRepository.save(generationRequest);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in demand prediction cycle: {}", e.getMessage());
        }

        // Планируем следующий запуск через 30 секунд виртуального времени
        queue.add(new SimulationEvent(
                dateTimeProvider.now().plusSeconds(30),
                () -> handleDemandPrediction(queue, questionRequests, processingRequests, runningAttempts)
        ));
    }

    private void wtf(long exerciseId) {
        var exerciseSettings = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found with id: " + exerciseId));
        var stages = exerciseSettings.getStages();
        var domain = domainFactory.getDomain("expresssion_dt");
        
        // для каждой стадии находим qr ей соответвующий
        var questionRequests = new ArrayList<QuestionRequest>(stages.size());
        for (var stage : stages) {
            var targetConcepts = stage.getConcepts().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedConcepts = stage.getConcepts().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetLaws = stage.getLaws().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .map(c -> domain.getLaw(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedLaws = stage.getLaws().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .map(c -> domain.getLaw(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetTags = exerciseSettings.getTags().stream()
                    .map(domain::getTag)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var targetSkills = stage.getSkills().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                    .map(c -> domain.getSkill(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            var deniedSkills = stage.getSkills().stream()
                    .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                    .map(c -> domain.getSkill(c.getName()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            var qr = QuestionRequest.builder()
                    .targetConcepts(targetConcepts)
                    .deniedConcepts(deniedConcepts)
                    .targetLaws(targetLaws)
                    .deniedLaws(deniedLaws)
                    .targetSkills(targetSkills)
                    .deniedSkills(deniedSkills)
                    .complexity(stage.getComplexity())
                    .targetTags(targetTags)
                    .domainShortname(domain.getShortnameForQuestionSearch())
                    .build();
            qr = domain.ensureQuestionRequestValid(qr);
            
            questionRequests.add(qr);
        }
        
        // запрос на генерацию
        var qr = questionRequests.get(0);
        var searchRequest = questionBank.createBankSearchRequest(qr);
        
        // вызов рассчета фич
        

       // Готовим данные (строго соблюдая порядок, как при обучении!)
        List<FeatureItem> features = new ArrayList<>(6);
        features.add(new FeatureItem("feature_students_currently_solving", 0));
        features.add(new FeatureItem("feature_active_students_global", 0));
        features.add(new FeatureItem("feature_avg_completion_sec", 0));
        features.add(new FeatureItem("feature_demand_rolling_5min", 0));
        features.add(new FeatureItem("delta_rolling_demand", 0));
        features.add(new FeatureItem("delta_active_solving", 0));
        var predictedDemand = predict(features);
        var predictedDemandRounded = Math.round(predictedDemand);
        
        if (predictedDemandRounded > 0) {
            var currentlyGeneratingQuestions = questionGenerationRequestRepository.findNumberOfCurrentlyGeneratingQuestions(qr.getDomainShortname(), searchRequest);
            var toGenerate = (int)(predictedDemandRounded - currentlyGeneratingQuestions);
            if  (toGenerate > 0) {
                var generationRequest = new QuestionGenerationRequestEntity(searchRequest, toGenerate, qr.getExerciseAttemptId());
                questionGenerationRequestRepository.save(generationRequest);
            }
        }
        
    }
    
    private MLModelClient mlModelClient = new MLModelClient("http://localhost:8000");    
    private Double predict(List<FeatureItem> features) {
        try {
            // Вызываем
            Double result = mlModelClient.predict("lr_2", features);
            log.info("Результат прогноза: " + result);
        } catch (Exception e) {
            // Ловим ошибки (например, FEATURE_MISMATCH или MODEL_NOT_FOUND)
            log.error("Не удалось получить прогноз: " + e.getMessage());
        }
        
        return 0.0;
    }

    @SneakyThrows
    private void handleStartAttempt(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, int maxAttemptQuestions, Set<Long> runningAttempts) {
        log.info("User {} started exercise attempt at {}", userId, dateTimeProvider.now());

        // Реальный запрос в БД (займет миллисекунды реального времени, но в базе будет время из simulationClock)
        Long attemptId = createExerciseAttempt(config.getExerciseId(), userId).getAttemptId();
        runningAttempts.add(attemptId);

        // Планируем решение первого вопроса (сразу же или с минимальной задержкой)
        scheduleQuestionStart(queue, config, userId, attemptId, 0, maxAttemptQuestions, runningAttempts);
    }

    private void scheduleQuestionStart(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions, Set<Long> runningAttempts) {
        queue.add(new SimulationEvent(
                dateTimeProvider.now().plusMillis(2),
                () -> handleQuestionStart(queue, config, userId, attemptId, questionIndex, maxQuestions, runningAttempts)
        ));
    }

    @SneakyThrows
    private void handleQuestionStart(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions, Set<Long> runningAttempts) {
        var random = randomProvider.getRandom();

        // Генерируем вопрос (взаимодействие с БД)
        var questionId = Objects.requireNonNull(generateQuestion(attemptId)).getQuestionId();        

        // Рассчитываем, сколько студент будет "думать" (Virtual Time Calculation)
        double duration = getQuestionDelaySeconds(random.nextDouble(), null);

        // Планируем момент ЗАВЕРШЕНИЯ вопроса
        long thinkTimeMillis = (long) (duration * 1000);
        Instant finishTime = dateTimeProvider.now().plusMillis(thinkTimeMillis);

        queue.add(new SimulationEvent(
                finishTime,
                () -> handleQuestionFinish(queue, config, userId, attemptId, questionId, questionIndex, maxQuestions, runningAttempts)
        ));
    }

    private void handleQuestionFinish(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, long questionId, int questionIndex, int maxQuestions, Set<Long> runningAttempts) {
        log.info("User {} completed #{} problem at {}", userId, questionIndex + 1, dateTimeProvider.now());

        // Добавляем один ответ, триггерящий интерацию после delay'a
        // такая интерация нужна, чтобы в бд зафиксировать окончание вопроса
        interactionRepository.createFakeInteraction(SEND_RESPONSE.toString(), questionId, dateTimeProvider.now());
        
        int nextIndex = questionIndex + 1;
        // Если вопросы кончились - выходим
        if (nextIndex >= maxQuestions) {
            queue.add(new SimulationEvent(
                    dateTimeProvider.now().plusMillis(2),
                    () -> handleAttemptFinish(queue, userId, attemptId, runningAttempts)
            ));
            return;
        }

        // Рассчитываем паузу МЕЖДУ вопросами (Post Question Delay)
        var random = randomProvider.getRandom();
        long postDelaySeconds = random.nextInt(config.getPostQuestionDelayMin(), config.getPostQuestionDelayMax() + 1);

        // Планируем старт следующего вопроса в будущем
        Instant nextQuestionStartTime = dateTimeProvider.now().plusSeconds(postDelaySeconds);

        queue.add(new SimulationEvent(
                nextQuestionStartTime,
                () -> handleQuestionStart(queue, config, userId, attemptId, nextIndex, maxQuestions, runningAttempts)
        ));
    }

    private void handleAttemptFinish(PriorityQueue<SimulationEvent> queue, Long userId, Long attemptId, Set<Long> runningAttempts) {
        log.info("User {} finished exercise attempt", userId);
        runningAttempts.remove(attemptId);
    }

    /**
     * Шаг 1 Генератора: Анализ очереди и планирование работы
     */
    private void handleGeneratorHeartbeat(PriorityQueue<SimulationEvent> queue, Set<Integer> processingRequests, Set<Long> runningAttempts) {        
        // 1. Ищем запросы, которые ожидают генерации (PENDING/IN_PROGRESS)
        var pendingRequests = questionGenerationRequestRepository.findAllActual("expression_dt", LocalDateTime.ofInstant(dateTimeProvider.now(), ZoneId.systemDefault()).minusMonths(3));

        var random = randomProvider.getRandom();
        if (pendingRequests != null) {
            for (var req : pendingRequests) {
                for (var subreq : req.getGenerationRequests()) {
                    if (processingRequests.contains(subreq.id())) {
                        continue; // Уже варится
                    }

                    // Берем в работу
                    processingRequests.add(subreq.id());

                    // для каждого вопроса планируем время на его генерацию
                    double maxQuestionGenerationTimeSeconds = 0;
                    for(int i = 0; i < subreq.questionsToGenerate(); i++) {
                        double genDurationSeconds = getGeneratorProcessingTime(random.nextDouble());
                        Instant finishTime = dateTimeProvider.now().plusMillis((long)(genDurationSeconds * 1000));
                        maxQuestionGenerationTimeSeconds = Math.max(maxQuestionGenerationTimeSeconds, genDurationSeconds);

                        queue.add(new SimulationEvent(
                                finishTime,
                                () -> handleQuestionGenerationStep(queue, subreq.id(), subreq.questionsToGenerate(), processingRequests)
                        ));

                        log.debug("Generator picked up request {} at {}. Will finish at {}", subreq.id(), dateTimeProvider.now(), finishTime);
                    }
                    
                    // по завершении планируем задачу по завершению запроса на генерацию
                    queue.add(new SimulationEvent(
                            dateTimeProvider.now().plusMillis((long)(maxQuestionGenerationTimeSeconds * 1000)),
                            () -> handleGenerationStepFinish(queue, subreq.id(), processingRequests)
                    ));
                }
            }
        }

        if (runningAttempts.isEmpty()) {
            return;
        }

        // 4. Планируем следующий тик проверки (например, каждые 1-5 секунд виртуального времени)
        // Это обеспечивает "постоянную проверку между шагами"
        queue.add(new SimulationEvent(
                dateTimeProvider.now().plusSeconds(5), // Проверяем очередь каждые 5 сек
                () -> handleGeneratorHeartbeat(queue, processingRequests, runningAttempts)
        ));
    }

    private void handleQuestionGenerationStep(PriorityQueue<SimulationEvent> queue, int requestId, int questionsToGenerate, Set<Integer> processingRequests) {
        // 1. Выполняем реальную логику генерации (сохранение в БД)
        executeSimpleRetry(() -> {
            var questionRequestForGenRequestId = questionGenerationRequestRepository.getQuestionRequestId(requestId);
            if (questionRequestForGenRequestId.isEmpty()) {
                return null;
            }

            var bankSearchRequest = questionMetadataSearchRequestRepository.findSearchRequestByQuestionRequestId(questionRequestForGenRequestId.get());            
            var metadataForRequestId = questionMetadataRepository.findTopRatedMetadata(bankSearchRequest, 1);
            if (metadataForRequestId.isEmpty()) {
                return null;
            }

            for (int i = 0; i < questionsToGenerate; ++i) {
                var newQuestionName = "new_question_" + requestId + "_" + i;
                var clonedMetadata = questionMetadataRepository.clone(metadataForRequestId.get(0).getId(), newQuestionName, newQuestionName, Date.from(dateTimeProvider.now()), requestId);

                // Предположим, нужно просто пометить запрос выполненным и создать вопрос:
                log.debug("generated question for request {} with metadata {}", requestId, clonedMetadata);
            }

            log.info("generated {} questions for request {}", questionsToGenerate, requestId);

            return null;
        });
    }

    private void handleGenerationStepFinish(PriorityQueue<SimulationEvent> queue, int requestId, Set<Integer> processingRequests) {
        executeSimpleRetry(() -> {
            questionGenerationRequestRepository.updateGenerationRequests(new Integer[] { requestId });
            return null;
        });

        processingRequests.remove(requestId);
    }

    private @Nullable QuestionDto generateQuestion(Long attemptId) {
        return executeSimpleRetry(() -> {
            try {
                return frontendService.generateQuestion(attemptId);
            } catch (NullPointerException ignored) {
                return null;
            }
        });
    }

    @SneakyThrows
    private ExerciseAttemptDto createExerciseAttempt(Long exerciseId, Long userId) {
        return executeSimpleRetry(() -> frontendService.createExerciseAttempt(exerciseId, userId));
    }

    @SneakyThrows
    private <T> T executeSimpleRetry(Callable<T> callback) {
        int maxRetries = 3;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callback.call();
            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                // В реальной многопоточности тут нужен sleep. 
                // В DES (дискретной симуляции) sleep нельзя. 
                // Просто пробуем еще раз, надеясь, что транзакция была сброшена.
            }
        }
        return null; // Should not execute
    }

    private static final Double[] defaultQuestionSolveFactorization = new Double[]{ 13.698911565177422, -465.5577244968546, 5807.635692396597, -26371.472191593017, 55482.38583228885, -54371.642536328945, 20124.57534014809 };

    private static double getQuestionDelaySeconds(double u, Double[] factorization) {
        if (factorization == null) factorization = defaultQuestionSolveFactorization;
        double raw = 0;
        for (int i = 0; i < factorization.length; i++) {
            raw += factorization[i] * Math.pow(u, i);
        }
        if (raw <= 0) return 0;
        if (raw >= 200) return 200;
        return raw;
    }

    private static final Double[] defaultQuestionGenerationFactorization = new Double[]{ -10.564476, 1815.617740, -62755.468392, 1034010.074099, -9314469.105164, 50671099.251030, -176462616.225144, 406388639.475639, -625836438.027879, 637425652.546357, -411975523.119385, 152970035.001251, -24838886.394478 };

    private static double getGeneratorProcessingTime(double u) {
        var factorization = defaultQuestionGenerationFactorization;
        double raw = 0;
        for (int i = 0; i < factorization.length; i++) {
            raw += factorization[i] * Math.pow(u, i);
        }
        if (raw <= 0) return 0;
        if (raw >= 600) return 600;
        return raw;
    }

    @AllArgsConstructor
    static class SimulationEvent implements Comparable<SimulationEvent> {
        Instant executionTime; // Виртуальное время, когда событие должно произойти
        Runnable task;         // Логика события

        @Override
        public int compareTo(SimulationEvent other) {
            return this.executionTime.compareTo(other.executionTime);
        }
    }

    // 1. Единичный признак (Feature)
    class FeatureItem {
        private String key;
        private Object value; // Object, чтобы принимать и int, и double, и string

        public FeatureItem(String key, Object value) {
            this.key = key;
            this.value = value;
        }
        // Getters
        public String getKey() { return key; }
        public Object getValue() { return value; }
    }

    // 2. Тело запроса
    class PredictionRequest {
        private String model;
        private List<FeatureItem> features;

        public PredictionRequest(String model, List<FeatureItem> features) {
            this.model = model;
            this.features = features;
        }
        // Getters for Jackson
        public String getModel() { return model; }
        public List<FeatureItem> getFeatures() { return features; }
    }

    // 3. Успешный ответ
    @JsonIgnoreProperties(ignoreUnknown = true)
    class PredictionResponse {
        private String model;
        private Double prediction;

        // Пустой конструктор нужен Jackson'у
        public PredictionResponse() {}

        public Double getPrediction() { return prediction; }
        public String getModel() { return model; }
    }

    // 4. Ответ с ошибкой (от нашего Python сервера)    
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ErrorResponse {        
        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("message")
        private String message;

        // Пустой конструктор
        public ErrorResponse() {}

        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
    }

    public class MLModelClient {
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final String serverUrl;

        public MLModelClient(String serverUrl) {
            this.serverUrl = serverUrl;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            this.objectMapper = new ObjectMapper();
        }

        public Double predict(String modelName, List<FeatureItem> features) throws Exception {
            // 1. Собираем объект запроса
            PredictionRequest requestPayload = new PredictionRequest(modelName, features);

            // 2. Превращаем объект в JSON строку
            String jsonBody = objectMapper.writeValueAsString(requestPayload);

            // 3. Формируем HTTP запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10)) // Таймаут на ожидание ответа
                    .build();

            // 4. Отправляем
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Обработка успешного ответа (Код 200)
            if (response.statusCode() == 200) {
                PredictionResponse success = objectMapper.readValue(response.body(), PredictionResponse.class);
                return success.getPrediction();
            }

            // 6. Обработка ошибок (400, 404, 500)
            else {
                handleError(response);
                return null; // Java требует return, но handleError всегда кидает exception
            }
        }

        private void handleError(HttpResponse<String> response) throws Exception {
            String body = response.body();
            String errorCode = "UNKNOWN_ERROR";
            String message = "Unknown error occurred";

            try {
                // Пытаемся распарсить JSON ошибки от нашего сервера
                // Сначала смотрим, не лежит ли ошибка внутри стандартного поля 'detail' (FastAPI style)
                JsonNode root = objectMapper.readTree(body);

                if (root.has("detail")) {
                    JsonNode detail = root.get("detail");
                    // Если detail это объект с нашими полями (как мы сделали в Python)
                    if (detail.has("error_code")) {
                        errorCode = detail.get("error_code").asText();
                        message = detail.get("message").asText();
                    } else {
                        // Если это стандартная ошибка валидации FastAPI (обычно массив или строка)
                        message = detail.toString();
                        errorCode = "VALIDATION_ERROR";
                    }
                } else if (root.has("error_code")) {
                    // Если мы вернули ошибку напрямую (в global handler)
                    errorCode = root.get("error_code").asText();
                    message = root.get("message").asText();
                }
            } catch (Exception e) {
                // Если вернулся не JSON, а html или просто текст (например 502 Bad Gateway от Nginx)
                message = "Raw server response: " + body;
            }

            throw new RuntimeException("ML Server Error [" + response.statusCode() + "]: (" + errorCode + ") " + message);
        }
    }
}
