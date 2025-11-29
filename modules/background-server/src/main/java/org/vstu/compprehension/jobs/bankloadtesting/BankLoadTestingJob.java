package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IteratorUtils;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.Random;
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

    @Autowired
    public BankLoadTestingJob(FrontendService frontendService, UserRepository userRepository, BankLoadTestingJobConfig config) {
        this.frontendService = frontendService;
        this.userRepository = userRepository;
        this.config = config;
    }

    @Job(name = "question-bank-load-testing-job", retries = 0)
    public void run() {
        try {
            runImpl(config);
        } catch (Exception e) {
            log.error("Bank loadint test exception - {}", e.getMessage(), e);
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
        var attempt = frontendService.createExerciseAttempt(exerciseId, userId);
        if (attempt == null) {
            throw new Exception("Could not create exercise attempt for exercise " + exerciseId);
        }
        
        Thread.sleep(1000L * random.nextInt(config.exerciseStartDelayMin, config.exerciseStartDelayMax));
        log.info("User {} starts his attempt", userId);
        
        var decision = Decision.CONTINUE;
        while (!decision.equals(Decision.FINISH)) {
            
            var question = frontendService.generateQuestion(attempt.getAttemptId());
            decision = question.getFeedback().getStrategyDecision();

            Thread.sleep(1000L * random.nextInt(config.questionDurationMin, config.questionDurationMax));
            Thread.sleep(1000L * random.nextInt(config.postQuestionDelayMin, config.postQuestionDelayMax));
        }

        log.info("User {} finished his attempt", userId);
    }
}
