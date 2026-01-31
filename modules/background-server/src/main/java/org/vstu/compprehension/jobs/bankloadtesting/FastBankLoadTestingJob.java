package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.GenerationRequestGroup;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.date.DateTimeProvider;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import javax.sql.rowset.CachedRowSet;
import java.sql.Date;
import java.time.*;
import java.time.temporal.TemporalUnit;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;

@Log4j2
@Service
public class FastBankLoadTestingJob {
    private final FrontendService frontendService;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final TransactionScope transactionScope;
    private final RandomProvider randomProvider;
    private final QuestionGenerationRequestRepository questionGenerationRequestRepository;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final DateTimeProvider dateTimeProvider;
    private final BankLoadTestingJobConfig config;
    private final BankLoadTestingJobBatchConfig batchConfig;

    public FastBankLoadTestingJob(FrontendService frontendService, ExerciseRepository exerciseRepository, UserRepository userRepository, TransactionScopeFactory transactionScopeFactory, RandomProvider randomProvider, QuestionMetadataRepository questionMetadataRepository, QuestionGenerationRequestRepository questionGenerationRequestRepository, DateTimeProvider dateTimeProvider, BankLoadTestingJobConfig config, BankLoadTestingJobBatchConfig batchConfig) {
        this.frontendService = frontendService;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.transactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
        this.randomProvider = randomProvider;
        this.questionGenerationRequestRepository = questionGenerationRequestRepository;
        this.questionMetadataRepository = questionMetadataRepository;
        this.dateTimeProvider = dateTimeProvider;
        this.config = config;
        this.batchConfig = batchConfig;
    }

    @Job(name = "question-bank-load-testing-job", retries = 0)
    public void run() {
        try {
            runSimulation(config);
        } catch (Exception e) {
            log.error("Bank loading test exception - {}", e.getMessage(), e);
            throw e;
        }
    }

    public void runSimulation(BankLoadTestingJobConfig config) {
        PriorityQueue<SimulationEvent> queue = new PriorityQueue<>();

        Instant realStart = Instant.now();
        Instant virtualStart = LocalDateTime.of(2029, 1, 1, 1, 1).toInstant(ZoneOffset.UTC);
        dateTimeProvider.setTime(virtualStart);

        var random = randomProvider.getRandom();

        // 1. Подготовка данных
        var questionsNumber = transactionScope.execute(() -> exerciseRepository.findById(config.getExerciseId()).orElseThrow()
                .getStages().stream()
                .map(ExerciseStageEntity::getNumberOfQuestions)
                .mapToInt(Integer::intValue)
                .sum());

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

        // Event Loop
        long eventsProcessed = 0;
        while (!queue.isEmpty()) {
            SimulationEvent event = queue.poll();

            // Перемещаем глобальные часы симуляции в момент события
            dateTimeProvider.setTime(event.executionTime);

            // Выполняем логику события (БД запросы и планирование следующих шагов)
            event.task.run();

            eventsProcessed++;
            if (eventsProcessed % 500 == 0) {
                log.debug("Events: {}, VirtualTime: {}", eventsProcessed, dateTimeProvider.now());
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

        // 1. Генерируем вопрос (взаимодействие с БД)
        generateQuestion(attemptId);

        // 2. Рассчитываем, сколько студент будет "думать" (Virtual Time Calculation)
        double duration = getQuestionDelaySeconds(random.nextDouble(), config.getQuestionDelayRandomFactorization());

        // 3. Планируем момент ЗАВЕРШЕНИЯ вопроса
        long thinkTimeMillis = (long) (duration * 1000);
        Instant finishTime = dateTimeProvider.now().plusMillis(thinkTimeMillis);

        queue.add(new SimulationEvent(
                finishTime,
                () -> handleQuestionFinish(queue, config, userId, attemptId, questionIndex, maxQuestions, runningAttempts)
        ));
    }

    private void handleQuestionFinish(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions, Set<Long> runningAttempts) {
        log.info("User {} completed #{} problem at {}", userId, questionIndex + 1, dateTimeProvider.now());

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
        var pendingRequests = transactionScope.execute(() ->
            questionGenerationRequestRepository.findAllActual("expression_dt", LocalDateTime.ofInstant(dateTimeProvider.now(), ZoneId.systemDefault()).minusMonths(3)) // Или ваш метод поиска активных
        );

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
                        double genDurationSeconds = getGeneratorProcessingTime();
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
            
            var metadataForRequestId = questionMetadataRepository.findByQuestionSearchRequestId(questionRequestForGenRequestId.get());
            if (metadataForRequestId.isEmpty()) {
                return null;
            }

            for (int i = 0; i < questionsToGenerate; ++i) {
                var newQuestionName = "new_question_" + requestId + "_" + i;
                var clonedMetadata = questionMetadataRepository.clone(metadataForRequestId.get(), newQuestionName, newQuestionName, Date.from(dateTimeProvider.now()), requestId);

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

    private double getGeneratorProcessingTime() {
        var random = randomProvider.getRandom();
        // Пример: Нормальное распределение (среднее 30с, отклонение 10с), минимум 5с
        double val = (random.nextGaussian() * 10) + 30;
        return Math.max(5.0, val);
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

    private static final Double[] defaultFactorization = new Double[]{ 13.698911565177422, -465.5577244968546, 5807.635692396597, -26371.472191593017, 55482.38583228885, -54371.642536328945, 20124.57534014809 };

    private static double getQuestionDelaySeconds(double u, Double[] factorization) {
        if (factorization == null) factorization = defaultFactorization;
        double raw = 0;
        for (int i = 0; i < factorization.length; i++) {
            raw += factorization[i] * Math.pow(u, i);
        }
        if (raw <= 0) return 0;
        if (raw >= 200) return 200;
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
}
