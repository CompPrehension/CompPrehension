package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.BktUserDataEntity;

@Repository
public interface BktUserDataRepository extends CrudRepository<BktUserDataEntity, BktUserDataEntity.BktUserDataId> {

}
