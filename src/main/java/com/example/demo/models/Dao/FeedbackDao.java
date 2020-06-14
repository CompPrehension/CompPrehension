package com.example.demo.models.Dao;

import com.example.demo.models.entities.Feedback;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FeedbackDao extends CrudRepository<Feedback, Long> {
    Optional<Feedback> findFeedbackById(Long id);
}
