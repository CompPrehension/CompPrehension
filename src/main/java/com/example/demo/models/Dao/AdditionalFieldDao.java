package com.example.demo.models.Dao;


import com.example.demo.models.entities.AdditionalField;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AdditionalFieldDao extends CrudRepository<AdditionalField, Long> {
    Optional<AdditionalField> findAdditionalFieldById(Long id);
}
