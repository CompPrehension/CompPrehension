SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `answer_object` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answer_id` int NOT NULL,
  `hyper_text` varchar(255) DEFAULT NULL,
  `domain_info` varchar(1000) DEFAULT NULL,
  `is_right_col` tinyint(1) DEFAULT NULL,
  `concept` varchar(255) DEFAULT NULL,
  `question_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_answerId__questionId` (`answer_id`,`question_id`),
  KEY `FK_ANSWEROBJECT_ON_QUESTION` (`question_id`),
  CONSTRAINT `FK_ANSWEROBJECT_ON_QUESTION` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bkt_domain_data` (
  `domain_name` varchar(255) NOT NULL,
  `empty_roster` text NOT NULL,
  PRIMARY KEY (`domain_name`),
  CONSTRAINT `fk_bkt_domain_data_domain` FOREIGN KEY (`domain_name`) REFERENCES `domain` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bkt_user_data` (
  `user_id` bigint NOT NULL,
  `domain_name` varchar(255) NOT NULL,
  `version` bigint NOT NULL,
  `roster` text NOT NULL,
  PRIMARY KEY (`user_id`,`domain_name`),
  KEY `fk_bkt_data_domain` (`domain_name`),
  CONSTRAINT `fk_bkt_data_domain` FOREIGN KEY (`domain_name`) REFERENCES `domain` (`name`),
  CONSTRAINT `fk_bkt_data_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `correct_law` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interaction_id` bigint NOT NULL,
  `law_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_CORRECTLAW_ON_INTERACTION` (`interaction_id`),
  CONSTRAINT `FK_CORRECTLAW_ON_INTERACTION` FOREIGN KEY (`interaction_id`) REFERENCES `interaction` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(512) NOT NULL,
  `external_course_id` varchar(255) DEFAULT NULL,
  `education_resource_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_course_edu_resource_external_course` (`education_resource_id`,`external_course_id`),
  CONSTRAINT `fk_course_education_resource` FOREIGN KEY (`education_resource_id`) REFERENCES `education_resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `domain` (
  `name` varchar(255) NOT NULL,
  `version` varchar(255) NOT NULL,
  `short_name` varchar(255) NOT NULL,
  `options_json` json NOT NULL,
  PRIMARY KEY (`name`),
  UNIQUE KEY `uc_e7da9cda2c1460840f30fc189` (`name`,`version`),
  UNIQUE KEY `uc_domain_shortname` (`short_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `education_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `url` varchar(512) NOT NULL,
  `type` varchar(64) NOT NULL,
  `trust_status` varchar(32) NOT NULL DEFAULT 'UNTRUSTED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_education_resource_url_type` (`url`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `exercise` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `max_retries` int DEFAULT NULL,
  `use_guiding_questions` tinyint(1) DEFAULT NULL,
  `hidden` tinyint(1) DEFAULT NULL,
  `tags` varchar(255) NOT NULL,
  `options_json` json NOT NULL,
  `exercise_type` int DEFAULT NULL,
  `language_id` int DEFAULT NULL,
  `domain_id` varchar(255) NOT NULL,
  `backend_id` varchar(100) NOT NULL,
  `strategy_id` varchar(100) NOT NULL,
  `stages_json` json NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `is_public` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_EXERCISE_ON_DOMAIN` (`domain_id`),
  CONSTRAINT `FK_EXERCISE_ON_DOMAIN` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `exercise_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_status` int DEFAULT NULL,
  `exercise_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `lti_lineitem_url` varchar(512) DEFAULT NULL,
  `lti_context_id` varchar(255) DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_EXERCISEATTEMPT_ON_EXERCISE` (`exercise_id`),
  KEY `FK_EXERCISEATTEMPT_ON_USER` (`user_id`),
  KEY `idx_exercise_attempt_course` (`course_id`),
  CONSTRAINT `fk_exercise_attempt_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `FK_EXERCISEATTEMPT_ON_EXERCISE` FOREIGN KEY (`exercise_id`) REFERENCES `exercise` (`id`),
  CONSTRAINT `FK_EXERCISEATTEMPT_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `exercise_course_link` (
  `exercise_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `linked_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`exercise_id`,`course_id`),
  KEY `fk_exercise_course_link_course` (`course_id`),
  CONSTRAINT `fk_exercise_course_link_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `fk_exercise_course_link_exercise` FOREIGN KEY (`exercise_id`) REFERENCES `exercise` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `explanation_template_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `field_name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `violation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_EXPLANATIONTEMPLATEINFO_ON_VIOLATION` (`violation_id`),
  CONSTRAINT `FK_EXPLANATIONTEMPLATEINFO_ON_VIOLATION` FOREIGN KEY (`violation_id`) REFERENCES `violation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `external_account` (
  `user_id` bigint NOT NULL,
  `education_resource_id` bigint NOT NULL,
  `external_id` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`,`education_resource_id`),
  KEY `fk_external_account_education_resource` (`education_resource_id`),
  CONSTRAINT `fk_external_account_education_resource` FOREIGN KEY (`education_resource_id`) REFERENCES `education_resource` (`id`),
  CONSTRAINT `fk_external_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `grade` float DEFAULT NULL,
  `interactions_left` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `interaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` int DEFAULT NULL,
  `last_supplementary_question` varchar(255) DEFAULT NULL,
  `interaction_type` varchar(255) DEFAULT NULL,
  `feedback_id` bigint DEFAULT NULL,
  `question_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_interaction_feedback_id` (`feedback_id`),
  KEY `FK_INTERACTION_ON_QUESTION` (`question_id`),
  CONSTRAINT `FK_INTERACTION_ON_FEEDBACK` FOREIGN KEY (`feedback_id`) REFERENCES `feedback` (`id`),
  CONSTRAINT `FK_INTERACTION_ON_QUESTION` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` varchar(36) DEFAULT NULL,
  `session_id` varchar(36) DEFAULT NULL,
  `date` datetime(6) DEFAULT NULL,
  `level` varchar(10) DEFAULT NULL,
  `message` text,
  `payload` text,
  `user_id` varchar(36) DEFAULT NULL,
  `app` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app`),
  KEY `idx_date` (`date` DESC),
  KEY `idx_level` (`level`),
  KEY `idx_message` (`message`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `permission_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kind` varchar(32) NOT NULL,
  `scope_item_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_permission_scope_kind_item` (`kind`,(coalesce(`scope_item_id`,0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_type` int DEFAULT NULL,
  `question_status` int DEFAULT NULL,
  `question_text` text,
  `question_name` varchar(255) DEFAULT NULL,
  `question_domain_type` varchar(255) DEFAULT NULL,
  `options_json` json DEFAULT NULL,
  `exercise_attempt_id` bigint DEFAULT NULL,
  `domain_name` varchar(255) NOT NULL,
  `solution_facts` json NOT NULL,
  `statement_facts` json NOT NULL,
  `metadata_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `question_request_id` binary(16) DEFAULT NULL,
  `question_request_id_text` varchar(36) GENERATED ALWAYS AS (bin_to_uuid(`question_request_id`,1)) VIRTUAL,
  `tags` json NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_QUESTION_ON_DOMAIN_NAME` (`domain_name`),
  KEY `FK_QUESTION_ON_METADATA` (`metadata_id`),
  KEY `FK_QUESTION_ON_QUESTION_REQUEST` (`question_request_id`),
  KEY `FK_QUESTION_ON_EXERCISEATTEMPT` (`exercise_attempt_id`),
  CONSTRAINT `FK_QUESTION_ON_DOMAIN_NAME` FOREIGN KEY (`domain_name`) REFERENCES `domain` (`name`),
  CONSTRAINT `FK_QUESTION_ON_EXERCISEATTEMPT` FOREIGN KEY (`exercise_attempt_id`) REFERENCES `exercise_attempt` (`id`) ON DELETE SET NULL,
  CONSTRAINT `FK_QUESTION_ON_METADATA` FOREIGN KEY (`metadata_id`) REFERENCES `questions_meta` (`id`) ON DELETE SET NULL,
  CONSTRAINT `FK_QUESTION_ON_QUESTION_REQUEST` FOREIGN KEY (`question_request_id`) REFERENCES `question_request_log` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `question_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `question_generation_requests` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `status` int NOT NULL,
  `updated_at` datetime NOT NULL,
  `question_request` json NOT NULL,
  `questions_to_generate` int NOT NULL,
  `processing_attempts` int NOT NULL,
  `domain_shortname` varchar(255) GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.domainShortname'))) VIRTUAL NOT NULL,
  `denied_concepts_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.deniedConceptsBitmask'))) VIRTUAL NOT NULL,
  `target_concepts_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.targetConceptsBitmask'))) VIRTUAL NOT NULL,
  `target_laws_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.targetLawsBitmask'))) VIRTUAL NOT NULL,
  `denied_laws_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.deniedLawsBitmask'))) VIRTUAL NOT NULL,
  `target_tags_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.targetTagsBitmask'))) VIRTUAL NOT NULL,
  `complexity` float GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.complexity'))) VIRTUAL NOT NULL,
  `steps_min` int GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.stepsMin'))) VIRTUAL NOT NULL,
  `steps_max` int GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.stepsMax'))) VIRTUAL NOT NULL,
  `exercise_attempt_id` bigint DEFAULT NULL,
  `denied_skills_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.deniedSkillsBitmask'))) VIRTUAL NOT NULL,
  `target_skills_bitmask` bigint GENERATED ALWAYS AS (json_unquote(json_extract(`question_request`,_utf8mb4'$.targetSkillsBitmask'))) VIRTUAL NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_QUESTION_GENERATION_REQUESTS_ON_EXERCISE_ATTEMPT` (`exercise_attempt_id`),
  KEY `question_generation_requests_search_idx` (`status`,`domain_shortname`,`created_at`,`questions_to_generate`),
  CONSTRAINT `FK_QUESTION_GENERATION_REQUESTS_ON_EXERCISE_ATTEMPT` FOREIGN KEY (`exercise_attempt_id`) REFERENCES `exercise_attempt` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `question_request_log` (
  `id` binary(16) NOT NULL,
  `id_text` varchar(36) GENERATED ALWAYS AS (bin_to_uuid(`id`,1)) VIRTUAL,
  `exercise_attempt_id` bigint DEFAULT NULL,
  `target_concept_names` json DEFAULT NULL,
  `denied_concept_names` json DEFAULT NULL,
  `allowed_concept_names` json DEFAULT NULL,
  `target_law_names` json DEFAULT NULL,
  `target_skill_names` json DEFAULT NULL,
  `denied_law_names` json DEFAULT NULL,
  `denied_skill_names` json DEFAULT NULL,
  `allowed_law_names` json DEFAULT NULL,
  `allowed_skill_names` json DEFAULT NULL,
  `denied_question_names` json DEFAULT NULL,
  `denied_question_meta_ids` json DEFAULT NULL,
  `denied_question_template_ids` json DEFAULT NULL,
  `solving_duration` int DEFAULT NULL,
  `complexity` double DEFAULT NULL,
  `complexity_search_direction` int DEFAULT NULL,
  `laws_search_direction` int DEFAULT NULL,
  `chance_to_pick_autogenerated_question` double DEFAULT '1',
  `domain_shortname` varchar(255) DEFAULT NULL,
  `concepts_denied_bitmask` bigint NOT NULL,
  `concepts_targeted_bitmask` bigint NOT NULL,
  `laws_denied_bitmask` bigint NOT NULL,
  `skills_denied_bitmask` bigint NOT NULL,
  `laws_targeted_bitmask` bigint NOT NULL,
  `skills_targeted_bitmask` bigint NOT NULL,
  `steps_max` int NOT NULL,
  `steps_min` int NOT NULL,
  `target_tags` json DEFAULT NULL,
  `target_tags_bitmask` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `questions_data` (
  `data` json NOT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `questions_meta` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `domain_shortname` varchar(45) NOT NULL DEFAULT 'ctrl_flow',
  `q_data_graph` text,
  `tag_bits` bigint DEFAULT NULL,
  `concept_bits` bigint DEFAULT NULL,
  `law_bits` bigint DEFAULT NULL,
  `skill_bits` bigint NOT NULL,
  `violation_bits` bigint DEFAULT NULL,
  `trace_concept_bits` bigint DEFAULT NULL,
  `solution_structural_complexity` double DEFAULT NULL,
  `integral_complexity` double DEFAULT NULL,
  `solution_steps` int DEFAULT NULL,
  `distinct_errors_count` int DEFAULT NULL,
  `_version` int DEFAULT NULL,
  `origin` varchar(1023) DEFAULT NULL,
  `origin_license` varchar(128) DEFAULT NULL,
  `structure_hash` varchar(1023) DEFAULT NULL,
  `template_id` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `question_data_id` int NOT NULL,
  `generation_request_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_domain_name` (`domain_shortname`),
  KEY `questions_meta_search_idx` (`domain_shortname`,`solution_steps`,`integral_complexity`,`template_id`(50),`name`(50)),
  KEY `FK_QUESTIONS_META_ON_GENERATION_REQUEST` (`generation_request_id`),
  KEY `FK_QUESTIONS_META_ON_QUESTION_DATA` (`question_data_id`),
  KEY `idx_questions_meta_domainshortname_name` (`domain_shortname`,`name`),
  KEY `idx_questions_meta_domainshortname_templateid` (`domain_shortname`,`template_id`),
  CONSTRAINT `FK_QUESTIONS_META_ON_GENERATION_REQUEST` FOREIGN KEY (`generation_request_id`) REFERENCES `question_generation_requests` (`id`) ON DELETE SET NULL,
  CONSTRAINT `FK_QUESTIONS_META_ON_QUESTION_DATA` FOREIGN KEY (`question_data_id`) REFERENCES `questions_data` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `questions_meta_search_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_request_id` binary(16) DEFAULT NULL,
  `question_request_id_text` varchar(36) GENERATED ALWAYS AS (bin_to_uuid(`question_request_id`,1)) VIRTUAL,
  `created_at` datetime NOT NULL,
  `quality` tinyint NOT NULL,
  `qlimit` int NOT NULL,
  `found` int NOT NULL,
  `search_request` json NOT NULL,
  `search_iterations` json NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qmsr_created_at` (`created_at`),
  KEY `idx_qmsr_question_request_id` (`question_request_id`),
  KEY `idx_qmsr_quality` (`quality`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `response` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `left_spec_value` int DEFAULT NULL,
  `left_object_id` bigint DEFAULT NULL,
  `right_object_id` bigint DEFAULT NULL,
  `interaction_id` bigint DEFAULT NULL,
  `created_by_interaction_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_RESPONSE_ON_INTERACTION` (`interaction_id`),
  KEY `FK_RESPONSE_ON_LEFTOBJECT` (`left_object_id`),
  KEY `FK_RESPONSE_ON_RIGHTOBJECT` (`right_object_id`),
  KEY `FK_RESPONSE_ON_CREATED_BY_INTERACTION` (`created_by_interaction_id`),
  CONSTRAINT `FK_RESPONSE_ON_CREATED_BY_INTERACTION` FOREIGN KEY (`created_by_interaction_id`) REFERENCES `interaction` (`id`),
  CONSTRAINT `FK_RESPONSE_ON_INTERACTION` FOREIGN KEY (`interaction_id`) REFERENCES `interaction` (`id`),
  CONSTRAINT `FK_RESPONSE_ON_LEFTOBJECT` FOREIGN KEY (`left_object_id`) REFERENCES `answer_object` (`id`),
  CONSTRAINT `FK_RESPONSE_ON_RIGHTOBJECT` FOREIGN KEY (`right_object_id`) REFERENCES `answer_object` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `fk_role_permission_permission` (`permission_id`),
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`),
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_user_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `permission_scope_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_rua_user_role_scope` (`user_id`,`role_id`,`permission_scope_id`),
  KEY `fk_rua_role` (`role_id`),
  KEY `fk_rua_scope` (`permission_scope_id`),
  CONSTRAINT `fk_rua_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_rua_scope` FOREIGN KEY (`permission_scope_id`) REFERENCES `permission_scope` (`id`),
  CONSTRAINT `fk_rua_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `supplementary_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `main_question_interaction_id` bigint NOT NULL,
  `situation_info` json NOT NULL,
  `next_state_id` int DEFAULT NULL,
  `supplementary_question_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_supplementary_step_supplementary_question_id` (`supplementary_question_id`),
  KEY `fk_supplementary_step_to_interaction` (`main_question_interaction_id`),
  CONSTRAINT `fk_supplementary_step_to_interaction` FOREIGN KEY (`main_question_interaction_id`) REFERENCES `interaction` (`id`),
  CONSTRAINT `fk_supplementary_step_to_supplementary_question` FOREIGN KEY (`supplementary_question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `survey_answers` (
  `survey_question_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `result` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`survey_question_id`,`question_id`,`user_id`),
  KEY `FK_SURVEYANSWERS_ON_QUESTION` (`question_id`),
  KEY `FK_SURVEYANSWERS_ON_USER` (`user_id`),
  CONSTRAINT `FK_SURVEYANSWERS_ON_QUESTION` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  CONSTRAINT `FK_SURVEYANSWERS_ON_SURVEY_QUESTION` FOREIGN KEY (`survey_question_id`) REFERENCES `survey_questions` (`id`),
  CONSTRAINT `FK_SURVEYANSWERS_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `survey_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(100) NOT NULL,
  `text` text NOT NULL,
  `options_json` json NOT NULL,
  `survey_id` varchar(255) DEFAULT NULL,
  `policy` json NOT NULL,
  `required` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_SURVEYQUESTIONS_ON_SURVEY` (`survey_id`),
  CONSTRAINT `FK_SURVEYQUESTIONS_ON_SURVEY` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `surveys` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `options_json` json NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `birthdate` datetime DEFAULT NULL,
  `login` varchar(255) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `preferred_language` int DEFAULT NULL,
  `external_user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `external_id_hidx` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `violation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interaction_id` bigint NOT NULL,
  `law_name` varchar(255) NOT NULL,
  `detailed_law_name` varchar(255) DEFAULT NULL,
  `violation_facts` json NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_VIOLATION_ON_INTERACTION` (`interaction_id`),
  CONSTRAINT `FK_VIOLATION_ON_INTERACTION` FOREIGN KEY (`interaction_id`) REFERENCES `interaction` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
