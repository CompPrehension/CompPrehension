package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.entities.role.RoleEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    List<RoleEntity> findByNameIn(Collection<Role> names);
}
