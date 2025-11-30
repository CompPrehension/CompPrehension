package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class BankLoadTestingJob {
    private final FrontendService frontendService;
    private final UserRepository userRepository;
    private final BankLoadTestingJobConfig config;
    private final Random random = new Random();
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public BankLoadTestingJob(FrontendService frontendService, UserRepository userRepository, BankLoadTestingJobConfig config, PlatformTransactionManager txManager) {
        this.frontendService = frontendService;
        this.userRepository = userRepository;
        this.config = config;
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        var users = IteratorUtils.toList(userRepository.findAll().iterator());
        var userIds = users.stream()
                .map(UserEntity::getId)
                .sorted((l, r) -> random.nextInt())
                .limit(config.usersCount)
                .toList();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("Users count is 0");
        }

        ExecutorService executor = Executors.newFixedThreadPool(config.usersCount);
        for (long userId : userIds) {
            executor.submit(() -> {
                try {
                    runUserExerciseAttempt(config, userId);
                } catch (Exception e) {
                    log.error("Error in user {} exercise attempt thread: {}", userId, e.getMessage(), e);
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

        Thread.sleep(1000L * random.nextInt(config.exerciseStartDelayMin, config.exerciseStartDelayMax));
        log.info("User {} starts his attempt", userId);

        var decision = Decision.CONTINUE;
        while (!decision.equals(Decision.FINISH)) {
            var question = generateQuestion(attemptId);            
            if (question == null) {
                continue;
            }
            
            log.info("User {} started question {}. Stage: {}", userId, question.getQuestionId(), question.getQuestionId());

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

            log.info("User {} completed question {}", userId, question.getQuestionId());

            Thread.sleep(1000L * random.nextInt(config.questionDurationMin, config.questionDurationMax));
            Thread.sleep(1000L * random.nextInt(config.postQuestionDelayMin, config.postQuestionDelayMax));
        }

        log.info("User {} finished his attempt", userId);
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
            log.debug("Problem question found for attempt {}", exAttemptId);
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
