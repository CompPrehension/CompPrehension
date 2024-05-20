package org.vstu.compprehension.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.models.repository.*;

import java.util.Optional;

@Service
public class AuthorizationService {

    private final PermissionScopeRepository permissionScopeRepository;
    private final PermissionRepository permissionRepository;
    private final RoleUserAssignmentRepository roleUserAssignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthorizationService(PermissionScopeRepository permissionScopeRepository, PermissionRepository permissionRepository,
                                RoleUserAssignmentRepository roleUserAssignmentRepository, UserRepository userRepository,
                                RoleRepository roleRepository) {
        this.permissionScopeRepository = permissionScopeRepository;
        this.permissionRepository = permissionRepository;
        this.roleUserAssignmentRepository = roleUserAssignmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Checks if a user is authorized to perform a specific action based on their user ID, permission name, permission scope kind, and owner ID.
     *
     * @param  userId            the ID of the user
     * @param  permissionName    the name of the permission
     * @param  permissionScopeKind  the kind of permission scope
     * @param  ownerId           the optional ID of the owner
     * @return                   true if the user is authorized, false otherwise
     */
    public boolean isAuthorized(long userId, String permissionName, PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        try {
            var permissionScope = _getPermissionScope(permissionScopeKind, ownerId).orElse(null);
            var permission = permissionRepository.findByName(permissionName).orElse(null);

            if (permission == null || permissionScope == null) {
                return false;
            }

            var user = userRepository.findById(userId).orElse(null);

            var roles = roleUserAssignmentRepository.findAllByUserAndPermissionScope(user, permissionScope).stream()
                    .map(RoleUserAssignmentEntity::getRole).toList();

            return roles.stream().anyMatch(role -> role.getPermissions().contains(permission));

        } catch (Exception e) {
            return false;
        }
    }

    private Optional<PermissionScopeEntity> _getPermissionScope(PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        if (ownerId.isEmpty()) {
            return permissionScopeRepository.findByKind(permissionScopeKind);
        } else {
            return permissionScopeRepository.findByOwnerIdAndKind(ownerId.get(), permissionScopeKind);
        }
    }

    /**
     * Adds a user role to the database.
     *
     * @param  userId            the ID of the user
     * @param  roleName          the name of the role to add
     * @param  permissionScopeKind  the kind of permission scope
     * @param  ownerId           the optional ID of the owner
     * @throws EntityNotFoundException if the role or permission scope is not found
     */
    public void addUserRole(long userId, String roleName, PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        var role = roleRepository.findByName(roleName).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        var permissionScope = _getPermissionScope(permissionScopeKind, ownerId).orElseThrow(() -> new EntityNotFoundException("PermissionScope not found"));
        var user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Role not found"));

        var userRoles = user.getRoleUserAssignments().stream().map(RoleUserAssignmentEntity::getRole).toList();
        if (userRoles.contains(role)) {
            return;
        }

        var newRole = new RoleUserAssignmentEntity();
        newRole.setUser(user);
        newRole.setRole(role);
        newRole.setPermissionScope(permissionScope);

        user.getRoleUserAssignments().add(newRole);
        userRepository.save(user);
    }

    /**
     * Deletes a user role from the database.
     *
     * @param  userId            the ID of the user
     * @param  roleName          the name of the role to delete
     * @param  permissionScopeKind  the kind of permission scope
     * @param  ownerId           the optional ID of the owner
     * @throws EntityNotFoundException if the role or permission scope is not found
     */
    public void deleteUserRole(long userId, String roleName, PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        var role = roleRepository.findByName(roleName).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        var permissionScope = _getPermissionScope(permissionScopeKind, ownerId).orElseThrow(() -> new EntityNotFoundException("PermissionScope not found"));
        var user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Role not found"));

        var userRoles = user.getRoleUserAssignments();

        var roleToRemove = userRoles.stream()
                .filter(assignment -> assignment.getRole().equals(role) && assignment.getPermissionScope().equals(permissionScope))
                .findFirst()
                .orElse(null);
        if (roleToRemove == null) {
            return;
        }

        user.getRoleUserAssignments().remove(roleToRemove);
        userRepository.save(user);
    }
}
