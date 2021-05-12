INSERT INTO `domain` (`version`, `name`) VALUES (1, 'ProgrammingLanguageExpressionDomain');
INSERT INTO `domain` (`version`, `name`) VALUES (1, 'ControlFlowStatementsDomain');

INSERT INTO `course` (`id`, `description`, `name`) VALUES (1, 'test', 'test');

INSERT INTO `backend` (`id`, `name`) VALUES (1, 'test');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (5, 1, 1, 0, 1, '10', 'test', 8, 1, 1, 1, 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (6, 1, 1, 0, 1, '10', 'test1', 8, 1, 1, 1, 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (7, 1, 1, 0, 1, '10', 'test2', 8, 1, 1, 1, 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (8, 1, 1, 0, 1, '10', 'test3', 8, 1, 1, 1, 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,Python');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (9, 1, 1, 0, 1, '10', 'test_type', 8, 1, 1, 1, 'ProgrammingLanguageExpressionDomain', 'type');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id`, `tags` )
VALUES (10, 1, 1, 0, 1, '10', 'test', 8, 1, 1, 1, 'ControlFlowStatementsDomain', 'C++');

INSERT INTO `user` (`id`, `email`, `first_name`, `last_name`, `login`, `password`, `preferred_language`)
VALUES (1, 'test', 'test', 'test', 'test', 'test', 0);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (4,1,5,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 0, 5);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 1, 5);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 5);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (5,1,8,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 0, 8);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 8);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 1, 8);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (7,1,6,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 0, 6);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 0, 6);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 6);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (9,1,7,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 1, 7);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 0, 7);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 7);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (11,1,9,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('type', 0, 9);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 9);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (12,1,5,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 2, 5);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 2, 5);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (13,1,8,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 2, 8);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 2, 8);

INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('trace', 0, 10);
