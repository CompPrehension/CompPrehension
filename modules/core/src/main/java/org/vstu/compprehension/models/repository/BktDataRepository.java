package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.BktDataEntity;

@Repository
public interface BktDataRepository extends CrudRepository<BktDataEntity, String> {

}
