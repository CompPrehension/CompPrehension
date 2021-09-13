package org.vstu.compprehension.Service;

import lombok.SneakyThrows;
import lombok.val;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.BackendTaskQueue;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.repository.BackendRepository;
import org.vstu.compprehension.models.entities.BackendEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Singleton
public class BackendService {
    @Autowired
    private BackendRepository backendRepository;

    @Autowired
    private BackendTaskQueue agent;

    public BackendEntity getDefaultBackend() {
        Iterator<BackendEntity> iterator = backendRepository.findAll().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        throw new NoSuchElementException("В базе нет backend-а по умолчанию");
    }

    @SneakyThrows
    public List<BackendFactEntity> judge(Backend backend, List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs) {
        val back = backend;
        return agent.postAsync(() -> back.judge(laws, statement, correctAnswer, response, violationVerbs)).get();
    }

    @SneakyThrows
    public List<BackendFactEntity> solve(Backend backend, List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs) {
        val back = backend;
        return agent.postAsync(() -> back.solve(laws, statement, solutionVerbs)).get();
    }
}
