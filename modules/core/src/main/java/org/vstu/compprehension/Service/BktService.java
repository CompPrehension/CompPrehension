package org.vstu.compprehension.Service;

import io.grpc.StatusRuntimeException;
import its.model.definition.ThisShouldNotHappen;
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
    public void updateBktRoster(String domainId, String userId, boolean correct, List<String> domainSkills) {
        val data = repository.findById(domainId).orElseThrow();
        val roster = data.getRoster();

        if (roster.isBlank()) return;

        val request = UpdateRosterRequest.newBuilder()
                .setStudent(userId)
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
