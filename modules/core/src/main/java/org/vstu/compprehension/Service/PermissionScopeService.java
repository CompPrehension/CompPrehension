package org.vstu.compprehension.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.repository.PermissionScopeRepository;

import java.util.Optional;

@Service
public class PermissionScopeService {
    private final PermissionScopeRepository permissionScopeRepository;

    @Autowired
    public PermissionScopeService(PermissionScopeRepository permissionScopeRepository) {
        this.permissionScopeRepository = permissionScopeRepository;
    }

    public PermissionScopeEntity getOrCreatePermissionScope(PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        Optional<PermissionScopeEntity> foundPermissionScope;

        if (ownerId.isEmpty()) {
            foundPermissionScope = permissionScopeRepository.findByKind(permissionScopeKind);
        } else {
            foundPermissionScope = permissionScopeRepository.findByOwnerIdAndKind(ownerId.get(), permissionScopeKind);
        }

        if (foundPermissionScope.isPresent()) {
            return foundPermissionScope.get();
        }

        var newPermissionScope = new PermissionScopeEntity();
        newPermissionScope.setKind(permissionScopeKind);
        newPermissionScope.setOwnerId(ownerId.orElse(null));
        return permissionScopeRepository.save(newPermissionScope);
    }
}
