package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends CrudRepository<ViolationEntity, Long> {
    @Query("select ve from ViolationEntity ve where ve.id in :ids")
    List<ViolationEntity> findByIds(@Param("ids") Iterable<Long> ids);
    @Query("select ve from ViolationEntity ve where ve.interaction.id = :iId and ve.lawName in :lawNames")
    List<ViolationEntity> findByLawNames(@Param("iId") Long interactionId, @Param("lawNames") Iterable<String> lawNames);
}
