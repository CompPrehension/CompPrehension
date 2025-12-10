package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.logging.log4j.ThreadContext;
import org.hibernate.exception.LockTimeoutException;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class BankLoadTestingJob {
    private final FrontendService frontendService;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final BankLoadTestingJobConfig config;
    private final BankLoadTestingJobBatchConfig batchConfig;
    private final TransactionScope transactionScope;
    private final RandomProvider randomProvider;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionGenerationRequestRepository questionGenerationRequestRepository;

    @Autowired
    public BankLoadTestingJob(FrontendService frontendService, ExerciseRepository exerciseRepository, UserRepository userRepository, BankLoadTestingJobConfig config, BankLoadTestingJobBatchConfig batchConfig, TransactionScopeFactory transactionScopeFactory, RandomProvider randomProvider, QuestionMetadataRepository questionMetadataRepository, QuestionGenerationRequestRepository questionGenerationRequestRepository) {
        this.frontendService = frontendService;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.config = config;
        this.batchConfig = batchConfig;
        this.transactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
        this.randomProvider = randomProvider;
        this.questionMetadataRepository = questionMetadataRepository;
        this.questionGenerationRequestRepository = questionGenerationRequestRepository;
    }

    @Job(name = "question-bank-load-testing-job", retries = 0)
    public void run() {
        try {
            runImpl(config);
        } catch (Exception e) {
            log.error("Bank loading test exception - {}", e.getMessage(), e);
            throw e;
        }
    }

    @Job(name = "question-bank-load-testing-batch-job", retries = 0)
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
        if (batchConfig.initialDelay != null &&  batchConfig.initialDelay > 0) {
            Thread.sleep(1000L * batchConfig.initialDelay);
        }
        
        for(int genThreshold = batchConfig.generatorThresholdFrom; genThreshold <= batchConfig.generatorThresholdTo; genThreshold += batchConfig.generatorThresholdStep) {
            for (int safeMargin = batchConfig.generatorAdditionalQuestionsToGenerateFrom; safeMargin <= batchConfig.generatorAdditionalQuestionsToGenerateTo; safeMargin += batchConfig.generatorAdditionalQuestionsToGenerateStep) {
                // ensure all gen requests cancelled
                transactionScope.execute(questionGenerationRequestRepository::cancelAllActiveRequests);
                
                log.info("Start cleaning bank from previous attempts");
                var deletedMetadatas = transactionScope.execute(() -> questionMetadataRepository.deleteMetadataFromDate(LocalDate.now().plusDays(-2)));
                log.info("Finish cleaning bank from previous attempts with {} deleted metadatas", deletedMetadatas);
                
                log.info("Generating experiment starts with generatorThreshold: {} and additionalQuestionsToGenerate: {}", genThreshold, safeMargin);
                var config = new BankLoadTestingJobConfig();
                config.randomSeed = 1111;
                config.generatorThreshold = genThreshold;
                config.generatorAdditionalQuestionsToGenerate = safeMargin;
                config.exerciseId = batchConfig.exerciseId;
                config.usersCount = batchConfig.usersCount;
                config.exerciseStartDelayMin = batchConfig.exerciseStartDelayMin;
                config.exerciseStartDelayMax = batchConfig.exerciseStartDelayMax;
                config.postQuestionDelayMin = batchConfig.postQuestionDelayMin;
                config.postQuestionDelayMax = batchConfig.postQuestionDelayMax;
                config.skipDelayForQuestionsWithoutGeneration = batchConfig.skipDelayForQuestionsWithoutGeneration;
                config.questionDelayRandomFactorization = batchConfig.questionDelayRandomFactorization;

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
                // дожидаемся, пока закончит работу генератор
                var cancelledCount = transactionScope.execute(questionGenerationRequestRepository::cancelAllActiveRequests);
                if (cancelledCount != null && cancelledCount > 0) {
                    Thread.sleep(1000 * 30);
                }
            }
        }
    }

    @SneakyThrows
    synchronized void runImpl(BankLoadTestingJobConfig config) {
        if (config.randomSeed != null) {
            randomProvider.reset(config.randomSeed);
        }
        var random = randomProvider.getRandom();
        
        // set overridable settings
        if (config.generatorThreshold != null) {
            transactionScope.executeNoResult(() -> {
                var exercise = exerciseRepository.findById(config.exerciseId)
                        .orElseThrow();
                exercise.getOptions().setGeneratorThreshold(config.generatorThreshold);
                exercise.getOptions().setGeneratorAdditionalQuestionsToGenerate(config.generatorAdditionalQuestionsToGenerate);
                exerciseRepository.save(exercise);
            });
        }

        var questionsNumber = transactionScope.execute(() ->exerciseRepository.findById(config.exerciseId).orElseThrow()
                .getStages().stream()
                .map(ExerciseStageEntity::getNumberOfQuestions)
                .mapToInt(Integer::intValue)
                .sum());

        var users = IteratorUtils.toList(userRepository.findAll().iterator());
        var userIds = users.stream()
                .map(UserEntity::getId)
                .sorted()
                // .sorted((l, r) -> random.nextInt())
                .limit(config.usersCount)
                .toList();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("Users count is 0");
        }
        log.info("User ids to be used for experiment: {}", userIds);

        ExecutorService executor = Executors.newFixedThreadPool(config.usersCount);
        for (long userId : userIds) {
            executor.submit(() -> {
                try {
                    ThreadContext.put("userId", String.valueOf(userId));
                    runUserExerciseAttempt(config, userId, questionsNumber);
                } catch (Exception e) {
                    log.error("Error in user {} exercise attempt thread: {}", userId, e.getMessage(), e);
                } finally {
                    ThreadContext.remove("userId");
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while awaiting executor termination", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("All {} user exercise attempts completed.", userIds.size());
    }

    private void runUserExerciseAttempt(BankLoadTestingJobConfig config, long userId, int maxAttemptQuestions) throws Exception {
        var exerciseId = config.exerciseId;
        Long attemptId = createExerciseAttempt(exerciseId, userId).getAttemptId();
        
        var random = randomProvider.getRandom();
        Thread.sleep(1000L * random.nextInt(config.exerciseStartDelayMin, config.exerciseStartDelayMax + 1));

        log.info("User {} started exercise attempt", userId);

        Long latsGenerationRequestId = null;
        for(int i = 0; i < maxAttemptQuestions; i++) {
            generateQuestion(attemptId);

            var questionSolveDuration = getQuestionDelaySeconds(random.nextDouble(), config.questionDelayRandomFactorization)
                    + random.nextInt(config.postQuestionDelayMin, config.postQuestionDelayMax + 1);

            // skip delay if generation request was not creates
            if (latsGenerationRequestId == null) {
                latsGenerationRequestId = questionGenerationRequestRepository.getLastRequestByExerciseAttemptId(attemptId).orElse(null);
                if (latsGenerationRequestId == null && config.skipDelayForQuestionsWithoutGeneration) {
                    log.debug("User {} skipped question solve delay because no generation requests have been found", userId);
                    questionSolveDuration = 0;
                }
            }

            if (questionSolveDuration > 0) {
                Thread.sleep((long)(1000 * questionSolveDuration));
            }

            log.info("User {} completed #{} problem", userId, i+1);
        }

        log.info("User {} finished exercise attempt", userId);
    }

    private @Nullable QuestionDto generateQuestion(Long exAttemptId) {
        /*
            return executeWithRetry(() -> {
                try {
                    return frontendService.generateQuestion(attemptId);
                } catch (NullPointerException ignored) {
                    return null;
                }
            }, "generateQuestion");
        */
        
        try {
            return frontendService.generateQuestion(exAttemptId);
        } catch (NullPointerException ignored) {
            // log.debug("Problem question found for attempt {}", exAttemptId);
            return null;
        }
    }
    
    @SneakyThrows
    private ExerciseAttemptDto createExerciseAttempt(Long exerciseId, Long userId) {
        return executeWithRetry(() -> {
            return frontendService.createExerciseAttempt(exerciseId, userId);
        }, "createExerciseAttempt");
    }

    private static final Double[] defaultFactorization = new Double[]{ 13.698911565177422, -465.5577244968546, 5807.635692396597, -26371.472191593017, 55482.38583228885, -54371.642536328945, 20124.57534014809 };

    private static double getQuestionDelaySeconds(double u, Double[] factorization) {
        if (factorization == null) {
            factorization = defaultFactorization;
        }

        double raw = 0;
        for (int i = 0; i < factorization.length; i++) {
            raw += factorization[i] * Math.pow(u, i);
        }

        if (raw <= 0) {
            return 0;
        }
        if (raw >= 200) {
            return 200;
        }

        return raw;
    }

    private <T> T executeWithRetry(Callable<T> callback, String operation) throws InterruptedException {
        int maxRetries = 25;
        long baseDelay = 50;
        long maxDelay = 5000L;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callback.call();
            } catch (Exception e) {
                if (isDeadlockException(e) && attempt < maxRetries) {
                    long delay = Math.min(baseDelay * (1L << attempt), maxDelay); // Exponential backoff capped at 5s
                    // log.warn("Deadlock in {} for user {}, retry {}/{} after {}ms", operation, Thread.currentThread().getName(), attempt + 1, maxRetries, delay);
                    Thread.sleep(delay);
                    continue;
                }
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        }
        throw new RuntimeException("Max retries exceeded for " + operation);
    }

    private boolean isDeadlockException(Exception e) {
        return e.getCause() instanceof org.hibernate.exception.LockAcquisitionException ||
                "Deadlock found when trying to get lock".equals(e.getMessage()) ||
                e instanceof LockTimeoutException ||
                e instanceof PessimisticLockingFailureException ||
                e instanceof org.springframework.dao.CannotAcquireLockException ||
                e instanceof org.springframework.transaction.UnexpectedRollbackException;
    }

}
