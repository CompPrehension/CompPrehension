/* https://mdex-nn.ru/page/izmenit-mysql-codepage-1251-utf8.html */

-- query to generate ALTER commands for each table (see below)
-- ===================

SELECT CONCAT(  'ALTER TABLE `', t.`TABLE_SCHEMA` ,  '`.`', t.`TABLE_NAME` ,  '` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;' ) AS sqlcode
FROM  `information_schema`.`TABLES` t
WHERE 1
AND t.`TABLE_SCHEMA` =  'test_comppr2'
ORDER BY 1
LIMIT 0 , 90


-- sqlcode (change `test_comppr2` appropriately)
-- ====================

ALTER DATABASE test_comppr2 charset=utf8;


ALTER TABLE `test_comppr2`.`additional_field` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`answer_object` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`backend_facts_sequence` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`backend_facts` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`backend_file` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`backend` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`correct_law` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`course` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`domain` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise_attempt` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise_concepts` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise_displaying_feedback_type` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise_laws` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise_question_type` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`exercise` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`explanation_template_info` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`feedback` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`hibernate_sequence` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`interaction` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`question_attempt` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`question` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`response` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`students_group` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_action_exercise` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_action` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_course_role` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_exercise` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_group` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user_role` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`user` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `test_comppr2`.`violation` CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
