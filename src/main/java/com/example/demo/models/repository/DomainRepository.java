package com.example.demo.models.repository;


import com.example.demo.models.entities.DomainEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends CrudRepository<DomainEntity, String> {
}
