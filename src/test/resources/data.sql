INSERT INTO `domain` (`id`, `name`) VALUES ('1', 'test');

INSERT INTO `course` (`id`, `description`, `name`) VALUES ('1', 'тест', 'тест');

INSERT INTO `backend` (`id`, `name`) VALUES (1, 'test');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `backend_id`, `course_id`, `domain_id` )
VALUES ('5', '1', '1', '0', '1', '10', 'test', '8', '1', '1', '1', '1');

INSERT INTO `user` (`id`, `email`, `first_name`, `last_name`, `login`, `password`, `preferred_language`)
VALUES (1, 'test', 'test', 'test', 'test', 'test', 0);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (4,1,5,1);
