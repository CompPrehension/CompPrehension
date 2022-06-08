INSERT INTO `domain` (`version`, `name`, `short_name`, `options_json`) VALUES (1, 'ProgrammingLanguageExpressionDomain', 'expression',
	'{"StorageSPARQLEndpointUrl": "http://vds84.server-1.biz:6515/expression/",
	"StorageUploadFilesBaseUrl": "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/compp/expression/",
	"StorageDownloadFilesBaseUrl": "http://vds84.server-1.biz/misc/ftp/compp/expression/",
	"StorageDummyDirsForNewFile": 2}');
INSERT INTO `domain` (`version`, `name`, `short_name`, `options_json`) VALUES (1, 'ControlFlowStatementsDomain', 'control_flow',
	'{"StorageSPARQLEndpointUrl": "http://vds84.server-1.biz:6515/control_flow/",
	"StorageUploadFilesBaseUrl": "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/compp/control_flow/",
	"StorageDownloadFilesBaseUrl": "http://vds84.server-1.biz/misc/ftp/compp/control_flow/",
	"StorageDummyDirsForNewFile": 2}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (5, 1, 1, 0, 1, '10', 'test', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (6, 1, 1, 0, 1, '10', 'test1', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (7, 1, 1, 0, 1, '10', 'test2', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (8, 1, 1, 0, 1, '10', 'test3', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,Python', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (9, 1, 1, 0, 1, '10', 'test_type', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (10, 1, 1, 0, 1, '10', 'test_all', 8, 0, 'GradeConfidenceBaseStrategy_Manual50Autogen50', 'Jena', 'ControlFlowStatementsDomain', 'sequence,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":false,"surveyOptions":{"enabled":true,"surveyId":"IsCreatedByHuman"}}');
--  role_in_exercise:
--     TARGETED = 0
--     FORBIDDEN = 1
--     PERMITTED = 2
INSERT INTO `exercise_laws` (`exercise_id`, `law_name`, `role_in_exercise` ) VALUES
	(10, 'NoFirstCondition', 0),
	(10, 'SequenceFinishedTooEarly', 0),
	(10, 'TooEarlyInSequence', 0),
	(10, 'LoopStartIsNotCondition', 0),
	(10, 'NoLoopEndAfterFailedCondition', 0),
	(10, 'LastFalseNoEnd', 0),
	(10, 'DuplicateOfAct', 0),
	(10, 'NoBranchWhenConditionIsTrue', 0),
	(10, 'NoConditionAfterIteration', 0),
	(10, 'BranchOfFalseCondition', 0),
	(10, 'NoAlternativeEndAfterBranch', 0),
	(10, 'ElseBranchAfterTrueCondition', 0),
	(10, 'NoIterationAfterSuccessfulCondition', 0),
	(10, 'LastConditionIsFalseButNoElse', 0),
	(10, 'LoopStartIsNotIteration', 0);


INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (11, 1, 1, 0, 1, '10', 'test_if', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'alternative,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (12, 1, 1, 0, 1, '10', 'test_while', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'while_loop,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (13, 1, 1, 0, 1, '10', 'test_do_while', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'do_while_loop,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (14, 1, 1, 0, 1, '10', 'test_operands1', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++,operand_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (15, 1, 1, 0, 1, '10', 'test_operands2', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++,operand_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (16, 1, 1, 0, 1, '10', 'test_precedence', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,C++,precedence_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (17, 1, 1, 0, 1, '10', 'test_explanation', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (18, 1, 1, 0, 1, '10', 'test_all_debug', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'sequence,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":true}');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`)
VALUES (19, 1, 1, 0, 1, '10', 'test_c#', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C#', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}');

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
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('operator_evaluating_left_operand_first', 1, 6);
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

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (14,1,14,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 14);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('operands_type', 0, 14);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('binary', 0, 14);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (15,1,15,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 15);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('operands_type', 0, 15);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('ternary', 0, 15);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (16,1,16,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 16);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence_type', 0, 16);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (17,1,17,1);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('precedence', 0, 17);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('associativity', 0, 17);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('operator_evaluating_left_operand_first', 0, 17);
INSERT INTO `exercise_concepts` (`concept_name`, `role_in_exercise`, `exercise_id`) VALUES ('SystemIntegrationTest', 0, 17);
