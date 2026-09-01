package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.auth.Role;
import org.vstu.compprehension.models.entities.role.RoleEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    List<RoleEntity> findByNameIn(Collection<Role> names);
}
