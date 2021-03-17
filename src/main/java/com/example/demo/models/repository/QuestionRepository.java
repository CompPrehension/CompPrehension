package com.example.demo.models.repository;

import com.example.demo.models.entities.QuestionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends CrudRepository<QuestionEntity, Long> {
//    public QuestionEntity findQuestion(HashSet<String> target, HashSet<String> allowed, HashSet<String> denied) {
//        @Query("SELECT a FROM A a WHERE a.userGroups not in (:userGroups)")
//        List<A> getAWithNoIntersectionInGroups(@param("userGroups") Set<UserGroup> userGroups)
//
//        CriteriaBuilder builder = em.getCriteriaBuilder();
//        CriteriaQuery<QuestionEntity> query = builder.createQuery(QuestionEntity.class);
//        Root<QuestionEntity> root = query.from(QuestionEntity.class);
//
//        ParameterExpression<String> conceptParam = builder.parameter(String.class);
//        query.select(root).where(builder.greaterThan(root.get(QuestionEntity.recipes).get(QuestionEntity.dateModified), dateParam));
//
//        CriteriaBuilder.In<String> inClause = builder.in(root.get("name"));
//        for (String title : target) {
//            inClause.value(title);
//        }
//        criteriaQuery.select(root).where(inClause);
//
//        return em.createQuery(query)
//                .setParameter(dateParam, dateModified)
//                .getSingleResult();
//    }
}

