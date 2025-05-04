package org.vstu.compprehension.Service;

import io.grpc.StatusRuntimeException;
import its.model.definition.ThisShouldNotHappen;
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
import org.vstu.compprehension.bkt.grpc.UpdateRosterRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.BktDataEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.BktDataRepository;

import java.util.List;

/**
 * Единая точка доступа к python-модели BKT.
 */
@Service
@RequiredArgsConstructor
public class BktService {

    private final BktServiceGrpc.BktServiceBlockingStub stub;
    private final BktDataRepository repository;

    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 10,
            backoff = @Backoff(delay = 100)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBktRoster(Domain domain, UserEntity user, boolean correct, List<String> domainSkills) {
        // Проверяем наличие roster у домена. Он обязателен для использования bkt
        val emptyRoster = domain.getDomainEntity().getEmptyRoster();
        if (emptyRoster == null || emptyRoster.isBlank()) return;

        // Достаем roster пользователя, либо создаем новый пустой
        val dataId = new BktDataEntity.BktDataId(user.getId(), domain.getDomainId());
        val data = repository.findById(dataId).orElseGet(() -> {
            // Создаем новую запись
            val newData = new BktDataEntity();
            newData.setUser(user);                       // ключ‑часть #1
            newData.setDomain(domain.getDomainEntity()); // ключ‑часть #2
            newData.setRoster(emptyRoster);              // дефолт‑roster (пустой, заполнится позже)
            try {
                return repository.save(newData);
            } catch (DataIntegrityViolationException ignored) {
                return repository.findById(dataId).orElseThrow();
            }
        });
        val roster = data.getRoster();
        if (roster.isBlank()) return;

        val request = UpdateRosterRequest.newBuilder()
                .setStudent(user.getId().toString())
                .setRoster(roster)
                .setCorrect(correct)
                .addAllSkills(domainSkills)
                .build();

        try {
            val response = stub.updateRoster(request); // blocking

            val updatedRoster = response.getRoster();
            data.setRoster(updatedRoster);
        } catch (StatusRuntimeException ignored) {
            throw new ThisShouldNotHappen();
        }
    }
}
