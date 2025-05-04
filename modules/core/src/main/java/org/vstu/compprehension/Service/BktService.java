package org.vstu.compprehension.Service;

import io.grpc.StatusRuntimeException;
import its.model.definition.ThisShouldNotHappen;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.bkt.grpc.BktServiceGrpc;
import org.vstu.compprehension.bkt.grpc.GetSkillStatesRequest;
import org.vstu.compprehension.bkt.grpc.SkillState;
import org.vstu.compprehension.bkt.grpc.UpdateRosterRequest;
import org.vstu.compprehension.models.entities.BktDataEntity;
import org.vstu.compprehension.models.repository.BktDataRepository;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.Collections;
import java.util.List;

/**
 * Единая точка доступа к python-модели BKT.
 */
@Service
@RequiredArgsConstructor
public class BktService {

    private final BktServiceGrpc.BktServiceBlockingStub stub;
    private final BktDataRepository repository;
    private final DomainRepository domainRepository;

    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 10,
            backoff = @Backoff(delay = 100)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }

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

    @Transactional(propagation = Propagation.MANDATORY)
    private @Nullable BktDataEntity getBktData(String domainId, Long userId) {
        val domain = domainRepository.findById(domainId).orElseThrow();
        // Проверяем наличие roster у домена. Он обязателен для использования bkt
        val emptyRoster = domain.getEmptyRoster();
        if (emptyRoster == null || emptyRoster.isBlank()) return null;

        // Достаем roster пользователя, либо создаем новый пустой
        val dataId = new BktDataEntity.BktDataId(userId, domainId);
        return repository.findById(dataId).orElseGet(() -> {
            // Создаем новую запись
            val newData = new BktDataEntity();
            newData.setUserId(userId);       // ключ‑часть #1
            newData.setDomainName(domainId); // ключ‑часть #2
            newData.setRoster(emptyRoster);  // дефолт‑roster (пустой, заполнится позже)
            try {
                return repository.save(newData);
            } catch (DataIntegrityViolationException ignored) {
                return repository.findById(dataId).orElseThrow();
            }
        });
    }
}
