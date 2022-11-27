INSERT INTO `domain` (`version`, `name`, `short_name`, `options_json`) VALUES (1, 'ProgrammingLanguageExpressionDomain', 'expression',
	'{"StorageSPARQLEndpointUrl": null,
                "StorageDummyDirsForNewFile": 2, "StorageDownloadFilesBaseUrl": "file:///C:/data/compp/expression/",
                "StorageUploadFilesBaseUrl": "file:///C:/data/compp/expression/",
                "QuestionsGraphPath": "C:/data/compp/expression.ttl"}' FORMAT JSON);
INSERT INTO `domain` (`version`, `name`, `short_name`, `options_json`) VALUES (1, 'ControlFlowStatementsDomain', 'control_flow',
	'{"StorageSPARQLEndpointUrl": null,
                "StorageDummyDirsForNewFile": 2, "StorageDownloadFilesBaseUrl": "file:///C:/data/compp/control_flow/",
                "StorageUploadFilesBaseUrl": "file:///C:/data/compp/control_flow/",
                "QuestionsGraphPath": "C:/data/compp/control_flow.ttl"}' FORMAT JSON);

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (5, 1, 1, 0, 1, '10', 10, 'test', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (6, 1, 1, 0, 1, '10', 10, 'test1', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [{"kind": "TARGETED", "name": "precedence"}, {"kind": "TARGETED", "name": "associativity"}, {"kind": "FORBIDDEN", "name": "operator_evaluating_left_operand_first"}, {"kind": "TARGETED", "name": "SystemIntegrationTest"}], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (7, 1, 1, 0, 1, '10', 10, 'test2', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (8, 1, 1, 0, 1, '10', 10, 'test3', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,Python', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (9, 1, 1, 0, 1, '10', 10, 'test_type', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (10, 1, 1, 0, 1, '10', 10, 'test_all', 8, 0, 'GradeConfidenceBaseStrategy_Manual50Autogen50', 'Jena', 'ControlFlowStatementsDomain', 'sequence,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":false,"surveyOptions":{"enabled":true,"surveyId":"IsCreatedByHuman"}}', '[{"laws": [{"kind": "TARGETED", "name": "NoFirstCondition"}, {"kind": "TARGETED", "name": "SequenceFinishedTooEarly"}, {"kind": "TARGETED", "name": "TooEarlyInSequence"}, {"kind": "TARGETED", "name": "LoopStartIsNotCondition"}, {"kind": "TARGETED", "name": "NoLoopEndAfterFailedCondition"}, {"kind": "TARGETED", "name": "LastFalseNoEnd"}, {"kind": "TARGETED", "name": "DuplicateOfAct"}, {"kind": "TARGETED", "name": "NoBranchWhenConditionIsTrue"}, {"kind": "TARGETED", "name": "NoConditionAfterIteration"}, {"kind": "TARGETED", "name": "BranchOfFalseCondition"}, {"kind": "TARGETED", "name": "NoAlternativeEndAfterBranch"}, {"kind": "TARGETED", "name": "ElseBranchAfterTrueCondition"}, {"kind": "TARGETED", "name": "NoIterationAfterSuccessfulCondition"}, {"kind": "TARGETED", "name": "LastConditionIsFalseButNoElse"}, {"kind": "TARGETED", "name": "LoopStartIsNotIteration"}], "concepts": [{"kind": "TARGETED", "name": "trace"}], "numberOfQuestions": 10}]');


INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (11, 1, 1, 0, 1, '10', 10, 'test_return', 8, 0, 'GradeConfidenceBaseStrategy_WithConcepts', 'Jena', 'ControlFlowStatementsDomain', 'C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":true,"surveyOptions":{"enabled":false,"surveyId":"IsCreatedByHuman"}, "forceNewAttemptCreationEnabled":false}', '[{"laws": [], "concepts": [{"kind": "TARGETED", "name": "return"}], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (12, 1, 1, 0, 1, '10', 10, 'test_while', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'while_loop,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (13, 1, 1, 0, 1, '10', 10, 'test_do_while', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'do_while_loop,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (14, 1, 1, 0, 1, '10', 10, 'test_operands1', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++,operand_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (15, 1, 1, 0, 1, '10', 10, 'test_operands2', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++,operand_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (16, 1, 1, 0, 1, '10', 10, 'test_precedence', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,C++,precedence_type', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (17, 1, 1, 0, 1, '10', 10, 'test_explanation', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (18, 1, 1, 0, 1, '10', 10, 'test_all_debug', 8, 0, 'GradeConfidenceBaseStrategy', 'Jena', 'ControlFlowStatementsDomain', 'sequence,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (19, 1, 1, 0, 1, '10', 10, 'test_c#', 8, 1, 'GradeConfidenceBaseStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C#', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (20, 0.25, 1, 0, 1, '10', 10, 'test_static:ctfl', 15, 0, 'StaticStrategy', 'Jena', 'ControlFlowStatementsDomain', 'sequence,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":false,"correctAnswerGenerationEnabled":true, "forceNewAttemptCreationEnabled":false}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `exercise` (`id`, `complexity`, `exercise_type`, `hidden`, `language_id`, `max_retries`, `number_of_questions`, `name`, `time_limit`, `use_guiding_questions`, `strategy_id`, `backend_id`, `domain_id`, `tags`, `options_json`, `stages_json`)
VALUES (21, 0.75, 1, 0, 1, '10', 10, 'test_static:expr', 15, 0, 'StaticStrategy', 'Jena', 'ProgrammingLanguageExpressionDomain', 'basics,operators,order,evaluation,C++', '{"newQuestionGenerationEnabled":true,"supplementaryQuestionsEnabled":true,"correctAnswerGenerationEnabled":true, "forceNewAttemptCreationEnabled":false}', '[{"laws": [], "concepts": [], "numberOfQuestions": 10}]');

INSERT INTO `user` (`id`, `email`, `first_name`, `last_name`, `login`, `password`, `preferred_language`)
VALUES (1, 'test', 'test', 'test', 'test', 'test', 0);

INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (4,1,5,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (5,1,8,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (7,1,6,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (9,1,7,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (11,1,9,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (12,1,5,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (13,1,8,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (14,1,14,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (15,1,15,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (16,1,16,1);
INSERT INTO `exercise_attempt` (`id`, `attempt_status`, `exercise_id`, `user_id` ) VALUES (17,1,17,1);
