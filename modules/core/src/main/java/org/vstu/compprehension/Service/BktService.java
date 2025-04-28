package org.vstu.compprehension.Service;

import io.grpc.StatusRuntimeException;
import its.model.definition.ThisShouldNotHappen;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.bkt.grpc.BktServiceGrpc;
import org.vstu.compprehension.bkt.grpc.UpdateRosterRequest;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.List;

/**
 * Единая точка доступа к python-модели BKT.
 */
@Service
@RequiredArgsConstructor
public class BktService {

    private final BktServiceGrpc.BktServiceBlockingStub stub;
    private final DomainRepository domainRepository;

    public void updateBktRoster(DomainEntity domain, String userId, Boolean isCorrect, List<String> domainSkills) {
        val roster = domain.getBktRoster();
        if (roster != null && !roster.isBlank()) {
            val request = UpdateRosterRequest.newBuilder()
                    .setStudent(userId)
                    .setRoster(roster)
                    .setCorrect(isCorrect)
                    .addAllSkills(domainSkills)
                    .build();

            try {
                val response = stub.updateRoster(request); // blocking

                val updatedRoster = response.getRoster();
                domain.setBktRoster(updatedRoster);
                domainRepository.save(domain);
            } catch (StatusRuntimeException ignored) {
                throw new ThisShouldNotHappen();
            }
        }
    }
}
