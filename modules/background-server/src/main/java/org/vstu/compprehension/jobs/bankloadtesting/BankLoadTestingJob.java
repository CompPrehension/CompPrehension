package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.logging.log4j.ThreadContext;
import org.hibernate.exception.LockTimeoutException;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

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
    private final TransactionScope transactionScope;
    private final RandomProvider randomProvider;

    @Autowired
    public BankLoadTestingJob(FrontendService frontendService, ExerciseRepository exerciseRepository, UserRepository userRepository, BankLoadTestingJobConfig config, TransactionScopeFactory transactionScopeFactory, RandomProvider randomProvider) {
        this.frontendService = frontendService;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.config = config;
        this.transactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
        this.randomProvider = randomProvider;
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

    @SneakyThrows
    private synchronized void runImpl(BankLoadTestingJobConfig config) {
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

        var users = IteratorUtils.toList(userRepository.findAll().iterator());
        var userIds = users.stream()
                .map(UserEntity::getId)
                // .sorted((l, r) -> random.nextInt())
                .limit(config.usersCount)
                .toList();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("Users count is 0");
        }

        ExecutorService executor = Executors.newFixedThreadPool(config.usersCount);
        for (long userId : userIds) {
            executor.submit(() -> {
                try {
                    ThreadContext.put("userId", String.valueOf(userId));
                    runUserExerciseAttempt(config, userId);
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

    private void runUserExerciseAttempt(BankLoadTestingJobConfig config, long userId) throws Exception {
        var exerciseId = config.exerciseId;
        Long attemptId = createExerciseAttempt(exerciseId, userId).getAttemptId();
        
        var random = randomProvider.getRandom();
        Thread.sleep(1000L * random.nextInt(config.exerciseStartDelayMin, config.exerciseStartDelayMax));

        log.info("User {} starts exercise attempt", userId);

        var decision = Decision.CONTINUE;
        while (!decision.equals(Decision.FINISH)) {
            var question = generateQuestion(attemptId);            
            if (question == null) {
                continue;
            }
            
            log.debug("User {} started question {}. Stage: {}", userId, question.getQuestionId(), question.getQuestionId());

            var feedback = executeWithRetry(() -> frontendService.generateNextCorrectAnswer(question.getQuestionId()), "firstAnswer");

            if (feedback == null) {
                break;
            }
            while (feedback != null && feedback.getStepsLeft() > 0) {
                feedback = executeWithRetry(() -> frontendService.generateNextCorrectAnswer(question.getQuestionId()), "nextAnswers");
            }
            if (feedback == null) {
                break;
            }
            decision = feedback.getStrategyDecision();

            log.debug("User {} completed question {}", userId, question.getQuestionId());

            Thread.sleep(1000L * random.nextInt(config.questionDurationMin, config.questionDurationMax));
            Thread.sleep(1000L * random.nextInt(config.postQuestionDelayMin, config.postQuestionDelayMax));
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
