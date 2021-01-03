package com.example.demo.models.repository;

import com.example.demo.models.entities.FeedbackEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends CrudRepository<FeedbackEntity, Long> {
}
