package org.vstu.compprehension.service;

import io.grpc.StatusRuntimeException;
import its.model.definition.ThisShouldNotHappen;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.bkt.grpc.*;
import org.vstu.compprehension.models.entities.BktUserDataEntity;
import org.vstu.compprehension.models.repository.BktDomainDataRepository;
import org.vstu.compprehension.models.repository.BktUserDataRepository;

import java.util.Collections;
import java.util.List;

/**
 * Единая точка доступа к python-модели BKT
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(BktServiceGrpc.BktServiceBlockingStub.class)
public class BktService {

    private final BktServiceGrpc.BktServiceBlockingStub stub;
    private final BktDomainDataRepository bktDomainDataRepository;
    private final BktUserDataRepository bktUserDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 10,
            backoff = @Backoff(delay = 100)
    )
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateBktRoster(String domainId, Long userId, boolean correct, List<String> skills) {
        val data = getBktData(domainId, userId);
        if (data == null) return;
        val roster = data.getRoster();
        if (roster.isBlank()) return;

        val request = UpdateRosterRequest.newBuilder()
                .setStudent(userId.toString())
                .setRoster(roster)
                .setCorrect(correct)
                .addAllSkills(skills)
                .build();

        try {
            val response = stub.updateRoster(request); // blocking

            val updatedRoster = response.getRoster();
            data.setRoster(updatedRoster);
            entityManager.flush();
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<SkillState> getSkillStates(String domainId, Long userId, List<String> skills) {
        val data = getBktData(domainId, userId);
        if (data == null) return Collections.emptyList();
        val roster = data.getRoster();
        if (roster.isBlank()) return Collections.emptyList();

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
        val data = getBktData(domainId, userId);
        if (data == null) return Collections.emptyList();
        val roster = data.getRoster();
        if (roster.isBlank()) return Collections.emptyList();

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

    @Transactional(propagation = Propagation.MANDATORY)
    private @Nullable BktUserDataEntity getBktData(String domainId, Long userId) {
        val bktDomainData = bktDomainDataRepository.findById(domainId).orElse(null);
        if (bktDomainData == null) return null;
        // Проверяем наличие roster у домена. Он обязателен для использования bkt
        val emptyRoster = bktDomainData.getEmptyRoster();
        if (emptyRoster.isBlank()) return null;

        // Достаем roster пользователя, либо создаем новый пустой
        val dataId = new BktUserDataEntity.BktUserDataId(userId, domainId);
        return bktUserDataRepository.findById(dataId).orElseGet(() -> {
            // Создаем новую запись
            val newData = new BktUserDataEntity();
            newData.setUserId(userId);       // ключ‑часть #1
            newData.setDomainName(domainId); // ключ‑часть #2
            newData.setRoster(emptyRoster);  // дефолт‑roster (пустой, заполнится позже)
            try {
                return bktUserDataRepository.save(newData);
            } catch (DataIntegrityViolationException ignored) {
                return bktUserDataRepository.findById(dataId).orElseThrow();
            }
        });
    }
}
