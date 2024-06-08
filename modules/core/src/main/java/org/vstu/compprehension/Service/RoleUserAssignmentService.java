package org.vstu.compprehension.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.models.repository.RoleUserAssignmentRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class RoleUserAssignmentService {

    private final RoleUserAssignmentRepository roleUserAssignmentRepository;

    @Autowired
    public RoleUserAssignmentService(RoleUserAssignmentRepository roleUserAssignmentRepository) {
        this.roleUserAssignmentRepository = roleUserAssignmentRepository;
    }

    public void saveIfDoesNotExist(Collection<RoleUserAssignmentEntity> entities) {
        List<RoleUserAssignmentEntity> entitiesToSave = new ArrayList<>();
        List<RoleUserAssignmentEntity> entitiesToRemove = new ArrayList<>();

        for (var entity : entities) {

            var existsRoles = roleUserAssignmentRepository.findUsersWithRoleOnPermissionScope(
                    entity.getUser().getId(),
                    entity.getPermissionScope().getId()
            );

            if (existsRoles.stream().anyMatch(r -> r.getRole().getId() == entity.getRole().getId())) {
                continue;
            } else if (!existsRoles.isEmpty()) {
                entitiesToRemove.addAll(existsRoles);
            }

            entitiesToSave.add(entity);
        }

        roleUserAssignmentRepository.deleteAll(entitiesToRemove);
        roleUserAssignmentRepository.saveAll(entitiesToSave);
    }
}
