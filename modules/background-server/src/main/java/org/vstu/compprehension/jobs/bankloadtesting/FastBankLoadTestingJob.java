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
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.date.DateTimeProvider;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.time.Instant;
import java.util.PriorityQueue;
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
    
    /**
     * Запуск быстрой дискретно-событийной симуляции
     */
    public void runSimulation(BankLoadTestingJobConfig config) {
        // Очередь событий, отсортированная по времени исполнения
        PriorityQueue<SimulationEvent> queue = new PriorityQueue<>();

        // Сбрасываем (или устанавливаем) начальное время симуляции
        dateTimeProvider.setTime(Instant.now());

        var random = randomProvider.getRandom();

        // 1. Подготовка данных (синхронно, мгновенно)
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
        for (Long userId : userIds) {
            // Вычисляем случайную задержку перед стартом
            long startDelaySeconds = random.nextInt(config.getExerciseStartDelayMin(), config.getExerciseStartDelayMax() + 1);
            Instant startTime = dateTimeProvider.now().plusSeconds(startDelaySeconds);

            // Добавляем событие в очередь
            queue.add(new SimulationEvent(
                    startTime,
                    () -> handleStartAttempt(queue, config, userId, questionsNumber)
            ));
        }

        // 3. Главный цикл обработки событий (Event Loop)
        long eventsProcessed = 0;
        while (!queue.isEmpty()) {
            SimulationEvent event = queue.poll();

            // Перемещаем глобальные часы симуляции в момент события
            dateTimeProvider.setTime(event.executionTime);

            // Выполняем логику события (БД запросы и планирование следующих шагов)
            event.task.run();

            eventsProcessed++;
            if (eventsProcessed % 100 == 0) {
                log.debug("Processed {} events. Current Virtual Time: {}", eventsProcessed, dateTimeProvider.now());
            }
        }

        log.info("Simulation finished. Processed {} events. Final Virtual Time: {}", eventsProcessed, dateTimeProvider.now());
    }

    // --- ЛОГИКА СОБЫТИЙ (STEP HANDLERS) ---

    /**
     * Шаг 1: Создание попытки и планирование первого вопроса
     */
    @SneakyThrows
    private void handleStartAttempt(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, int maxAttemptQuestions) {
        log.info("User {} started exercise attempt at {}", userId, dateTimeProvider.now());

        // Реальный запрос в БД (займет миллисекунды реального времени, но в базе будет время из simulationClock)
        Long attemptId = createExerciseAttempt(config.getExerciseId(), userId).getAttemptId();

        // Планируем решение первого вопроса (сразу же или с минимальной задержкой)
        scheduleQuestionStart(queue, config, userId, attemptId, 0, maxAttemptQuestions);
    }

    /**
     * Вспомогательный метод: Планирование начала решения вопроса
     */
    private void scheduleQuestionStart(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions) {
        // Мы планируем событие на "сейчас" (текущее виртуальное время), так как паузы уже отработаны
        queue.add(new SimulationEvent(
                dateTimeProvider.now().plusMillis(2),
                () -> handleQuestionStart(queue, config, userId, attemptId, questionIndex, maxQuestions)
        ));
    }

    /**
     * Шаг 2: Генерация вопроса и "думание"
     */
    @SneakyThrows
    private void handleQuestionStart(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions) {
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
                () -> handleQuestionFinish(queue, config, userId, attemptId, questionIndex, maxQuestions)
        ));
    }

    /**
     * Шаг 3: Завершение вопроса и пауза перед следующим
     */
    private void handleQuestionFinish(PriorityQueue<SimulationEvent> queue, BankLoadTestingJobConfig config, long userId, Long attemptId, int questionIndex, int maxQuestions) {
        log.info("User {} completed #{} problem at {}", userId, questionIndex + 1, dateTimeProvider.now());

        int nextIndex = questionIndex + 1;

        // Если вопросы кончились - выходим
        if (nextIndex >= maxQuestions) {
            log.info("User {} finished exercise attempt", userId);
            return;
        }

        // Рассчитываем паузу МЕЖДУ вопросами (Post Question Delay)
        var random = randomProvider.getRandom();
        long postDelaySeconds = random.nextInt(config.getPostQuestionDelayMin(), config.getPostQuestionDelayMax() + 1);

        // Планируем старт следующего вопроса в будущем
        Instant nextQuestionStartTime = dateTimeProvider.now().plusSeconds(postDelaySeconds);

        queue.add(new SimulationEvent(
                nextQuestionStartTime,
                () -> handleQuestionStart(queue, config, userId, attemptId, nextIndex, maxQuestions)
        ));
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

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

    /**
     * Упрощенный Retry без Thread.sleep, так как мы в Single Threaded Simulation.
     * Если база залочена, лучше сразу упасть или повторить сразу же, 
     * так как sleep остановит всю вселенную.
     */
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
