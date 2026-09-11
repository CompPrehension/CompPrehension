package org.vstu.compprehension.service;

import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.Nullable;
import its.model.definition.ThisShouldNotHappen;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.bkt.grpc.*;
import org.vstu.compprehension.repositories.data.BktDataRepository;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(BktServiceGrpc.BktServiceBlockingStub.class)
public class BktService {

    private final BktServiceGrpc.BktServiceBlockingStub stub;
    private final BktDataRepository bktData;

    @Retryable(
            includes = { ObjectOptimisticLockingFailureException.class },
            maxRetries = 9,
            delay = 100
    )
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateBktRoster(String domainId, Long userId, boolean correct, List<String> skills) {
        val roster = getRoster(domainId, userId);
        if (roster == null) return;

        val request = UpdateRosterRequest.newBuilder()
                .setStudent(userId.toString())
                .setRoster(roster)
                .setCorrect(correct)
                .addAllSkills(skills)
                .build();

        try {
            val response = stub.updateRoster(request); // blocking

            bktData.updateRoster(domainId, userId, response.getRoster());
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<SkillState> getSkillStates(String domainId, Long userId, List<String> skills) {
        val roster = getRoster(domainId, userId);
        if (roster == null) return Collections.emptyList();

        val request = GetSkillStatesRequest.newBuilder()
                .setRoster(roster)
                .setStudent(userId.toString())
                .addAllSkills(skills)
                .build();

        try {
            val response = stub.getSkillStates(request); // blocking

            return response.getSkillStatesList();
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<String> chooseBestQuestion(String domainId, Long userId, List<String> skills) {
        val roster = getRoster(domainId, userId);
        if (roster == null) return Collections.emptyList();

        val request = ChooseBestQuestionRequest.newBuilder()
                .setRoster(roster)
                .setStudent(userId.toString())
                .addAllAllSkills(skills)
                .setMaxQuestionSkillsCount(3) // Увеличение приводит к экспоненциальному росту времени вычисления
                .build();

        try {
            val response = stub.chooseBestQuestion(request); // blocking

            return response.getBestTargetSkillsList();
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }

    private @Nullable String getRoster(String domainId, Long userId) {
        val roster = bktData.findRoster(domainId, userId).orElse(null);
        return roster == null || roster.isBlank() ? null : roster;
    }
}
