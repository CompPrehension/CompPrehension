package org.vstu.compprehension.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.RoleAssignmentDTO;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.models.repository.*;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorizationService {

    private final PermissionScopeRepository permissionScopeRepository;
    private final RoleUserAssignmentRepository roleUserAssignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthorizationService(PermissionScopeRepository permissionScopeRepository,
                                RoleUserAssignmentRepository roleUserAssignmentRepository, UserRepository userRepository,
                                RoleRepository roleRepository) {
        this.permissionScopeRepository = permissionScopeRepository;
        this.roleUserAssignmentRepository = roleUserAssignmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Checks if a user is authorized to perform a specific action globally based on their user ID and permission name.
     *
     * @param  userId        the ID of the user
     * @param  permissionName    the name of the permission
     * @return               true if the user is authorized, false otherwise
     */
    public boolean isAuthorizedGlobal(long userId, String permissionName) {
        return isAuthorized(userId, permissionName, PermissionScopeKind.GLOBAL, Optional.empty());
    }

    /**
     * Checks if a user is authorized to perform a specific action in a course based on their user ID, permission name, and course ID.
     *
     * @param  userId            the ID of the user
     * @param  permissionName    the name of the permission
     * @param  courseId          the ID of the course
     * @return                   true if the user is authorized, false otherwise
     */
    public boolean isAuthorizedCourse(long userId, String permissionName, long courseId) {
        return isAuthorized(userId, permissionName, PermissionScopeKind.COURSE, Optional.of(courseId));
    }

    /**
     * Checks if a user is authorized to perform a specific action in any course or globally based on their user ID, permission name, and course ID.
     *
     * @param  userId            the ID of the user
     * @param  permissionName    the name of the permission
     * @param  courseId          the ID of the course
     * @return                   true if the user is authorized, false otherwise
     */
    public boolean isAuthorizedAnyCourseOrGlobal(long userId, String permissionName, Optional<Long> courseId) {
        try {
            return roleUserAssignmentRepository.isUserAuthorizedForCourseOrGlobal(
                    userId,
                    permissionName,
                    courseId);
        } catch (Exception e) {
            return false;
        }
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
            return roleUserAssignmentRepository.isUserAuthorized(
                    userId,
                    permissionName,
                    permissionScopeKind,
                    ownerId.orElse(null)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<PermissionScopeEntity> getPermissionScope(PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) {
        if (ownerId.isEmpty() || permissionScopeKind == PermissionScopeKind.GLOBAL) {
            return permissionScopeRepository.findByKind(permissionScopeKind);
        } else {
            return permissionScopeRepository.findByOwnerIdAndKind(ownerId.get(), permissionScopeKind);
        }
    }

    /**
     * Adds multiple user roles to the system.
     *
     * @param  userId  the ID of the user
     * @param  roles   a list of RoleAssignmentDTO objects representing the roles to be added
     */
    public void addUserRoles(long userId, List<RoleAssignmentDTO> roles) {
        for (var role : roles) {
            addUserRole(userId, role);
        }
    }

    /**
     * Adds a user role to the system.
     *
     * @param userId the ID of the user
     * @param roleAssignmentDTO the role assignment details
     * @throws EntityNotFoundException if the role or permission scope is not found
     */
    public void addUserRole(long userId, RoleAssignmentDTO roleAssignmentDTO) {
        var role = roleRepository.findByName(roleAssignmentDTO.roleName()).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        var permissionScope = getPermissionScope(roleAssignmentDTO.permissionScopeKind(), roleAssignmentDTO.ownerId()).orElseThrow(() -> new EntityNotFoundException("PermissionScope not found"));
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
        var permissionScope = getPermissionScope(permissionScopeKind, ownerId).orElseThrow(() -> new EntityNotFoundException("PermissionScope not found"));
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
