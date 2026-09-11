import { __toESM } from "./rolldown-runtime-ClB4ozQ6.js";
import { Alert_default, Badge, BrowserRouter, Bug, Button, Droppable, Form_default, Link, ListGroup_default, Modal_default, Navbar_default, Navigate, Pagination_default, Popover, PopoverContent, PopoverTrigger, ResizeMirror, Route, Routes, Spinner, StateManagedSelect$1, Table, Type, X, absurd, action, array, autorun, boolean, chain, components, configure, esm_default, failure, fromArray, import_lib, initReactI18next, instance, intersection, isLeft, isNonEmpty, isNone, isRight, keyof, left, literal, makeAutoObservable, map, nullType, number, observable, observer, partial, pe, pipe, recursion, require_client, require_jsx_runtime, require_react, right, string, success, toJS, tuple, type, undefinedType, union, untracked, useNavigate, useSearchParams, useTranslation } from "./vendor-Bg5-e7iL.js";
//#region \0vite/modulepreload-polyfill.js
(function polyfill() {
	const relList = document.createElement("link").relList;
	if (relList && relList.supports && relList.supports("modulepreload")) return;
	for (const link of document.querySelectorAll("link[rel=\"modulepreload\"]")) processPreload(link);
	new MutationObserver((mutations) => {
		for (const mutation of mutations) {
			if (mutation.type !== "childList") continue;
			for (const node of mutation.addedNodes) if (node.tagName === "LINK" && node.rel === "modulepreload") processPreload(node);
		}
	}).observe(document, {
		childList: true,
		subtree: true
	});
	function getFetchOpts(link) {
		const fetchOpts = {};
		if (link.integrity) fetchOpts.integrity = link.integrity;
		if (link.referrerPolicy) fetchOpts.referrerPolicy = link.referrerPolicy;
		if (link.crossOrigin === "use-credentials") fetchOpts.credentials = "include";
		else if (link.crossOrigin === "anonymous") fetchOpts.credentials = "omit";
		else fetchOpts.credentials = "same-origin";
		return fetchOpts;
	}
	function processPreload(link) {
		if (link.ep) return;
		link.ep = true;
		const fetchOpts = getFetchOpts(link);
		fetch(link.href, fetchOpts);
	}
})();
//#endregion
//#region src/main/js/mobx-config.ts
configure({ enforceActions: "never" });
//#endregion
//#region src/main/js/i18n.ts
var import_react = /* @__PURE__ */ __toESM(require_react(), 1);
var import_client = require_client();
instance.use(initReactI18next).init({
	resources: {
		EN: { translation: {
			question_header: "Question #{{questionNumber}}",
			language_header: "Language",
			signedin_as_header: "Signed in as",
			logout_header: "Logout",
			nextCorrectAnswerBtn: "I'm confused, tell me the next correct step",
			generateNextQuestion_nextQuestion: "Next question",
			generateNextQuestion_warning: "Warning",
			generateNextQuestion_continueAttempt: "Continue attempt",
			generateNextQuestion_modalMessage1: "It is recommended to move on to the next question only after solving all the previous ones.",
			generateNextQuestion_modalMessage2: "Are you sure you want to move on to the next question?",
			grade_feeback: "Grade",
			correctsteps_feeback: "Correct steps",
			stepswitherrors_feeback: "Steps with errors",
			stepsleft_feeback: "Steps left",
			issolved_feeback: "Everything operator is already evaluated. Task is solved",
			foundExisitingAttempt_title: "Found existing attempt",
			foundExisitingAttempt_descr: "Would you like to continue the existing attempt or start a new one",
			foundExisitingAttempt_continueattempt: "Continue",
			foundExisitingAttempt_newattempt: "New",
			exercise_completed: "Exercise completed",
			exercise_supquestion_gotit: "Got it",
			exercise_supquestion_details: "More details",
			exercise_supquestion_send_answer: "Send answer",
			exercise_supquestion_next_question: "Next question",
			exercisesettings_title: "Exercise settings",
			exercisesettings_name: "Name",
			exercisesettings_domain: "Domain",
			exercisesettings_strategy: "Strategy",
			exercisesettings_qcomplexity: "Question difficulty",
			exercisesettings_answlen: "Answer length",
			exercisesettings_qopt: "Options",
			exercisesettings_qopt_forceAttCreation: "Always create a new attempt",
			exercisesettings_qopt_genCorAnsw: "Allow 'generate correct answer' button",
			exercisesettings_qopt_forceShowGenNextQ: "Always show 'generate new question' button",
			exercisesettings_qopt_supQ: "Allow supplementary questions",
			exercisesettings_qopt_debugBtn: "Enable debug information button",
			exercisesettings_max_concurrent_students: "Maximum expected number of students performing the exercise simultaneously",
			exercisesettings_survey: "Survey",
			exercisesettings_tags: "Tags",
			exercisesettings_commonConcepts: "Common concepts",
			exercisesettings_commonLaws: "Common laws",
			exercisesettings_commonSkills: "Common skills",
			exercisesettings_stages: "Stage",
			exercisesettings_stageN: "Stage #{{stageNumber}}",
			exercisesettings_stageN_qnumber: "Number of questions",
			exercisesettings_stageN_concepts: "Concepts",
			exercisesettings_stageN_laws: "Laws",
			exercisesettings_stageN_skills: "Skills",
			exercisesettings_stageN_matchedQuestionExamples: "Question examples",
			exercisesettings_addStage: "Add stage",
			exercisesettings_removeStage: "Remove stage",
			exercisesettings_save: "Save",
			exercisesettings_saveNopen: "Save & Open",
			exercisesettings_open: "Open",
			exercisesettings_genDebugAtt: "Generate debug attempt",
			exercisesettings_optDenied: "Denied",
			exercisesettings_optAllowed: "Allowed",
			exercisesettings_optTarget: "Target",
			exercisesettings_questionsInBank: "Questions in bank",
			exercisesettings_noQuestionsFound: "No suitable questions found",
			survey_sendresults: "Send survey results",
			tour_skip: "Skip introduction",
			tour_complete: "Complete",
			tour_next: "Next",
			tour_exprs_intro: "Welcome to the CompPrehension exercise. Since this is your first time, we recommend taking the tutorial.",
			tour_exprs_intro_title: "First introduction to the trainer",
			tour_exprs_expr: "In this task, you need to select the operators in the expression in the order in which they will be evaluated during program execution.",
			tour_exprs_expr_title: "This is your task",
			tour_exprs_operator: "All the operators that can be selected in this exercise for successful task solution look like this",
			tour_exprs_operator_title: "Operators",
			tour_exprs_hint: "If you're unsure about the next correct step, click this button to request a hint. The correct step will be selected automatically.",
			tour_exprs_hint_title: "Request a hint",
			tour_exprs_error_hint: "This section shows information about an error you made while solving the task, along with a short hint for resolving it.",
			tour_exprs_error_hint_title: "Hint about the mistake",
			tour_exprs_feedback_grade: "This section shows your score for solving the task, in the range from 0 to 1.",
			tour_exprs_feedback_grade_title: "Score",
			tour_exprs_feedback_steps: "This section shows the steps remaining until the task is completed.",
			tour_exprs_feedback_steps_title: "Remaining steps",
			tour_exprs_feedback_errors: "This section shows how many mistakes you made while solving this task.",
			tour_exprs_feedback_errors_title: "Mistakes in solution",
			tour_exprs_earlyfinish: "If you think there's nothing more to evaluate in the expression, click this button.",
			tour_exprs_earlyfinish_title: "Everything is evaluated",
			tour_exprs_tracevalue: "This hint helps you understand the value an operand had at the moment of evaluation.",
			tour_exprs_tracevalue_title: "Operand value at evaluation",
			tour_exprs_selectedop: "Selected and evaluated operators will be displayed in this style. The number below indicates the evaluation order.",
			tour_exprs_selectedop_title: "Evaluated operators",
			tour_exprs_pages: "You can switch between questions in the exercise using these buttons. You can also use the 'Next question' button to go to the next one.",
			tour_exprs_pages_title: "Switching between questions",
			importModal_title: "Import from global pool",
			importModal_modeLabel: "Mode:",
			importModal_inherit_body: "the course will use the shared pool entry. Any change the author makes in the pool will be immediately reflected here and may break ongoing student attempts if the content changes. Convenient for synchronization, but risky during active use. If unsure — choose Clone.",
			importModal_clone_body: "an independent copy is created. The course then works with its own version; the author's changes in the pool do not affect this copy.",
			importModal_loading: "Loading…",
			importModal_importing: "Importing…",
			importModal_import: "Import",
			importModal_cancel: "Cancel",
			deleteModal_title: "Remove exercise from global pool",
			deleteModal_loading: "Loading…",
			deleteModal_noUsages: "This exercise is not used by anyone. It will be removed from the pool.",
			deleteModal_warning: "Warning:",
			deleteModal_warningBody: "independent copies of this exercise will be created in all courses listed below. The link to the original will be broken, and the original will be removed from the pool.",
			deleteModal_cancel: "Cancel",
			deleteModal_confirm: "Delete",
			courses_page_title: "Courses",
			courses_page_globalPoolBtn: "Global exercise pool",
			courses_page_empty: "No courses available",
			course_page_title: "Course #{{id}}",
			course_page_courseIdRequired: "courseId is required",
			course_page_createExerciseBtn: "Create new exercise in course",
			course_page_importBtn: "Import from global pool",
			course_page_empty: "This course has no exercises yet",
			deeplink_title: "Add exercises to the Moodle course",
			deeplink_hint: "Select exercises — Moodle will create an External Tool activity for each.",
			deeplink_blockHint: "Fill the course with exercises, then in Moodle: “Add an activity or resource” → “CompPrehension” → “Select content” — Moodle will create the activities automatically.",
			deeplink_addBtn: "Add selected to Moodle",
			deeplink_added: "added",
			deeplink_submitting: "Sending…",
			deeplink_error: "Failed to prepare the Moodle response",
			deeplink_selectAtLeastOne: "Select at least one exercise",
			deeplink_empty: "This course has no exercises yet — add some first.",
			globalPool_page_title: "Global exercise pool",
			globalPool_page_createBtn: "Create new exercise in pool",
			globalPool_page_empty: "Pool is empty",
			importModal_inherit_label: "⚠ Inherit:",
			importModal_clone_label: "Clone:",
			importModal_inherit_btn: "⚠ Inherit",
			importModal_clone_btn: "Clone",
			exerciseBadge_global: "Global",
			exerciseBadge_original: "Course-private",
			exerciseBadge_inherited: "Inherited from global",
			exerciseBadge_cloned: "Cloned from global",
			exerciseModeBar_convertToClone: "Convert to clone",
			exerciseModeBar_unlinkFromCourse: "Remove from course",
			exerciseModeBar_copyToPool: "Copy to global pool",
			exerciseModeBar_deleteExercise: "Delete exercise",
			exerciseModeBar_confirmDelete: "Delete exercise? Attempt history will be deleted.",
			error_notification_title: "Request failed",
			error_page_title: "Failed to load",
			error_page_retry: "Retry",
			error_boundary_title: "Something went wrong",
			error_boundary_reload: "Reload the page"
		} },
		RU: { translation: {
			question_header: "Вопрос #{{questionNumber}}",
			language_header: "Язык",
			signedin_as_header: "Пользователь",
			logout_header: "Выйти",
			nextCorrectAnswerBtn: "Я в замешательстве, подскажи следующий шаг",
			generateNextQuestion_nextQuestion: "Следующий вопрос",
			generateNextQuestion_warning: "Предупреждение",
			generateNextQuestion_continueAttempt: "Продолжить попытку",
			generateNextQuestion_modalMessage1: "Рекомендуется переходить к следующему вопросу только после решения всех предыдущих.",
			generateNextQuestion_modalMessage2: "Вы действительно хотите перейти к следующему вопросу?",
			grade_feeback: "Оценка",
			correctsteps_feeback: "Правильных шагов",
			stepswitherrors_feeback: "Шагов с ошибками",
			stepsleft_feeback: "Шагов осталось",
			issolved_feeback: "Все действия программы выполнены. Задача решена",
			foundExisitingAttempt_title: "Найдена неоконченная попытка",
			foundExisitingAttempt_descr: "Вы хотите продолжить существующую попытку или начать новую",
			foundExisitingAttempt_continueattempt: "Продолжить",
			foundExisitingAttempt_newattempt: "Новая",
			exercise_completed: "Упражнение завершено",
			exercise_supquestion_gotit: "Понятно",
			exercise_supquestion_details: "Разобраться подробнее",
			exercise_supquestion_send_answer: "Отправить ответ",
			exercise_supquestion_next_question: "Следующий вопрос",
			exercisesettings_title: "Настройка упражнений",
			exercisesettings_name: "Название",
			exercisesettings_domain: "Домен",
			exercisesettings_strategy: "Стратегия",
			exercisesettings_qcomplexity: "Сложность вопросов",
			exercisesettings_answlen: "Длина ответа",
			exercisesettings_qopt: "Опции",
			exercisesettings_qopt_forceAttCreation: "Всегда создавать новую попытку",
			exercisesettings_qopt_genCorAnsw: "Разрешить подсказку следующего шага",
			exercisesettings_qopt_forceShowGenNextQ: "Разрешить неоконченные вопросы",
			exercisesettings_qopt_supQ: "Разрешить вспомогательные вопросы",
			exercisesettings_qopt_debugBtn: "Включить кнопку для получения отладочной информации",
			exercisesettings_max_concurrent_students: "Максимальное ожидаемое количество студентов, выполняющих упражнение одновременно",
			exercisesettings_survey: "Опрос",
			exercisesettings_tags: "Теги",
			exercisesettings_commonConcepts: "Общие концепты",
			exercisesettings_commonLaws: "Общие законы",
			exercisesettings_commonSkills: "Общие умения",
			exercisesettings_stages: "Стадии упражнения",
			exercisesettings_stageN: "Стадия #{{stageNumber}}",
			exercisesettings_stageN_qnumber: "Количество вопросов",
			exercisesettings_stageN_concepts: "Концепты",
			exercisesettings_stageN_laws: "Законы",
			exercisesettings_stageN_skills: "Умения",
			exercisesettings_stageN_matchedQuestionExamples: "Примеры вопросов",
			exercisesettings_addStage: "Добавить стадию",
			exercisesettings_removeStage: "Удалить стадию",
			exercisesettings_save: "Сохранить",
			exercisesettings_saveNopen: "Сохранить & Открыть",
			exercisesettings_open: "Открыть",
			exercisesettings_genDebugAtt: "Создать отладочную попытку",
			exercisesettings_optDenied: "Запрет",
			exercisesettings_optAllowed: "Разреш.",
			exercisesettings_optTarget: "Цель",
			exercisesettings_questionsInBank: "Вопросов в банке задач",
			exercisesettings_noQuestionsFound: "Подходящих вопросов не найдено",
			survey_sendresults: "Отправить результаты опроса",
			tour_skip: "Не показывать",
			tour_complete: "Завершить",
			tour_next: "Далее",
			tour_exprs_intro: "Добро пожаловать в упражнение CompPrehension! Новичкам рекомендуется познакомиться с элементами управления. Начнём?",
			tour_exprs_intro_title: "Знакомство с тренажёром",
			tour_exprs_expr: "В этом задании нужно «прокликать» операторы в выражения в том порядке, в каком они должны быть вычислены при выполнении программы",
			tour_exprs_expr_title: "Задание",
			tour_exprs_operator: "Это один из операторов выражения, и он подчёркнут. Их нужно нажимать в процессе решения задачи.",
			tour_exprs_operator_title: "Оператор",
			tour_exprs_hint: "Не знаете, что должно быть вычислено? Нажмите, чтобы запросить следующий корректный шаг.",
			tour_exprs_hint_title: "Запрос подсказки",
			tour_exprs_error_hint: "На красном фоне даётся описание текущей ошибки и подсказка",
			tour_exprs_error_hint_title: "Ошибки",
			tour_exprs_feedback_grade: "Ваша оценка за решение всех заданий, в диапазоне от 0 до 1",
			tour_exprs_feedback_grade_title: "Оценка за серию вопросов",
			tour_exprs_feedback_steps: "Столько шагов остаётся до окончания решения задачи",
			tour_exprs_feedback_steps_title: "Обратный счётчик шагов",
			tour_exprs_feedback_errors: "Столько раз вы при решении этой задачи вы нажали «не туда»",
			tour_exprs_feedback_errors_title: "Счётчик ошибок",
			tour_exprs_earlyfinish: "В некоторых задачах не все операторы должны быть вычислены. Нажмите эту кнопку, когда вычислено всё, что нужно.",
			tour_exprs_earlyfinish_title: "Раннее завершение",
			tour_exprs_tracevalue: "Некоторые операторы ведут себя по-разному в зависимости от значения своих операндов. Значения таких операндов отображаются здесь. «true» — истина (ДА), «false» — ложь (НЕТ).",
			tour_exprs_tracevalue_title: "Значение операнда",
			tour_exprs_selectedop: "Вычисленные и использованные операторы отображаются с зелёным подчёркиванием и номером снизу",
			tour_exprs_selectedop_title: "Вычисленные операторы",
			tour_exprs_pages: "С помощью этих кнопок можно вернуться к любому вопросу в упражнении. Для перехода вперёд используйте кнопку «Следующий вопрос» внизу.",
			tour_exprs_pages_title: "Переключение между вопросами",
			importModal_title: "Импорт из глобального пула",
			importModal_modeLabel: "Режим:",
			importModal_inherit_body: "курс будет использовать общую запись из пула. Любое изменение автора в пуле сразу отразится здесь и поломает уже идущие attempt'ы студентов, если автор поменяет наполнение. Это удобно для синхронизации, но опасно при активном использовании. Если не уверен — выбери Clone.",
			importModal_clone_body: "создаётся независимая копия. Дальше курс работает со своей версией; изменения автора в пуле никак не влияют на эту копию.",
			importModal_loading: "Загрузка…",
			importModal_importing: "Импорт…",
			importModal_import: "Импортировать",
			importModal_cancel: "Отмена",
			deleteModal_title: "Удалить упражнение из глобального пула",
			deleteModal_loading: "Загрузка…",
			deleteModal_noUsages: "Это упражнение никем не используется. Будет удалено из пула.",
			deleteModal_warning: "Внимание:",
			deleteModal_warningBody: "во всех курсах ниже будут созданы независимые копии этого упражнения. Связь с оригиналом разорвётся, оригинал будет удалён из пула.",
			deleteModal_cancel: "Отмена",
			deleteModal_confirm: "Удалить",
			courses_page_title: "Курсы",
			courses_page_globalPoolBtn: "Глобальный пул упражнений",
			courses_page_empty: "Нет доступных курсов",
			course_page_title: "Курс #{{id}}",
			course_page_courseIdRequired: "Требуется courseId",
			course_page_createExerciseBtn: "Создать новое упражнение в курсе",
			course_page_importBtn: "Импортировать из глобального пула",
			course_page_empty: "В этом курсе пока нет упражнений",
			deeplink_title: "Добавить упражнения в курс Moodle",
			deeplink_hint: "Выберите упражнения — Moodle создаст по активности «Внешний инструмент» на каждое.",
			deeplink_blockHint: "Наполните курс упражнениями, затем в Moodle: «Добавить элемент курса» → «CompPrehension» → «Выбрать содержимое» — Moodle создаст активности автоматически.",
			deeplink_addBtn: "Добавить выбранные в Moodle",
			deeplink_added: "добавлено",
			deeplink_submitting: "Отправка…",
			deeplink_error: "Не удалось подготовить ответ для Moodle",
			deeplink_selectAtLeastOne: "Выберите хотя бы одно упражнение",
			deeplink_empty: "В курсе пока нет упражнений — сначала добавьте их.",
			globalPool_page_title: "Глобальный пул упражнений",
			globalPool_page_createBtn: "Создать новое упражнение в пуле",
			globalPool_page_empty: "Пул пуст",
			importModal_inherit_label: "⚠ Inherit:",
			importModal_clone_label: "Clone:",
			importModal_inherit_btn: "⚠ Inherit",
			importModal_clone_btn: "Clone",
			exerciseBadge_global: "Глобальное",
			exerciseBadge_original: "Только в курсе",
			exerciseBadge_inherited: "Унаследовано из пула",
			exerciseBadge_cloned: "Клонировано из пула",
			exerciseModeBar_convertToClone: "Преобразовать в клон",
			exerciseModeBar_unlinkFromCourse: "Удалить из курса",
			exerciseModeBar_copyToPool: "Скопировать в глобальный пул",
			exerciseModeBar_deleteExercise: "Удалить упражнение",
			exerciseModeBar_confirmDelete: "Удалить упражнение? История попыток будет удалена.",
			error_notification_title: "Запрос не выполнен",
			error_page_title: "Не удалось загрузить",
			error_page_retry: "Повторить",
			error_boundary_title: "Что-то пошло не так",
			error_boundary_reload: "Перезагрузить страницу"
		} },
		PL: { translation: {
			question_header: "Pytanie #{{questionNumber}}",
			language_header: "Język",
			signedin_as_header: "Zalogowany jako",
			logout_header: "Wyloguj się",
			nextCorrectAnswerBtn: "Nie wiem co robić dalej, podpowiedz mi następny poprawny krok",
			generateNextQuestion_nextQuestion: "Następne pytanie",
			generateNextQuestion_warning: "Ostrzeżenie",
			generateNextQuestion_continueAttempt: "Kontynuuj podejście",
			generateNextQuestion_modalMessage1: "Zaleca się przejście do następnego pytania dopiero po rozwiązaniu wszystkich poprzednich.",
			generateNextQuestion_modalMessage2: "Czy na pewno chcesz przejść do następnego pytania?",
			grade_feeback: "Ocena",
			correctsteps_feeback: "Poprawne kroki",
			stepswitherrors_feeback: "Kroki z błędami",
			stepsleft_feeback: "Pozostałe kroki",
			issolved_feeback: "Rozwiązane",
			foundExisitingAttempt_title: "Znaleziono już istniejące podejście",
			foundExisitingAttempt_descr: "Czy chcesz kontynuować dotychczasowe podejście, czy rozpocząć nowe",
			foundExisitingAttempt_continueattempt: "Kontynuuj",
			foundExisitingAttempt_newattempt: "Nowe",
			exercise_completed: "Ćwiczenie zakończone",
			exercise_supquestion_gotit: "Oczywiście!",
			exercise_supquestion_details: "Zobacz szczegóły",
			exercise_supquestion_send_answer: "Wyślij odpowiedź",
			exercise_supquestion_next_question: "Następne pytanie",
			exercisesettings_title: "Exercise settings",
			exercisesettings_name: "Name",
			exercisesettings_domain: "Domain",
			exercisesettings_strategy: "Strategy",
			exercisesettings_qcomplexity: "Question difficulty",
			exercisesettings_answlen: "Answer length",
			exercisesettings_qopt: "Options",
			exercisesettings_qopt_forceAttCreation: "Always create a new attempt",
			exercisesettings_qopt_genCorAnsw: "Allow 'generate correct answer' button",
			exercisesettings_qopt_forceShowGenNextQ: "Always show 'generate new question' button",
			exercisesettings_qopt_supQ: "Allow supplementary questions",
			exercisesettings_max_concurrent_students: "Maximum expected number of students performing the exercise simultaneously",
			exercisesettings_survey: "Survey",
			exercisesettings_tags: "Tags",
			exercisesettings_commonConcepts: "Common concepts",
			exercisesettings_commonLaws: "Common laws",
			exercisesettings_stages: "Stage",
			exercisesettings_stageN: "Stage #{{stageNumber}}",
			exercisesettings_stageN_qnumber: "Number of questions",
			exercisesettings_stageN_concepts: "Concepts",
			exercisesettings_stageN_laws: "Laws",
			exercisesettings_stageN_matchedQuestionExamples: "Question examples",
			exercisesettings_addStage: "Add stage",
			exercisesettings_removeStage: "Remove stage",
			exercisesettings_save: "Save",
			exercisesettings_saveNopen: "Save & Open",
			exercisesettings_open: "Open",
			exercisesettings_genDebugAtt: "Generate debug attempt",
			exercisesettings_optDenied: "Denied",
			exercisesettings_optAllowed: "Allowed",
			exercisesettings_optTarget: "Target",
			exercisesettings_questionsInBank: "Questions in bank",
			exercisesettings_noQuestionsFound: "No suitable questions found",
			survey_sendresults: "Send survey results",
			importModal_title: "Importuj z globalnej puli",
			importModal_modeLabel: "Tryb:",
			importModal_inherit_body: "kurs będzie korzystał ze wspólnego wpisu z puli. Każda zmiana autora w puli natychmiast się tutaj pojawi i może zepsuć trwające próby studentów, jeśli autor zmieni zawartość. Wygodne do synchronizacji, ale ryzykowne przy aktywnym użytkowaniu. Jeśli nie jesteś pewien — wybierz Clone.",
			importModal_clone_body: "tworzona jest niezależna kopia. Kurs następnie pracuje z własną wersją; zmiany autora w puli nie mają wpływu na tę kopię.",
			importModal_loading: "Ładowanie…",
			importModal_importing: "Importowanie…",
			importModal_import: "Importuj",
			importModal_cancel: "Anuluj",
			deleteModal_title: "Usuń ćwiczenie z globalnej puli",
			deleteModal_loading: "Ładowanie…",
			deleteModal_noUsages: "To ćwiczenie nie jest używane przez nikogo. Zostanie usunięte z puli.",
			deleteModal_warning: "Uwaga:",
			deleteModal_warningBody: "we wszystkich poniższych kursach zostaną utworzone niezależne kopie tego ćwiczenia. Połączenie z oryginałem zostanie zerwane, a oryginał zostanie usunięty z puli.",
			deleteModal_cancel: "Anuluj",
			deleteModal_confirm: "Usuń",
			courses_page_title: "Kursy",
			courses_page_globalPoolBtn: "Globalna pula ćwiczeń",
			courses_page_empty: "Brak dostępnych kursów",
			course_page_title: "Kurs #{{id}}",
			course_page_courseIdRequired: "Wymagany jest courseId",
			course_page_createExerciseBtn: "Utwórz nowe ćwiczenie w kursie",
			course_page_importBtn: "Importuj z globalnej puli",
			course_page_empty: "Ten kurs nie ma jeszcze ćwiczeń",
			deeplink_title: "Dodaj ćwiczenia do kursu Moodle",
			deeplink_hint: "Wybierz ćwiczenia — Moodle utworzy dla każdego aktywność „Narzędzie zewnętrzne”.",
			deeplink_blockHint: "Wypełnij kurs ćwiczeniami, a następnie w Moodle: „Dodaj aktywność lub zasób” → „CompPrehension” → „Wybierz zawartość” — Moodle utworzy aktywności automatycznie.",
			deeplink_addBtn: "Dodaj wybrane do Moodle",
			deeplink_added: "dodano",
			deeplink_submitting: "Wysyłanie…",
			deeplink_error: "Nie udało się przygotować odpowiedzi dla Moodle",
			deeplink_selectAtLeastOne: "Wybierz co najmniej jedno ćwiczenie",
			deeplink_empty: "Ten kurs nie ma jeszcze ćwiczeń — najpierw je dodaj.",
			globalPool_page_title: "Globalna pula ćwiczeń",
			globalPool_page_createBtn: "Utwórz nowe ćwiczenie w puli",
			globalPool_page_empty: "Pula jest pusta",
			importModal_inherit_label: "⚠ Inherit:",
			importModal_clone_label: "Clone:",
			importModal_inherit_btn: "⚠ Inherit",
			importModal_clone_btn: "Clone",
			exerciseBadge_global: "Globalne",
			exerciseBadge_original: "Tylko w kursie",
			exerciseBadge_inherited: "Odziedziczone z puli",
			exerciseBadge_cloned: "Sklonowane z puli",
			exerciseModeBar_convertToClone: "Konwertuj na klon",
			exerciseModeBar_unlinkFromCourse: "Usuń z kursu",
			exerciseModeBar_copyToPool: "Skopiuj do globalnej puli",
			exerciseModeBar_deleteExercise: "Usuń ćwiczenie",
			exerciseModeBar_confirmDelete: "Usunąć ćwiczenie? Historia prób zostanie usunięta.",
			error_notification_title: "Żądanie nie powiodło się",
			error_page_title: "Nie udało się załadować",
			error_page_retry: "Ponów",
			error_boundary_title: "Coś poszło nie tak",
			error_boundary_reload: "Odśwież stronę"
		} }
	},
	lng: "EN",
	interpolation: { escapeValue: false }
});
//#endregion
//#region src/main/js/components/common/optional.tsx
var import_jsx_runtime = require_jsx_runtime();
var Optional = observer((props) => {
	const { isVisible, children } = props;
	if (!isVisible) return null;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children });
});
//#endregion
//#region src/main/js/components/common/debug.tsx
var DebugButton = ({ metadataId, attemptId }) => {
	const [clicked, setClicked] = (0, import_react.useState)(false);
	(0, import_react.useEffect)(() => {
		setClicked(false);
	}, [metadataId, attemptId]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "position-absolute",
		style: {
			bottom: "0.5rem",
			right: "0.5rem",
			zIndex: 1050
		},
		children: !clicked ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
			variant: "light",
			className: "border-0",
			style: {
				backgroundColor: "transparent",
				color: "transparent",
				padding: "0.2rem",
				transition: "all 0.3s ease"
			},
			onMouseEnter: (e) => {
				e.currentTarget.style.color = "#dc3545";
				e.currentTarget.style.backgroundColor = "#f8d7da";
			},
			onMouseLeave: (e) => {
				e.currentTarget.style.color = "transparent";
				e.currentTarget.style.backgroundColor = "transparent";
			},
			onClick: () => setClicked(true),
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Bug, { size: 18 })
		}) : /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "alert alert-danger p-2 mb-0",
			role: "alert",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("small", { children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("strong", { children: "Metadata ID:" }),
				" ",
				metadataId
			] }) }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
				isVisible: attemptId !== void 0,
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("small", { children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("strong", { children: "Attempt ID:" }),
					" ",
					attemptId
				] }) })
			})]
		})
	});
};
//#endregion
//#region src/main/js/stores/notifications-store.ts
var AUTO_DISMISS_MS = 12e3;
/** Grace period that lets a page claim the error and render it in its own slot instead. */
var CLAIM_WINDOW_MS = 250;
var keyOf = (error) => `${error.status} ${error.message}`;
var NotificationsStore = class {
	notifications = [];
	nextId = 1;
	pending = /* @__PURE__ */ new Map();
	constructor() {
		makeAutoObservable(this, {
			nextId: false,
			pending: false
		});
	}
	report(error) {
		const key = keyOf(error);
		clearTimeout(this.pending.get(key));
		this.pending.set(key, setTimeout(() => {
			this.pending.delete(key);
			this.show(error);
		}, CLAIM_WINDOW_MS));
	}
	handled(error) {
		const key = keyOf(error);
		clearTimeout(this.pending.get(key));
		this.pending.delete(key);
		this.notifications = this.notifications.filter((n) => keyOf(n.error) !== key);
	}
	dismiss(id) {
		this.notifications = this.notifications.filter((n) => n.id !== id);
	}
	show(error) {
		const key = keyOf(error);
		const same = this.notifications.find((n) => keyOf(n.error) === key);
		if (same) {
			same.count++;
			return;
		}
		const id = this.nextId++;
		this.notifications.push({
			id,
			error,
			count: 1
		});
		setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS);
	}
};
var notifications = new NotificationsStore();
//#endregion
//#region src/main/js/components/common/errors.tsx
function statusLine(error) {
	if (error.status === void 0) return null;
	return error.title ? `${error.status} ${error.title}` : `${error.status}`;
}
var ErrorNotificationAlert = observer(({ notification }) => {
	const { t } = useTranslation();
	const { error, count } = notification;
	const status = statusLine(error);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Alert_default, {
		variant: "danger",
		dismissible: true,
		onClose: () => notifications.dismiss(notification.id),
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Alert_default.Heading, {
				as: "h6",
				className: "d-flex align-items-center",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: status ?? t("error_notification_title") }), count > 1 && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
					className: "badge bg-light text-dark ms-2",
					children: count
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "comp-ph-error-notification-message",
				children: error.message
			}),
			error.path && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("small", {
				className: "text-muted d-block mt-1",
				children: error.path
			})
		]
	});
});
var ErrorNotifications = observer(() => {
	if (notifications.notifications.length === 0) return null;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "comp-ph-error-notifications",
		children: notifications.notifications.map((n) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ErrorNotificationAlert, { notification: n }, n.id))
	});
});
function useHandledError(error) {
	(0, import_react.useEffect)(() => {
		if (error) notifications.handled(error);
	}, [error]);
}
var InlineError = observer(({ error }) => {
	useHandledError(error);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default, {
		variant: "danger",
		children: error.message
	});
});
var LoadFailure = observer(({ error, onRetry }) => {
	const { t } = useTranslation();
	const status = statusLine(error);
	useHandledError(error);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Alert_default, {
		variant: "danger",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default.Heading, {
				as: "h6",
				children: t("error_page_title")
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "comp-ph-error-notification-message",
				children: status ? `${status} — ${error.message}` : error.message
			}),
			onRetry && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
				variant: "outline-danger",
				size: "sm",
				className: "mt-2",
				onClick: onRetry,
				children: t("error_page_retry")
			})
		]
	});
});
//#endregion
//#region src/main/js/components/common/loader.tsx
var Loader = observer((props) => {
	const delay = props.delay ?? 0;
	const [enabled, setEnabled] = (0, import_react.useState)(delay === 0);
	(0, import_react.useEffect)(() => {
		if (delay > 0) setTimeout(() => !enabled && setEnabled(true), delay);
	});
	if (!enabled) return null;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Spinner, {
		style: { ...props.styleOverride },
		animation: "border",
		variant: "primary"
	});
});
var LoadingWrapper = observer((props) => {
	const { children, isLoading } = props;
	if (isLoading) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, { ...props });
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children });
});
union([
	nullType,
	undefinedType,
	literal("")
]);
function TOptionalRequestResult(type, name) {
	return union([
		type,
		nullType,
		undefinedType,
		literal("")
	], name ?? type.name);
}
function delayPromise(timeout) {
	return new Promise((resolve) => setTimeout(() => resolve(), timeout));
}
function isNullOrUndefined(value) {
	return value === null || value === void 0;
}
//#endregion
//#region src/main/js/components/common/modal.tsx
var Modal = (props) => {
	const { title, primaryBtnTitle, primaryBtnVariant, handlePrimaryBtnClicked, secondaryBtnTitle, handleSecondaryBtnClicked, children, closeButton, show, handleClose, type, size } = props;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
		isVisible: show ?? true,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(ModalWrapper, {
			type: type ?? "MODAL",
			show: show ?? true,
			onHide: handleClose ?? void 0,
			size,
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
					isVisible: !isNullOrUndefined(title) && title.length > 0,
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal_default.Header, {
						closeButton: closeButton ?? void 0,
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal_default.Title, { children: title })
					})
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal_default.Body, { children }),
				secondaryBtnTitle || primaryBtnTitle ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Modal_default.Footer, { children: [secondaryBtnTitle && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "secondary",
					onClick: handleSecondaryBtnClicked ?? void 0,
					children: secondaryBtnTitle
				}), primaryBtnTitle && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: primaryBtnVariant ?? "primary",
					onClick: handlePrimaryBtnClicked ?? void 0,
					children: primaryBtnTitle
				})] }) : null
			]
		})
	});
};
var ModalWrapper = (props) => {
	const { type, show, onHide, children, size } = props;
	if (type === "DIALOG") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "modal position-static d-block",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal_default.Dialog, {
			size,
			children
		})
	});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal_default, {
		size,
		show,
		onHide,
		children
	});
};
//#endregion
//#region src/main/js/utils/ajax.ts
var commonParams = {
	method: "GET",
	headers: { "Content-Type": "application/json" }
};
/**
* Do GET request
* @param {string} url Target url
* @param {io.Type<T, T, unknown>} [validator] Optional response validator
* @returns Pair of either RequestError or ResposeBody
*/
async function ajaxGet(url, validator, signal) {
	return await ajax(url, {
		...commonParams,
		signal
	}, validator);
}
/**
* Do POST request
* @param {string} url Target url
* @param {object} body Request body
* @param {io.Type<T, T, unknown>} [validator] Optional response validator
* @returns Pair of either RequestError or ResposeBody
*/
async function ajaxPost(url, body, validator, signal, payloadType) {
	return await ajax(url, {
		...commonParams,
		method: "POST",
		body: JSON.stringify(body),
		signal
	}, validator, payloadType);
}
/**
* Do PUT request
* @param {string} url Target url
* @param {object} body Request body
* @param {io.Type<T, T, unknown>} [validator] Optional response validator
* @returns Pair of either RequestError or ResposeBody
*/
async function ajaxPut(url, body, validator, signal) {
	return await ajax(url, {
		...commonParams,
		method: "PUT",
		body: JSON.stringify(body),
		signal
	}, validator);
}
/**
* Do DELETE request
* @param {string} url Target url
* @param {object} body Request body
* @param {io.Type<T, T, unknown>} [validator] Optional response validator
* @returns Pair of either RequestError or ResposeBody
*/
async function ajaxDelete(url, validator, signal) {
	return await ajax(url, {
		...commonParams,
		method: "DELETE",
		signal
	}, validator);
}
var statusTexts = {
	400: "Bad request",
	401: "Unauthorized",
	403: "Forbidden",
	404: "Not found",
	405: "Method not allowed",
	409: "Conflict",
	413: "Payload too large",
	415: "Unsupported media type",
	422: "Unprocessable entity",
	429: "Too many requests",
	500: "Internal server error",
	502: "Bad gateway",
	503: "Service unavailable",
	504: "Gateway timeout"
};
var asText = (value) => typeof value === "string" && value.trim() !== "" ? value.trim() : void 0;
function parseErrorBody(body) {
	try {
		const parsed = JSON.parse(body);
		return typeof parsed === "object" && parsed !== null ? parsed : void 0;
	} catch {
		return;
	}
}
function plainTextBody(body) {
	const text = asText(body);
	return text !== void 0 && text.length <= 300 && !text.startsWith("<") ? text : void 0;
}
async function toRequestError(response) {
	const body = await response.text().catch(() => "");
	const parsed = parseErrorBody(body);
	const title = asText(parsed?.title) ?? asText(parsed?.error);
	return {
		status: response.status,
		message: asText(parsed?.detail) ?? asText(parsed?.message) ?? title ?? (parsed === void 0 ? plainTextBody(body) : void 0) ?? statusTexts[response.status] ?? `Request failed with status ${response.status}`,
		title,
		path: asText(parsed?.instance) ?? asText(parsed?.path),
		timestamp: asText(parsed?.timestamp),
		trace: asText(parsed?.trace)
	};
}
async function readPayload(response, payloadType) {
	const body = await response.text();
	if (payloadType === "raw") return body;
	return body.trim() === "" ? "" : JSON.parse(body);
}
function fail(error) {
	console.error(error);
	notifications.report(error);
	return left(error);
}
var isAbort = (err) => err instanceof DOMException && err.name === "AbortError";
async function ajax(url, params, validator, payloadType) {
	payloadType ??= "json";
	let response;
	try {
		response = await fetch(url, params);
	} catch (err) {
		if (isAbort(err)) return left({ message: "Request aborted" });
		return fail({ message: `Network error: ${err instanceof Error ? err.message : String(err)}` });
	}
	if (!response.ok) return fail(await toRequestError(response));
	let payload;
	try {
		payload = await readPayload(response, payloadType);
	} catch (err) {
		if (isAbort(err)) return left({ message: "Request aborted" });
		return fail({
			status: response.status,
			message: `Malformed response body: ${err instanceof Error ? err.message : String(err)}`
		});
	}
	const decoded = validator ? validator.decode(payload) : success(payload);
	if (isLeft(decoded)) return fail({
		status: response.status,
		message: `Type inconsistency for properties of ${validator?.name} type: ${getPaths(decoded.left).join(", ")}`
	});
	return right(decoded.right);
}
var getPaths = (errors) => {
	return errors.map((error) => error.context.map(({ key }) => key).join("."));
};
//#endregion
//#region src/main/js/types/course.ts
var TCourseDto = type({
	id: number,
	name: string,
	educationResourceId: number,
	educationResourceUrl: string
});
//#endregion
//#region src/main/js/types/exercise-options.ts
var TExerciseOptions = intersection([type({
	forceNewAttemptCreationEnabled: boolean,
	debugButtonEnabled: boolean,
	newQuestionGenerationEnabled: boolean,
	supplementaryQuestionsEnabled: boolean,
	correctAnswerGenerationEnabled: boolean,
	maxExpectedConcurrentStudents: number
}), partial({ surveyOptions: type({
	enabled: boolean,
	surveyId: string
}) })], "ExerciseOptions");
//#endregion
//#region src/main/js/types/utils.ts
/**
* @since 0.5.0
*/
function nonEmptyArray(codec, name = `NonEmptyArray<${codec.name}>`) {
	const arr = array(codec);
	return new Type(name, (u) => arr.is(u) && isNonEmpty(u), (u, c) => pipe(arr.validate(u, c), chain((as) => {
		const onea = fromArray(as);
		return isNone(onea) ? failure(u, c) : success(onea.value);
	})), map(codec.encode));
}
function getUrlParameterByName(name) {
	return new URLSearchParams(window.location.search).get(name);
}
//#endregion
//#region src/main/js/types/exercise-settings.ts
var TExerciseListItem = type({
	id: number,
	name: string,
	isPublic: boolean
});
var TExerciseListPermissions = type({
	canCreateExercise: boolean,
	canImportInherit: boolean,
	canImportClone: boolean
}, "ExerciseListPermissions");
var noExerciseListPermissions = {
	canCreateExercise: false,
	canImportInherit: false,
	canImportClone: false
};
var TExerciseList = type({
	exercises: array(TExerciseListItem),
	permissions: TExerciseListPermissions
}, "ExerciseList");
var TExerciseCardConcept = type({
	name: string,
	kind: keyof({
		"FORBIDDEN": null,
		"PERMITTED": null,
		"TARGETED": null
	})
});
var TExerciseCardLaw = type({
	name: string,
	kind: keyof({
		"FORBIDDEN": null,
		"PERMITTED": null,
		"TARGETED": null
	})
});
var TExerciseCardSkill = type({
	name: string,
	kind: keyof({
		"FORBIDDEN": null,
		"PERMITTED": null,
		"TARGETED": null
	})
});
var TExerciseStage = type({
	numberOfQuestions: number,
	complexity: number,
	concepts: array(TExerciseCardConcept),
	laws: array(TExerciseCardLaw),
	skills: array(TExerciseCardSkill)
});
var TExerciseCardPermissions = type({
	canEdit: boolean,
	canDelete: boolean,
	canCloneToCourse: boolean,
	canCopyToGlobalPool: boolean,
	canUnlinkFromCourse: boolean
}, "ExerciseCardPermissions");
var TExerciseCard = type({
	id: number,
	name: string,
	domainId: string,
	strategyId: string,
	backendId: string,
	stages: nonEmptyArray(TExerciseStage),
	tags: array(string),
	options: TExerciseOptions,
	isPublic: boolean,
	permissions: TExerciseCardPermissions
});
var TDomainSkill = recursion("DomainSkill", () => type({
	name: string,
	displayName: string,
	childs: array(TDomainSkill)
}));
var TDomainLaw = recursion("DomainLaw", () => type({
	name: string,
	displayName: string,
	bitflags: number,
	childs: array(TDomainLaw)
}));
var DomainConceptFlag = /* @__PURE__ */ function(DomainConceptFlag) {
	DomainConceptFlag[DomainConceptFlag["VisibleToTeacher"] = 1] = "VisibleToTeacher";
	DomainConceptFlag[DomainConceptFlag["TargetEnabled"] = 2] = "TargetEnabled";
	return DomainConceptFlag;
}({});
var TDomainConcept = recursion("DomainConcept", () => type({
	name: string,
	displayName: string,
	bitflags: number,
	childs: array(TDomainConcept)
}));
var TDomain = type({
	id: string,
	displayName: string,
	description: union([string, nullType]),
	laws: array(TDomainLaw),
	skills: array(TDomainSkill),
	concepts: array(TDomainConcept),
	tags: array(string)
});
var TStrategy = type({
	id: string,
	displayName: string,
	description: union([string, nullType]),
	options: type({ multiStagesEnabled: boolean })
});
var TQuestionBankSearchResult = type({
	count: number,
	topRatedCount: number,
	questions: array(type({
		metadataId: number,
		name: string
	}))
});
//#endregion
//#region src/main/js/controllers/course/course-controller.ts
var CourseController = class {
	getMyCourses() {
		return ajaxGet(`/api/course/my`, array(TCourseDto));
	}
	getCourseExercises(courseId) {
		return ajaxGet(`/api/exercise/list?courseId=${courseId}`, TExerciseList);
	}
	getExerciseMemberships(exerciseId) {
		return ajaxGet(`/api/course/memberships?exerciseId=${exerciseId}`, array(TCourseDto));
	}
	addExerciseToCourse(exerciseId, courseId) {
		return ajaxPost(`/api/course/exercise/add?exerciseId=${exerciseId}&courseId=${courseId}`, {});
	}
	removeExerciseFromCourse(exerciseId, courseId) {
		return ajaxDelete(`/api/course/exercise/remove?exerciseId=${exerciseId}&courseId=${courseId}`);
	}
};
//#endregion
//#region src/main/js/types/exercise-attempt.ts
var TExerciseAttempt = type({
	attemptId: number,
	exerciseId: number,
	questionIds: array(number)
}, "ExerciseAttempt");
var TOptionalExerciseAttemptResult = TOptionalRequestResult(TExerciseAttempt);
//#endregion
//#region src/main/js/types/exercise-statistics.ts
var TExerciseStatisticsItem = type({
	attemptId: number,
	questionsCount: number,
	totalInteractionsCount: number,
	totalInteractionsWithErrorsCount: number,
	averageGrade: number
}, "ExerciseStatisticsItem");
var TExerciseStatisticsItems = array(TExerciseStatisticsItem);
//#endregion
//#region src/main/js/types/exercise.ts
var TExercise = type({
	id: number,
	options: TExerciseOptions
}, "Exercise");
//#endregion
//#region src/main/js/controllers/exercise/exercise-controller.ts
var ExerciseController = class {
	getExerciseShortInfo(id, courseId) {
		return ajaxGet(`/api/exercise/shortInfo?id=${id}${courseId != null ? `&courseId=${courseId}` : ""}`, TExercise);
	}
	getExerciseAttempt(attemptId) {
		return ajaxGet(`/api/exercise/getExerciseAttempt?attemptId=${attemptId}`, TExerciseAttempt);
	}
	getExistingExerciseAttempt(exerciseId, courseId) {
		return ajaxGet(`/api/exercise/getExistingExerciseAttempt?exerciseId=${exerciseId}${courseId != null ? `&courseId=${courseId}` : ""}`, TOptionalExerciseAttemptResult);
	}
	createExerciseAttempt(exerciseId, courseId) {
		return ajaxGet(`/api/exercise/createExerciseAttempt?exerciseId=${exerciseId}${courseId != null ? `&courseId=${courseId}` : ""}`, TExerciseAttempt);
	}
	createDebugExerciseAttempt(exerciseId, courseId) {
		return ajaxGet(`/api/exercise/createDebugExerciseAttempt?exerciseId=${exerciseId}${courseId != null ? `&courseId=${courseId}` : ""}`, TExerciseAttempt);
	}
	getExerciseStatistics(exerciseId) {
		return ajaxGet(`/api/exercise/getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems);
	}
	getExercises() {
		return ajaxGet(`/api/exercise/getExercises`, array(number));
	}
};
//#endregion
//#region src/main/js/controllers/exercise/exercise-settings.ts
var ExerciseSettingsController = class {
	listExercises(courseId) {
		return ajaxGet(`/api/exercise/list${courseId == null ? "" : `?courseId=${courseId}`}`, TExerciseList);
	}
	getExercise(id, courseId = null) {
		const courseQ = courseId == null ? "" : `&courseId=${courseId}`;
		return ajaxGet(`/api/exercise?id=${encodeURIComponent(id)}${courseQ}`, TExerciseCard);
	}
	saveExercise(card, courseId = null) {
		return ajaxPost(`/api/exercise${courseId == null ? "" : `?courseId=${courseId}`}`, toJS(card));
	}
	createExercise(name, domainId, strategyId, courseId = null) {
		return ajaxPut(`/api/exercise`, {
			name,
			domainId,
			strategyId,
			courseId
		}, number);
	}
	cloneExercise(id, courseId) {
		return ajaxPost(`/api/exercise/${id}/clone${courseId == null ? "" : `?courseId=${courseId}`}`, {}, number);
	}
	deleteExercise(id, courseId = null) {
		const courseQ = courseId == null ? "" : `&courseId=${courseId}`;
		return ajaxDelete(`/api/exercise?id=${encodeURIComponent(id)}${courseQ}`);
	}
	getStrategies() {
		return ajaxGet(`/api/refTables/strategies`, array(TStrategy));
	}
	getBackends() {
		return ajaxGet(`/api/refTables/backends`, array(string));
	}
	getDomains() {
		return ajaxGet(`/api/refTables/domains`, array(TDomain));
	}
	getDomainLaws(domainsId) {
		return ajaxGet(`/api/refTables/domainLaws?domaindId=${encodeURIComponent(domainsId)}`, array(string));
	}
	getDomainConcepts(domainsId) {
		return ajaxGet(`/api/refTables/domainConcepts?domaindId=${encodeURIComponent(domainsId)}`, array(string));
	}
	search(domainId, concepts, laws, skills, tags, complexity, limit, courseId, signal) {
		return ajaxPost(`/api/question-bank/search`, {
			domainId,
			tags,
			concepts,
			laws,
			skills,
			complexity,
			limit,
			courseId
		}, TQuestionBankSearchResult, signal);
	}
};
//#endregion
//#region src/main/js/types/answer.ts
var TAnswer = intersection([type({
	answer: tuple([number, number]),
	isCreatedByUser: boolean
}), partial({ createdByInteraction: union([number, nullType]) })]);
//#endregion
//#region src/main/js/types/feedback.ts
var TFeedbackViolationLaw = type({
	name: string,
	canCreateSupplementaryQuestion: boolean
});
var TFeedbackMessage = union([type({
	type: literal("SUCCESS"),
	message: string,
	violationLaws: union([array(TFeedbackViolationLaw), nullType])
}), type({
	type: literal("ERROR"),
	message: string,
	violationLaws: union([array(TFeedbackViolationLaw), nullType])
})]);
var TFeedback = intersection([type({ isCorrect: boolean }), partial({
	isCorrect: boolean,
	grade: union([number, nullType]),
	violations: union([array(number), nullType]),
	correctAnswers: union([array(TAnswer), nullType]),
	correctSteps: union([number, nullType]),
	stepsLeft: union([number, nullType]),
	stepsWithErrors: union([number, nullType]),
	message: union([array(TFeedbackMessage), nullType]),
	strategyDecision: union([keyof({
		"CONTINUE": null,
		"FINISH": null
	}), nullType])
})], "Feedback");
var TOrderQuestionFeedback = intersection([TFeedback, partial({ trace: union([array(string), nullType]) })]);
//#endregion
//#region src/main/js/types/question-options.ts
var TQuestionOptions = type({
	requireContext: boolean,
	showSupplementaryQuestions: boolean
}, "QuestionOptions");
var TOrderQuestionOptions = intersection([
	TQuestionOptions,
	type({
		showTrace: boolean,
		multipleSelectionEnabled: boolean,
		requireAllAnswers: boolean
	}),
	partial({ orderNumberOptions: intersection([type({
		delimiter: string,
		position: keyof({
			"PREFIX": null,
			"SUFFIX": null,
			"BOTTOM": null,
			"NONE": null
		})
	}), partial({ replacers: union([array(string), nullType]) })]) })
], "OrderQuestionOptions");
var TMatchingQuestionOptions = intersection([
	TQuestionOptions,
	type({ multipleSelectionEnabled: boolean }),
	union([type({ displayMode: literal("combobox") }), type({
		displayMode: literal("dragNdrop"),
		draggableStyle: string,
		dropzoneStyle: string,
		dropzoneHtml: string
	})])
], "MatchingQuestionOptions");
var TSingleChoiceQuestionOptions = intersection([TQuestionOptions, type({ displayMode: keyof({
	"radio": null,
	"dragNdrop": null
}) })], "SingleChoiceQuestionOptions");
var TMultiChoiceQuestionOptions = intersection([TQuestionOptions, union([intersection([type({ displayMode: literal("switch") }), partial({ selectorReplacers: union([tuple([string, string]), nullType]) })]), type({
	displayMode: literal("dragNdrop"),
	dropzoneHtml: string,
	dropzoneStyle: string,
	draggableStyle: string
})])], "MultiChoiceQuestionOptions");
//#endregion
//#region src/main/js/types/question.ts
var TQuestionType = keyof({
	"SINGLE_CHOICE": null,
	"MULTI_CHOICE": null,
	"MATCHING": null,
	"ORDER": null
}, "QuestionType");
var THtml = string;
var TQuestionAnswer = type({
	id: number,
	text: THtml
}, "QuestionAnswer");
var TQuestionBase = type({
	questionId: number,
	questionMetadataId: number,
	type: TQuestionType,
	options: TQuestionOptions,
	text: THtml,
	answers: array(TQuestionAnswer),
	responses: union([array(TAnswer), nullType]),
	feedback: union([TFeedback, nullType])
}, "QuestionBase");
var TOrderQuestion = intersection([
	TQuestionBase,
	type({
		type: literal("ORDER"),
		options: TOrderQuestionOptions,
		feedback: union([TOrderQuestionFeedback, nullType])
	}),
	partial({ initialTrace: union([array(string), nullType]) })
], "OrderQuestion");
var TSingleChoiceQuestion = intersection([TQuestionBase, type({
	type: literal("SINGLE_CHOICE"),
	options: TSingleChoiceQuestionOptions
})], "SingleChoiceQuestion");
var TMultiChoiceQuestion = intersection([TQuestionBase, type({
	type: literal("MULTI_CHOICE"),
	options: TMultiChoiceQuestionOptions
})], "MultiChoiceQuestion");
var TMatchingQuestion = intersection([TQuestionBase, type({
	type: literal("MATCHING"),
	answers: array(TQuestionAnswer),
	groups: array(TQuestionAnswer),
	options: TMatchingQuestionOptions
})], "MatchingQuestion");
var TQuestion = union([
	TOrderQuestion,
	TSingleChoiceQuestion,
	TMultiChoiceQuestion,
	TMatchingQuestion
], "Question");
TOptionalRequestResult(TQuestion);
//#endregion
//#region src/main/js/types/supplementary-question.ts
var TSupplementaryFeedbackAction = keyof({
	"CONTINUE_AUTO": null,
	"CONTINUE_MANUAL": null,
	"FINISH": null
});
var TSupplementaryFeedback = type({
	message: TFeedbackMessage,
	action: TSupplementaryFeedbackAction
});
var TSupplementaryQuestion = partial({
	question: union([TQuestion, nullType]),
	message: union([TSupplementaryFeedback, nullType])
});
//#endregion
//#region src/main/js/controllers/exercise/question-controller.ts
var QuestionController = class {
	generateQuestionByAttempt(attemptId) {
		return ajaxGet(`/api/question/generate?attemptId=${attemptId}`, TQuestion);
	}
	generateQuestionByMetadata(metadataId) {
		return ajaxGet(`/api/question/generateByMetadata?metadataId=${metadataId}`, TQuestion);
	}
	getQuestion(questionId) {
		return ajaxGet(`/api/question?questionId=${questionId}`, TQuestion);
	}
	generateSupplementaryQuestion(questionRequest) {
		return ajaxPost(`/api/question/generateSupplementaryQuestion`, questionRequest, TSupplementaryQuestion);
	}
	generateNextCorrectAnswer(questionId) {
		return ajaxGet(`/api/question/generateNextCorrectAnswer?questionId=${questionId}`, TFeedback);
	}
	addQuestionAnswer(interaction) {
		return ajaxPost(`/api/question/addQuestionAnswer`, interaction, TFeedback);
	}
	addSupplementaryQuestionAnswer(interaction) {
		return ajaxPost(`/api/question/addSupplementaryQuestionAnswer`, interaction, TSupplementaryFeedback);
	}
};
//#endregion
//#region src/main/js/types/survey.ts
var TSurveyQuestionTriggeringPolicy = union([
	type({ kind: literal("AFTER_FIRST") }),
	type({ kind: literal("AFTER_LAST") }),
	type({ kind: literal("AFTER_EACH") }),
	type({
		kind: literal("AFTER_SPECIFIC"),
		numbers: array(number)
	})
]);
var TYesNoSurveyQuestion = type({
	id: number,
	type: literal("yes-no"),
	text: string,
	policy: TSurveyQuestionTriggeringPolicy,
	required: boolean,
	options: type({
		yesText: string,
		yesValue: string,
		noText: string,
		noValue: string
	})
}, "YesNoSurveyQuestion");
var TSingleChoiceSurveyQuestion = type({
	id: number,
	type: literal("single-choice"),
	text: string,
	policy: TSurveyQuestionTriggeringPolicy,
	required: boolean,
	options: array(type({
		id: string,
		text: string
	}))
});
var TOpenEndedSurveyQuestion = type({
	id: number,
	type: literal("open-ended"),
	text: string,
	policy: TSurveyQuestionTriggeringPolicy,
	required: boolean
});
var TSurveyQuestion = union([
	TYesNoSurveyQuestion,
	TSingleChoiceSurveyQuestion,
	TOpenEndedSurveyQuestion
]);
var TSurvey = type({
	surveyId: string,
	options: type({}),
	questions: array(TSurveyQuestion)
}, "Survey");
var TSurveyResultItem = type({
	surveyQuestionId: number,
	questionId: number,
	answer: string
});
//#endregion
//#region src/main/js/controllers/exercise/survey-controller.ts
var SurveyController = class {
	surveyCache = {};
	async getSurvey(suerveyId) {
		if (this.surveyCache[suerveyId]) return right(this.surveyCache[suerveyId]);
		const result = await ajaxGet(`/api/survey/${suerveyId}`, TSurvey);
		if (isRight(result)) this.surveyCache[suerveyId] = result.right;
		return result;
	}
	async postSurveyAnswer(surveyQuestionId, questionId, answer) {
		return ajaxPost(`/api/survey`, {
			surveyQuestionId,
			questionId,
			answer
		});
	}
	async getCurrentUserAttemptSurveyVotes(surveyId, attemptId) {
		return ajaxGet(`/api/survey/${encodeURIComponent(surveyId)}/user-votes?attemptId=${attemptId}`, array(TSurveyResultItem));
	}
};
//#endregion
//#region src/main/js/types/language.ts
var TLanguage = keyof({
	EN: null,
	RU: null,
	PL: null
});
//#endregion
//#region src/main/js/types/user-info.ts
var TUserPermissions = type({ canViewGlobalPool: boolean }, "UserPermissions");
var TUserInfo = type({
	id: number,
	displayName: string,
	email: union([string, nullType]),
	language: TLanguage,
	permissions: TUserPermissions
}, "UserInfo");
//#endregion
//#region src/main/js/controllers/exercise/user-controller.ts
var UserController = class {
	getCurrentUser() {
		return ajaxGet(`/api/users/whoami`, TUserInfo);
	}
	async getLanguages() {
		return right(["EN", "RU"]);
	}
	async setLanguage(language) {
		return ajaxPost(`/api/users/language`, { language }, TLanguage, void 0, "raw");
	}
};
//#endregion
//#region src/main/js/controllers/lti/deep-linking-controller.ts
var TDeepLinkBuildResponse = type({
	jwt: string,
	returnUrl: string
});
var TDeepLinkExistingResponse = type({ exerciseIds: array(number) });
var DeepLinkingController = class {
	/** Build a signed LtiDeepLinkingResponse for the selected course exercises. */
	build(exerciseIds) {
		return ajaxPost(`/api/lti/deep-link/build`, { exerciseIds }, TDeepLinkBuildResponse);
	}
	/** exercise_id's already added to the Moodle course as activities (AGS-based dedup). */
	existing() {
		return ajaxGet(`/api/lti/deep-link/existing`, TDeepLinkExistingResponse);
	}
};
//#endregion
//#region src/main/js/controllers/index.ts
var courseController = new CourseController();
var deepLinkingController = new DeepLinkingController();
var exerciseController = new ExerciseController();
var exerciseSettingsController = new ExerciseSettingsController();
var questionController = new QuestionController();
var surveyController = new SurveyController();
var userController = new UserController();
//#endregion
//#region src/main/js/stores/sup-question-store.ts
var SupplementaryQuestionStore = class {
	sourceQuestionId;
	feedback = void 0;
	question = void 0;
	answer = [];
	questionState = "INITIAL";
	constructor(sourceQuestionId) {
		this.sourceQuestionId = sourceQuestionId;
		makeAutoObservable(this);
	}
	setQuestionState = (newState) => {
		if (this.questionState !== newState) this.questionState = newState;
	};
	get isQuestionFreezed() {
		return this.questionState !== "LOADED";
	}
	get isFeedbackLoading() {
		return this.questionState === "ANSWER_EVALUATING";
	}
	get canSendQuestionAnswers() {
		if (!this.question || this.questionState === "COMPLETED") return false;
		switch (this.question.type) {
			case "SINGLE_CHOICE":
			case "MULTI_CHOICE": return this.answer.length > 0;
			case "ORDER": return true;
			case "MATCHING": return this.answer.length === this.question.answers.length;
			default: return absurd(this.question);
		}
	}
	get questionSubmitMode() {
		if (!this.question) return null;
		return this.question.type === "SINGLE_CHOICE" ? "IMPLICIT" : "EXPLICIT";
	}
	generateSupplementaryQuestion = async (violationLaws) => {
		if (violationLaws.length === 0) throw new Error("violationLaws mist be non-empty");
		this.setQuestionState("LOADING");
		const questionRequest = {
			questionId: this.sourceQuestionId,
			violationLaws
		};
		const dataEither = await questionController.generateSupplementaryQuestion(questionRequest);
		if (isLeft(dataEither)) {
			this.setQuestionState("LOADED");
			return;
		}
		this.#onQuestionLoaded(dataEither.right.question, dataEither.right.message);
	};
	sendAnswers = async () => {
		const { question } = this;
		if (!question) throw new Error("Question is empty");
		const body = toJS({
			questionId: question.questionId,
			answers: toJS([...this.answer])
		});
		this.setQuestionState("ANSWER_EVALUATING");
		const feedbackEither = await questionController.addSupplementaryQuestionAnswer(body);
		if (isLeft(feedbackEither)) {
			this.setQuestionState("LOADED");
			return;
		}
		this.setQuestionState("COMPLETED");
		this.feedback = feedbackEither.right;
	};
	setAnswer = (newAnswer) => {
		this.answer = newAnswer;
	};
	#onQuestionLoaded = (question, feedback) => {
		if (question?.options.requireContext) [...question.text.matchAll(/(<\w.*?\sid\s*?=(['"]))\s*(answer_(\d+?))\2(.*?>)/gim)].forEach((match, matchIdx) => {
			question.text = question.text.replace(match[0], `${match[1]}question_${question.questionId}_${match[3]}_${matchIdx}${match[2]} data-answer-id='${match[4]}' ${match[5]}`);
		});
		this.question = question ?? void 0;
		this.feedback = feedback ?? void 0;
		this.answer = question?.responses ?? [];
		this.questionState = !question ? "COMPLETED" : "LOADED";
	};
};
//#endregion
//#region src/main/js/stores/question-store.ts
/**
* Store question data
*/
var QuestionStore = class {
	isFeedbackVisible = true;
	isQuestionFreezed = false;
	feedback = void 0;
	question = void 0;
	lastAnswer = [];
	answersHistory = [];
	supplementaryQuestion;
	questionState = "INITIAL";
	storeState = { tag: "VALID" };
	constructor() {
		makeAutoObservable(this);
	}
	onQuestionLoaded = (question) => {
		if (question.options.requireContext) [...question.text.matchAll(/(<\w.*?\sid\s*?=(['"]))\s*(answer_(\d+?))\2(.*?>)/gim)].forEach((match, matchIdx) => {
			question.text = question.text.replace(match[0], `${match[1]}question_${question.questionId}_${match[3]}_${matchIdx}${match[2]} data-answer-id='${match[4]}' ${match[5]}`);
		});
		this.question = question;
		this.supplementaryQuestion = new SupplementaryQuestionStore(question.questionId);
		this.feedback = question.feedback ?? void 0;
		this.isFeedbackVisible = true;
		this.answersHistory = [];
		this.lastAnswer = question.responses ?? [];
		if (question.feedback && question.feedback.stepsLeft === 0) this.setQuestionState("COMPLETED");
	};
	onAnswerEvaluated(feedback) {
		this.feedback = feedback;
		this.isFeedbackVisible = true;
		if (feedback && feedback.correctAnswers) {
			this.setFullAnswer(feedback.correctAnswers, false);
			if (!isNullOrUndefined(feedback.stepsLeft) && feedback.stepsLeft === 0) this.setQuestionState("COMPLETED");
		}
	}
	setQuestionState = (newState) => {
		if (this.questionState !== newState) this.questionState = newState;
	};
	setValidStoreState = () => {
		if (this.storeState.tag !== "VALID") this.storeState = { tag: "VALID" };
	};
	setErrorStoreState = (error) => {
		this.storeState = {
			tag: "ERROR",
			error
		};
	};
	loadQuestion = async (questionId) => {
		this.setValidStoreState();
		this.setQuestionState("LOADING");
		const dataEither = await questionController.getQuestion(questionId);
		this.setQuestionState("LOADED");
		if (isLeft(dataEither)) {
			this.setErrorStoreState(dataEither.left);
			return;
		}
		this.onQuestionLoaded(dataEither.right);
	};
	generateQuestion = async (attemptId) => {
		this.setValidStoreState();
		this.setQuestionState("LOADING");
		const dataEither = await questionController.generateQuestionByAttempt(attemptId);
		this.setQuestionState("LOADED");
		if (isLeft(dataEither)) {
			this.setErrorStoreState(dataEither.left);
			return;
		}
		this.onQuestionLoaded(dataEither.right);
	};
	generateQuestionByMetadata = async (metadataId) => {
		this.setValidStoreState();
		this.setQuestionState("LOADING");
		const dataEither = await questionController.generateQuestionByMetadata(metadataId);
		this.setQuestionState("LOADED");
		if (isLeft(dataEither)) {
			this.setErrorStoreState(dataEither.left);
			return;
		}
		this.onQuestionLoaded(dataEither.right);
	};
	generateNextCorrectAnswer = async () => {
		const { question } = this;
		if (!question) throw new Error("Current question not found");
		this.setValidStoreState();
		this.setQuestionState("ANSWER_EVALUATING");
		const feedbackEither = await questionController.generateNextCorrectAnswer(question.questionId);
		this.setQuestionState("LOADED");
		if (isLeft(feedbackEither)) {
			this.setErrorStoreState(feedbackEither.left);
			return;
		}
		this.onAnswerEvaluated(feedbackEither.right);
	};
	sendAnswersImpl = async (questionId, answers) => {
		const body = toJS({
			questionId,
			answers: toJS([...answers])
		});
		this.setValidStoreState();
		this.setQuestionState("ANSWER_EVALUATING");
		const feedbackEither = await questionController.addQuestionAnswer(body);
		this.setQuestionState("LOADED");
		if (isLeft(feedbackEither)) {
			this.setErrorStoreState(feedbackEither.left);
			return;
		}
		this.onAnswerEvaluated(feedbackEither.right);
	};
	sendAnswers = async () => {
		const { question, lastAnswer } = this;
		if (!question) return;
		await this.sendAnswersImpl(question.questionId, toJS(lastAnswer));
	};
	onAnswersChanged = async (answer, sendAnswers = true) => {
		this.answersHistory.push(answer);
		if (!sendAnswers) return;
		try {
			await this.sendAnswers();
		} catch {
			this.answersHistory.pop();
		}
	};
	setFullAnswer = async (fullAnswer, sendAnswers = true) => {
		if (!this.isAnswerChanged(fullAnswer)) return false;
		const prevLastAnswer = this.lastAnswer;
		this.lastAnswer = fullAnswer;
		if (prevLastAnswer.length > 0) this.answersHistory.push(prevLastAnswer);
		if (!sendAnswers) return true;
		try {
			await this.sendAnswers();
			return true;
		} catch {
			this.lastAnswer = prevLastAnswer;
			if (prevLastAnswer.length > 0) this.answersHistory.pop();
			return false;
		}
	};
	isAnswerChanged = (newAnswer) => {
		const { lastAnswer, question } = this;
		if (!question) throw new Error("no question");
		const answersHistoryRaw = lastAnswer.map((x) => x.answer);
		const newHistoryRaw = newAnswer.map((x) => x.answer);
		switch (question.type) {
			case "ORDER": return newHistoryRaw.length !== answersHistoryRaw.length || JSON.stringify(newHistoryRaw) !== JSON.stringify(answersHistoryRaw);
			case "MATCHING":
			case "MULTI_CHOICE":
			case "SINGLE_CHOICE": return newHistoryRaw.length !== answersHistoryRaw.length || JSON.stringify(newHistoryRaw.sort()) !== JSON.stringify(answersHistoryRaw.sort());
		}
	};
};
//#endregion
//#region src/main/js/stores/exercise-store.ts
var ExerciseStore = class {
	isExerciseLoading = false;
	exerciseId;
	courseId = void 0;
	exercise = void 0;
	currentAttemptId = void 0;
	currentAttempt = void 0;
	currentQuestion;
	exerciseState = "INITIAL";
	storeState = { tag: "VALID" };
	survey = void 0;
	isDebug = false;
	constructor() {
		this.isDebug = getUrlParameterByName("debug") !== null;
		this.currentQuestion = new QuestionStore();
		const rawExerciseId = getUrlParameterByName("exerciseId");
		if (rawExerciseId === null) {
			this.exerciseState = "LAUNCH_ERROR";
			this.storeState = {
				tag: "ERROR",
				error: { message: "Invalid exercise id" }
			};
		}
		this.exerciseId = rawExerciseId !== null ? +rawExerciseId : -1;
		const rawCourseId = getUrlParameterByName("courseId");
		if (rawCourseId !== null) this.courseId = +rawCourseId;
		const rawAttemptId = getUrlParameterByName("attemptId");
		if (rawAttemptId !== null) this.currentAttemptId = +rawAttemptId;
		makeAutoObservable(this, {
			setExerciseState: action,
			ensureQuestionSurveyExists: action
		});
		this.registerOnStrategyDecisionChangedAction();
	}
	registerOnStrategyDecisionChangedAction = () => {
		autorun(() => {
			if (this.currentQuestion.feedback?.strategyDecision === "FINISH" && this.exerciseState !== "COMPLETED") this.setExerciseState("COMPLETED");
		});
	};
	forceSetValidState = () => {
		if (this.storeState.tag !== "VALID") this.storeState = { tag: "VALID" };
	};
	setExerciseState = (newState) => {
		if (this.exerciseState !== newState) this.exerciseState = newState;
	};
	setSurveyAnswers = (quesionId, answers) => {
		if (!this.survey) return;
		this.survey.questions[quesionId].status = "COMPLETED";
		this.survey.questions[quesionId].results = answers;
	};
	loadExercise = async () => {
		if (this.exercise) throw new Error("exerciseInfo loaded");
		if (this.isExerciseLoading) return;
		this.forceSetValidState();
		this.isExerciseLoading = true;
		const exercise = await exerciseController.getExerciseShortInfo(this.exerciseId, this.courseId);
		this.isExerciseLoading = false;
		if (isRight(exercise)) this.exercise = exercise.right;
		else this.storeState = {
			tag: "ERROR",
			error: exercise.left
		};
	};
	loadExerciseAttempt = async (attemptId) => {
		if (!this.exercise) throw new Error("exerciseInfo is not defined");
		this.forceSetValidState();
		const resultEither = await exerciseController.getExerciseAttempt(attemptId);
		if (isLeft(resultEither)) {
			this.storeState = {
				tag: "ERROR",
				error: resultEither.left
			};
			return;
		}
		if (!resultEither.right) return false;
		this.currentAttempt = resultEither.right;
		await this.onAttemptLoaded();
		return true;
	};
	loadExistingExerciseAttempt = async () => {
		const { exercise } = this;
		if (!exercise) throw new Error("exercise is not defined");
		this.forceSetValidState();
		const resultEither = await exerciseController.getExistingExerciseAttempt(exercise.id, this.courseId);
		if (isLeft(resultEither)) {
			this.storeState = {
				tag: "ERROR",
				error: resultEither.left
			};
			return;
		}
		if (!resultEither.right) return false;
		this.currentAttempt = resultEither.right;
		await this.onAttemptLoaded();
		return true;
	};
	onAttemptLoaded = async () => {
		await this.loadSurvey();
	};
	createExerciseAttempt = async () => {
		const { exercise } = this;
		if (!exercise) throw new Error("exercise is not defined");
		this.forceSetValidState();
		const resultEither = await exerciseController.createExerciseAttempt(exercise.id, this.courseId);
		if (isLeft(resultEither)) {
			this.storeState = {
				tag: "ERROR",
				error: resultEither.left
			};
			return;
		}
		this.currentAttempt = resultEither.right;
		await this.onAttemptLoaded();
	};
	createDebugExerciseAttempt = async () => {
		const { exercise } = this;
		if (!exercise) throw new Error("exercise is not defined");
		this.forceSetValidState();
		const resultEither = await exerciseController.createDebugExerciseAttempt(exercise.id, this.courseId);
		if (isLeft(resultEither)) {
			this.storeState = {
				tag: "ERROR",
				error: resultEither.left
			};
			return;
		}
		this.currentAttempt = resultEither.right;
		await this.onAttemptLoaded();
	};
	generateQuestion = async () => {
		const { exercise, currentAttempt } = this;
		if (!exercise || !currentAttempt) throw new Error("Session is not defined");
		this.forceSetValidState();
		await this.currentQuestion.generateQuestion(currentAttempt.attemptId);
		currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
	};
	loadSurvey = async () => {
		if (this.survey || !this.currentAttempt || !this.exercise) return;
		if (!this.exercise.options.surveyOptions?.enabled || this.exercise.options.surveyOptions.surveyId.length === 0) return;
		const surveyId = this.exercise.options.surveyOptions.surveyId;
		const attemptId = this.currentAttempt.attemptId;
		const [survey, surveyResults] = await Promise.all([surveyController.getSurvey(surveyId), surveyController.getCurrentUserAttemptSurveyVotes(surveyId, attemptId)]);
		if (isRight(survey) && isRight(surveyResults)) {
			const tmp = groupBy(surveyResults.right, (x) => x.questionId);
			this.survey = {
				survey: survey.right,
				questions: [...tmp.keys()].map((k) => ({
					questionId: k,
					status: "COMPLETED",
					questions: tmp.get(k)?.map((z) => z.surveyQuestionId) ?? [],
					results: tmp.get(k)?.reduce((acc, z) => (acc[z.surveyQuestionId] = z.answer, acc), {}) ?? {}
				})).reduce((acc, i) => (acc[i.questionId] = i, acc), {})
			};
		}
	};
	ensureQuestionSurveyExists = (questionId) => {
		if (this.survey?.questions[questionId]) return this.survey?.questions[questionId].questions;
		const qs = [];
		const currentQuestionIdx = this.currentAttempt.questionIds.findIndex((z) => z === this.currentQuestion.question?.questionId);
		for (const q of this.survey?.survey.questions || []) {
			const policy = q.policy;
			if (policy.kind === "AFTER_EACH" || policy.kind === "AFTER_FIRST" && currentQuestionIdx === 0 || policy.kind === "AFTER_LAST" && this.exerciseState === "COMPLETED" || policy.kind === "AFTER_SPECIFIC" && policy.numbers.includes(currentQuestionIdx + 1)) qs.push(q);
		}
		console.log("Selected questions");
		console.log(toJS(qs));
		const questionSurvey = {
			questionId,
			status: "ACTIVE",
			questions: qs.map((z) => z.id),
			results: {}
		};
		this.survey.questions[questionId] = questionSurvey;
		return qs.map((z) => z.id);
	};
};
function groupBy(list, keyGetter) {
	const map = /* @__PURE__ */ new Map();
	list.forEach((item) => {
		const key = keyGetter(item);
		const collection = map.get(key);
		if (!collection) map.set(key, [item]);
		else collection.push(item);
	});
	return map;
}
var sharedExerciseStore;
/** The exercise page and every component inside it work with one and the same store. */
function getExerciseStore() {
	return sharedExerciseStore ??= new ExerciseStore();
}
//#endregion
//#region src/main/js/components/common/question/answer-slot.ts
/**
* The backend sends question text as html, and `QuestionStore.onQuestionLoaded` marks the
* answer positions in it with `data-answer-id`. Returns that id for a slot node, or null
* for anything else.
*/
function answerSlotId(node) {
	const attribs = node.attribs;
	if (node.type !== "tag" || attribs?.["data-answer-id"] === void 0) return null;
	return +attribs["data-answer-id"];
}
//#endregion
//#region src/main/js/components/common/question/matching-question.tsx
/**
* The store owns the answers, so the selects are controlled by it: `defaultValue` would
* only be read once and would ignore everything that does not come from a click - a
* reloaded question, or a step the server filled in.
*/
var selectedOption = (options, answers, slotId) => options.find((o) => o.value === answers.find((a) => a.answer[0] === slotId)?.answer[1]) ?? null;
var MatchingQuestionComponent = observer((props) => {
	const { question } = props;
	const { options } = question;
	switch (true) {
		case options.displayMode === "combobox" && !options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ComboboxMatchingQuestionComponent, { ...props });
		case options.displayMode === "combobox" && options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ComboboxMatchingQuestionWithCtxComponent, { ...props });
		case options.displayMode === "dragNdrop": return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DragAndDropMatchingQuestionComponent, { ...props });
	}
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: "Not Implemented" });
});
var DragAndDropMatchingQuestionComponent = observer((props) => {
	const { question, getAnswers, getFeedback, onChanged } = props;
	if (question.options.displayMode !== "dragNdrop") return null;
	const { groups = [] } = question;
	const { options } = question;
	const dropzoneStyle = options.dropzoneStyle && JSON.parse(options.dropzoneStyle) || {};
	const draggableStyle = options.dropzoneStyle && JSON.parse(options.draggableStyle) || {};
	(0, import_react.useEffect)(() => {
		document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`).forEach((e) => {
			e.classList.add("comp-ph-dropzone");
			Object.assign(e.style, dropzoneStyle);
			e.innerHTML = `<div class="comp-ph-dropzone-placeholder">${options.dropzoneHtml}</div>`;
		});
		const droppable = new Droppable(document.querySelectorAll(".comp-ph-droppable-container"), {
			draggable: ".comp-ph-draggable",
			dropzone: ".comp-ph-dropzone",
			plugins: [ResizeMirror],
			mirror: { constrainDimensions: true }
		});
		droppable.on("drag:over", () => console.log("is out"));
		droppable.on("droppable:stop", (e) => {
			const draggableId = e.dragEvent?.source?.id;
			const droppableId = e.dropzone?.id;
			if (!draggableId || !droppableId) return;
			if (options.multipleSelectionEnabled) {
				const wrapperId = `dragAnswerWrapper_${draggableId.split("_")[1] ?? ""}`;
				const wrapper = document.getElementById(wrapperId);
				const draggable = document.getElementById(draggableId);
				if (wrapperId !== droppableId && wrapper && draggable) wrapper.innerHTML = draggable.outerHTML;
			}
			setTimeout(() => {
				const newHistory = [...document.querySelectorAll(`[id^="question_${question.questionId}_answer_"] > [id^="dragAnswer_"]`)].map((e) => {
					const slot = e.parentElement;
					const leftId = slot?.getAttribute("data-answer-id") ?? slot?.id.split(`question_${question.questionId}_answer_`)[1] ?? "";
					const rightId = e?.id.split("dragAnswer_")[1] ?? "";
					return [+leftId, +rightId];
				});
				const oldHistory = getAnswers();
				onChanged(newHistory.map((h) => ({
					answer: h,
					isCreatedByUser: oldHistory.find((x) => x.answer[0] === h[0] && x.answer[1] === h[1])?.isCreatedByUser ?? true
				})));
			}, 10);
		});
	}, [question.questionId]);
	const answerKey = getAnswers().map((a) => a.answer.join(":")).join(",");
	const confirmed = new Set((getFeedback?.()?.correctAnswers ?? []).map((a) => a.answer.join(":")));
	const confirmedKey = [...confirmed].join(",");
	(0, import_react.useEffect)(() => {
		const slots = Array.from(document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`));
		const answers = getAnswers();
		slots.forEach((slot) => {
			const slotId = +(slot.getAttribute("data-answer-id") ?? slot.id.split(`question_${question.questionId}_answer_`)[1] ?? "");
			const answer = answers.find((a) => a.answer[0] === slotId);
			const placed = slot.querySelector(".comp-ph-draggable");
			const placedGroupId = +(placed?.id.split("dragAnswer_")[1] ?? "");
			if (placed && answer?.answer[1] !== placedGroupId) {
				const wrapper = document.getElementById(`dragAnswerWrapper_${placedGroupId}`);
				if (!options.multipleSelectionEnabled && wrapper && !wrapper.querySelector(".comp-ph-draggable")) wrapper.appendChild(placed);
				else placed.remove();
				slot.classList.remove("draggable-dropzone--occupied");
			}
			if (answer && !slot.querySelector(".comp-ph-draggable")) {
				const source = document.querySelector(`#dragAnswerWrapper_${answer.answer[1]} .comp-ph-draggable`);
				if (source) {
					slot.appendChild(options.multipleSelectionEnabled ? source.cloneNode(true) : source);
					slot.classList.add("draggable-dropzone--occupied");
				}
			}
			slot.classList.toggle("comp-ph-answer-locked", answer !== void 0 && confirmed.has(answer.answer.join(":")));
		});
	}, [
		question.questionId,
		answerKey,
		confirmedKey
	]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [!options.requireContext && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
		className: "mb-3 comp-ph-question-text",
		dangerouslySetInnerHTML: { __html: question.text }
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "row",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "col-md",
			children: !options.requireContext ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "d-flex flex-column comp-ph-droppable-container comp-ph-question-text",
				children: question.answers.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "d-flex flex-row mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "me-2 mt-1",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { id: `question_${question.questionId}_answer_${a.id}` })
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { dangerouslySetInnerHTML: { __html: a.text } })]
				}))
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "comp-ph-droppable-container comp-ph-question-text",
				dangerouslySetInnerHTML: { __html: question.text }
			})
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "col-md comp-ph-droppable-container d-flex justify-content-start align-items-start flex-column",
			children: groups.map((g) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				id: `dragAnswerWrapper_${g.id}`,
				className: "comp-ph-dropzone mb-2",
				style: dropzoneStyle,
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "comp-ph-dropzone-placeholder",
					dangerouslySetInnerHTML: { __html: options.dropzoneHtml }
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					id: `dragAnswer_${g.id}`,
					className: "comp-ph-draggable",
					style: draggableStyle,
					dangerouslySetInnerHTML: { __html: g.text }
				})]
			}))
		})]
	})] });
});
var ComboboxMatchingQuestionComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "combobox") return null;
	const { groups = [] } = question;
	const groupsMaxLength = groups.reduce((len, g) => g.text.length > len ? g.text.length : len, 0);
	const groupOptions = groups.map((g) => ({
		value: g.id,
		label: g.text
	}));
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
		className: "mb-5 comp-ph-question-text",
		dangerouslySetInnerHTML: { __html: question.text }
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: question.answers.map((asw) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "row mb-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "col-md-6",
			dangerouslySetInnerHTML: { __html: asw.text }
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "col-md-auto",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				style: { width: `${8 * groupsMaxLength + 100}px` },
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(StateManagedSelect$1, {
					value: selectedOption(groupOptions, getAnswers(), asw.id),
					options: groupOptions,
					components: {
						Option: RawHtmlSelectOption,
						SingleValue: RawHtmlSelectSingleValue
					},
					onChange: ((v) => {
						if (!v) return;
						const otherHistoryItems = getAnswers().filter((v) => v.answer[0] !== asw.id);
						const historyItem = {
							answer: [asw.id, +v.value],
							isCreatedByUser: true
						};
						const newAnswersHistory = [...otherHistoryItems, historyItem];
						onChanged(newAnswersHistory);
					})
				})
			})
		})]
	})) })] });
});
var ComboboxMatchingQuestionWithCtxComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "combobox") return null;
	const { groups = [] } = question;
	const groupOptions = groups.map((g) => ({
		value: g.id,
		label: g.text
	}));
	const content = esm_default(question.text, { replace: (node) => {
		const answerId = answerSlotId(node);
		if (answerId === null) return;
		return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(StateManagedSelect$1, {
			value: selectedOption(groupOptions, getAnswers(), answerId),
			options: groupOptions,
			components: {
				Option: RawHtmlSelectOption,
				SingleValue: RawHtmlSelectSingleValue
			},
			onChange: ((v) => {
				if (!v) return;
				const otherHistoryItems = getAnswers().filter((v) => v.answer[0] !== answerId);
				const historyItem = {
					answer: [answerId, +v.value],
					isCreatedByUser: true
				};
				const newAnswersHistory = [...otherHistoryItems, historyItem];
				onChanged(newAnswersHistory);
			})
		});
	} });
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		id: `question_${question.questionId}`,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "comp-ph-question-text",
			children: content
		})
	});
});
var RawHtmlSelectOption = (props) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(components.Option, {
	...props,
	children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { dangerouslySetInnerHTML: { __html: props.data.label } })
});
var RawHtmlSelectSingleValue = (props) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(components.SingleValue, {
	...props,
	children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { dangerouslySetInnerHTML: { __html: props.data.label } })
});
//#endregion
//#region src/main/js/components/common/toggle.tsx
var ClickableLabel = ({ id, title, value, onChange, isChecked, style, ...props }) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
	htmlFor: id,
	onClick: () => onChange(value),
	style: isChecked && style || void 0,
	...props,
	children: title
});
var ConcealedRadio = ({ id, value, name, selected, ...props }) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
	id,
	type: "radio",
	name,
	checked: selected === value,
	readOnly: true,
	...props
});
var ToggleSwitch = observer((props) => {
	const handleChange = (val) => {
		props.onChange?.(val);
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		id: props.id,
		className: "switch-field d-inline-flex flex-row",
		children: props.values.map((val, i) => {
			return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ConcealedRadio, {
				id: `${props.id}_${val}_checkbox`,
				name: `${props.id}_switch`,
				"data-value": val,
				value: val,
				selected: props.selected,
				...props.inputAttributes
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ClickableLabel, {
				id: `${props.id}_${val}_checkbox`,
				isChecked: val === props.selected,
				title: props.displayNames?.[i] ?? val,
				value: val,
				onChange: handleChange,
				style: props.valueStyles?.[i] ?? void 0,
				...props.inputAttributes
			})] }, i);
		})
	});
});
//#endregion
//#region src/main/js/components/common/question/multi-choice-question.tsx
var MultiChoiceQuestionComponent = observer((props) => {
	const { question } = props;
	const { options } = question;
	switch (true) {
		case options.displayMode == "switch" && !options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SwitchMultiChoiceQuestionComponent, { ...props });
		case options.displayMode === "switch" && options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SwitchMultiChoiceQuestionWithCtxComponent, { ...props });
		case options.displayMode === "dragNdrop": return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DndMultiChoiceQuestionComponent, { ...props });
	}
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: "Not implemented" });
});
var SwitchMultiChoiceQuestionComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "switch") return null;
	const { options } = question;
	const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
	const onSwitched = (answerId, val) => {
		const value = selectorTexts.indexOf(val);
		const newHistory = [...getAnswers().filter((v) => v.answer[0] !== answerId), {
			answer: [answerId, value],
			isCreatedByUser: true
		}];
		onChanged(newHistory);
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "comp-ph-question-text",
		dangerouslySetInnerHTML: { __html: question.text }
	}) }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
		className: "d-flex flex-column",
		children: question.answers.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "d-flex flex-row mb-3",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "me-2 mt-1",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
					id: `question_${question.questionId}_anwser_${a.id}`,
					selected: selectorTexts[getAnswers().filter((h) => h.answer[0] === a.id)?.[0]?.answer?.[1]] ?? "",
					values: selectorTexts,
					inputAttributes: { "data-answer-id": a.id },
					onChange: (val) => onSwitched(a.id, val)
				})
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: a.text })]
		}))
	})] });
});
var SwitchMultiChoiceQuestionWithCtxComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "switch") return null;
	const { options } = question;
	const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
	const onSwitched = (answerId, val) => {
		const value = selectorTexts.indexOf(val);
		const newHistory = [...getAnswers().filter((v) => v.answer[0] !== answerId), {
			answer: [answerId, value],
			isCreatedByUser: true
		}];
		onChanged(newHistory);
	};
	const content = esm_default(question.text, { replace: (node) => {
		const id = answerSlotId(node);
		if (id === null) return;
		return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
			id: `toggle_answer_${id}`,
			selected: selectorTexts[getAnswers().filter((h) => h.answer[0] === id)?.[0]?.answer?.[1]] ?? "",
			inputAttributes: { "data-answer-id": id },
			values: selectorTexts,
			onChange: (val) => onSwitched(id, val)
		});
	} });
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		id: `question_${question.questionId}`,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "comp-ph-question-text",
			children: content
		})
	});
});
var DndMultiChoiceQuestionComponent = observer((props) => {
	const { question, getAnswers, getFeedback, onChanged, answers } = props;
	if (question.options.displayMode !== "dragNdrop") return null;
	const matchingQuestion = {
		...question,
		type: "MATCHING",
		options: {
			...question.options,
			multipleSelectionEnabled: true
		},
		groups: [{
			id: 0,
			text: `<span class="badge bg-success" style="height: 100%; width: 100%;display: flex; align-items: center;justify-content: center;">✓</span>`
		}, {
			id: 1,
			text: `<span class="badge bg-danger" style="height: 100%; width: 100%;display: flex; align-items: center;justify-content: center;">x</span>`
		}]
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DragAndDropMatchingQuestionComponent, {
		question: matchingQuestion,
		getAnswers,
		getFeedback,
		onChanged,
		answers
	});
});
//#endregion
//#region src/main/js/components/common/question/order-question.tsx
var OrderQuestionComponent = observer((props) => {
	const { question, getAnswers, onChanged, getFeedback } = props;
	if (!question.options.requireContext) return null;
	const { options } = question;
	const orderNumberOptions = options.orderNumberOptions ?? {
		delimiter: "/",
		position: "SUFFIX"
	};
	const originalText = (0, import_react.useMemo)(() => {
		const originalText = document.createElement("div");
		originalText.innerHTML = question.text;
		return originalText;
	}, [question.text]);
	(0, import_react.useEffect)(() => {
		document.querySelectorAll(`#question_${question.questionId} [data-answer-id]`).forEach((e) => {
			const id = +(e.getAttribute("data-answer-id") ?? "");
			e.addEventListener("click", () => onChanged([...getAnswers(), {
				answer: [id, id],
				isCreatedByUser: true
			}]));
		});
		document.querySelectorAll("[data-comp-ph-value]").forEach((e) => {
			const value = e.getAttribute("data-comp-ph-value");
			e.innerHTML += `<span class="comp-ph-expr-bottom-hint">${value}</span>`;
		});
		document.querySelectorAll("[data-comp-ph-pos]").forEach((e) => {
			const pos = e.getAttribute("data-comp-ph-pos");
			e.innerHTML += `<span class="comp-ph-expr-top-hint">${pos}</span>`;
		});
	}, [question.questionId]);
	const answersCount = getAnswers().length;
	(0, import_react.useEffect)(() => {
		document.querySelectorAll(`#question_${question.questionId} [data-answer-id]`).forEach((e) => {
			const value = e.getAttribute("data-comp-ph-value");
			const pos = e.getAttribute("data-comp-ph-pos");
			e.innerHTML = originalText.querySelector(`#${e.id}`)?.innerHTML + (pos ? `<span class="comp-ph-expr-top-hint">${pos}</span>` : "") + (value ? `<span class="comp-ph-expr-bottom-hint">${value}</span>` : "");
			e.classList.remove("disabled");
			e.classList.remove("comp-ph-question-answer--last-selected-by-system");
		});
		const lastGeneratedAnswers = !(getFeedback()?.isCorrect ?? true) ? [] : [...getAnswers()].reverse().reduce(((acc, answer) => {
			const [prevInteractionId, result] = acc;
			if (prevInteractionId === -1) return acc;
			if (answer.isCreatedByUser || prevInteractionId !== 0 && prevInteractionId !== answer.createdByInteraction) return [-1, result];
			result.push(answer);
			return [answer.createdByInteraction || -1, result];
		}), [0, []])[1].reverse();
		getAnswers().forEach((answer, idx) => {
			const { answer: asnwerPair } = answer;
			const [h] = asnwerPair;
			const answrs = document.querySelectorAll(`[data-answer-id='${h}']`);
			if (!answrs.length) return 0;
			answrs.forEach((answr) => {
				if (lastGeneratedAnswers.includes(answer)) answr.classList.add("comp-ph-question-answer--last-selected-by-system");
				if (orderNumberOptions.position !== "NONE") {
					const delim = orderNumberOptions.delimiter;
					const orderNumber = orderNumberOptions.replacers?.[idx] ?? idx + 1;
					answr.innerHTML = orderNumberOptions.position === "PREFIX" ? `${orderNumber}${delim}${answr.innerHTML}` : orderNumberOptions.position === "SUFFIX" ? `${answr.innerHTML}${delim}${orderNumber}` : orderNumberOptions.position === "BOTTOM" ? `<span class="comp-ph-expr-bottom-hint">${delim}${orderNumber}</span>${answr.innerHTML}` : answr.innerHTML;
				}
				if (!options.multipleSelectionEnabled) answr.classList.add("disabled");
			});
		});
	}, [question.questionId, answersCount]);
	const trace = getFeedback()?.trace ?? (getAnswers().length === 0 ? question.initialTrace : null);
	const isTraceVisible = options.showTrace && !isNullOrUndefined(trace) && trace.length > 0;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		id: `question_${question.questionId}`,
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "comp-ph-question-text",
			dangerouslySetInnerHTML: { __html: question.text }
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
			isVisible: isTraceVisible,
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("table", {
				className: "comp-ph-trace",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("tbody", { children: trace?.map((t, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("tr", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("td", { dangerouslySetInnerHTML: { __html: t } }) }, idx)) })
			}) })
		})]
	});
});
//#endregion
//#region src/main/js/components/common/question/single-choice-question.tsx
var SingleChoiceQuestionComponent = observer((props) => {
	const { question } = props;
	switch (true) {
		case question.options.displayMode === "radio" && !question.options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(RadioSingleChoiceQuestionComponent, { ...props });
		case question.options.displayMode === "radio" && question.options.requireContext: return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(RadioSingleChoiceQuestionWithCtxComponent, { ...props });
	}
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: "Not implemented" });
});
var RadioSingleChoiceQuestionComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "radio") return null;
	const selfOnChange = (answerId, checked) => {
		if (checked) onChanged([{
			answer: [answerId, answerId],
			isCreatedByUser: true
		}]);
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mb-3",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "comp-ph-question-text",
			dangerouslySetInnerHTML: { __html: question.text }
		})
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "d-flex flex-column",
		children: question.answers.map((a, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("label", {
			htmlFor: `question_${question.questionId}_answer_${a.id}`,
			className: `comp-ph-singlechoice-label d-flex flex-row ${idx !== question.answers.length - 1 && "mb-3" || ""}`,
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "me-2 mt-1",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
					id: `question_${question.questionId}_answer_${a.id}`,
					name: `switch_${question.questionId}`,
					type: "radio",
					checked: getAnswers().some((h) => h.answer[0] === a.id),
					onChange: (e) => selfOnChange(a.id, e.target.checked),
					readOnly: true
				})
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { dangerouslySetInnerHTML: { __html: a.text } })]
		}, a.id))
	})] });
});
var RadioSingleChoiceQuestionWithCtxComponent = observer((props) => {
	const { question, getAnswers, onChanged } = props;
	if (question.options.displayMode !== "radio") return null;
	const selfOnChange = (answerId, checked) => {
		if (checked) onChanged([{
			answer: [answerId, answerId],
			isCreatedByUser: true
		}]);
	};
	const content = esm_default(question.text, { replace: (node) => {
		const id = answerSlotId(node);
		if (id === null) return;
		return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("label", {
			htmlFor: `question_${question.questionId}_answer_${id}`,
			className: "comp-ph-singlechoice-label",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
				id: `question_${question.questionId}_answer_${id}`,
				name: `switch_${question.questionId}`,
				"data-answer-id": id,
				type: "radio",
				checked: getAnswers().some((h) => h.answer[0] === id),
				onChange: (e) => selfOnChange(id, e.target.checked),
				readOnly: true
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: (0, import_lib.domToReact)(node.children) })]
		});
	} });
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		id: `question_${question.questionId}`,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "comp-ph-question-text",
			children: content
		})
	});
});
//#endregion
//#region src/main/js/components/common/question/question.tsx
var QuestionComponent = observer((props) => {
	const { question, answers, onChanged, getAnswers, getFeedback, isFeedbackLoading, isQuestionFreezed } = props;
	let questonComponent;
	switch (question.type) {
		case "MATCHING":
			questonComponent = /* @__PURE__ */ (0, import_jsx_runtime.jsx)(MatchingQuestionComponent, {
				question,
				onChanged,
				answers,
				getAnswers,
				getFeedback
			});
			break;
		case "MULTI_CHOICE":
			questonComponent = /* @__PURE__ */ (0, import_jsx_runtime.jsx)(MultiChoiceQuestionComponent, {
				question,
				onChanged,
				answers,
				getAnswers,
				getFeedback
			});
			break;
		case "SINGLE_CHOICE":
			questonComponent = /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SingleChoiceQuestionComponent, {
				question,
				onChanged,
				answers,
				getAnswers
			});
			break;
		case "ORDER":
			questonComponent = /* @__PURE__ */ (0, import_jsx_runtime.jsx)(OrderQuestionComponent, {
				question,
				onChanged,
				answers,
				getAnswers,
				getFeedback
			});
			break;
		default: return absurd(question);
	}
	const wrapperClassName = [
		"comp-ph-question-wrapper",
		getFeedback()?.stepsLeft === 0 && "comp-ph-question-wrapper--finished" || "",
		isFeedbackLoading && "comp-ph-question-wrapper--loading-feedback" || "",
		isQuestionFreezed && "comp-ph-question-wrapper--freezed" || ""
	].join(" ");
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: wrapperClassName,
		children: questonComponent
	});
});
//#endregion
//#region src/main/js/components/exercise/generate-sup-question.tsx
var GenerateSupQuestion = observer((props) => {
	const { violationLaw, store } = props;
	const [isModalVisible, setIsModalVisible] = (0, import_react.useState)(false);
	const [isButtonsVisible, setIsButtonsVisible] = (0, import_react.useState)(true);
	const [isAllVisible, setAllVisible] = (0, import_react.useState)(true);
	const [currentViolationLaw, setCurrentViolationLaw] = (0, import_react.useState)(violationLaw);
	const { t } = useTranslation();
	const onDetailsClicked = async () => {
		setIsButtonsVisible(false);
		setIsModalVisible(true);
		await store.generateSupplementaryQuestion(currentViolationLaw.map((v) => v.name));
		if (!store.question || store.feedback?.action === "FINISH") {
			console.log(`no need to generate sup question`);
			setAllVisible(false);
		}
	};
	const onGotitClicked = () => {
		setAllVisible(false);
	};
	const tryContinueAuto = async () => {
		if ((store.questionState == "COMPLETED" || !store.question) && store.feedback?.action === "CONTINUE_AUTO") {
			console.log(`show feedback for 3 seconds`);
			await delayPromise(3e3);
			await onNextQuestionClicked();
		}
	};
	const onAnswered = async () => {
		await store.sendAnswers();
		const { feedback } = store;
		if (!feedback) {
			console.log(`empty feedback for question asnwer`);
			setAllVisible(false);
			return;
		}
		await tryContinueAuto();
	};
	const onNextQuestionClicked = async () => {
		const newViolationLaw = store.feedback?.message?.violationLaws || null;
		if (!newViolationLaw) {
			console.log(`empty violation laws`);
			setAllVisible(false);
			return;
		}
		setCurrentViolationLaw(newViolationLaw);
		await store.generateSupplementaryQuestion(newViolationLaw.map((v) => v.name));
		await tryContinueAuto();
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
		isVisible: isAllVisible,
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
			isVisible: isButtonsVisible,
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "d-flex flex-row mt-3",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					onClick: onDetailsClicked,
					variant: "primary",
					children: t("exercise_supquestion_details")
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					onClick: onGotitClicked,
					variant: "success",
					className: "ms-2",
					children: t("exercise_supquestion_gotit")
				})]
			})
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal, {
			type: "DIALOG",
			size: "xl",
			show: isModalVisible,
			closeButton: false,
			handleClose: () => setIsModalVisible(false),
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SupQuestion, {
				store,
				onSubmitted: onAnswered,
				onNextQuestionRequested: onNextQuestionClicked
			})
		})]
	});
});
var SupQuestion = observer((props) => {
	const { store, onSubmitted, onNextQuestionRequested } = props;
	const { t } = useTranslation();
	const questionData = store.question;
	if (store.questionState === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {})
	});
	const onChanged = (newHistory) => {
		store.setAnswer(newHistory);
		if (store.questionSubmitMode === "IMPLICIT") onSubmitted?.();
	};
	const getAnswer = () => store.answer;
	const getFeedback = () => void 0;
	const showSendAnswerButton = store.questionSubmitMode === "EXPLICIT" && store.canSendQuestionAnswers;
	const showQuestionFeedback = store.questionState === "COMPLETED" && !!store.feedback && !!questionData;
	const showMessageFeedback = store.questionState === "COMPLETED" && !!store.feedback && !questionData;
	const showNextQBtn = store.feedback?.action === "CONTINUE_MANUAL" && (showQuestionFeedback || showMessageFeedback) && !!store.feedback?.message.violationLaws;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [
		questionData && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(QuestionComponent, {
			question: questionData,
			answers: store.answer,
			getAnswers: getAnswer,
			onChanged,
			getFeedback,
			isFeedbackLoading: store.isFeedbackLoading,
			isQuestionFreezed: store.isQuestionFreezed
		}),
		store.isFeedbackLoading && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "mt-2",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {})
		}),
		showSendAnswerButton && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
			variant: "primary",
			onClick: onSubmitted,
			children: t("exercise_supquestion_send_answer")
		}),
		showMessageFeedback && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: store.feedback.message.message }),
		showQuestionFeedback && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "mt-2",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ShortFeedbackAlert, { message: store.feedback.message })
		}),
		showNextQBtn && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "mt-3",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
				variant: "primary",
				onClick: onNextQuestionRequested,
				children: t("exercise_supquestion_next_question")
			})
		})
	] });
});
var ShortFeedbackAlert = observer((props) => {
	const { message } = props;
	const variant = message.type === "SUCCESS" ? "success" : "danger";
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default, {
		variant,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { dangerouslySetInnerHTML: { __html: message.message } })
	});
});
//#endregion
//#region src/main/js/components/exercise/domain-terms.tsx
function ParsedMessage({ html }) {
	const transform = (node) => {
		if (node.type === "tag" && node.name === "span" && node.attribs?.class?.includes("domain-term")) {
			const el = node;
			const explanation = el.attribs["data-explanation"];
			const child = el.children?.[0];
			let text = "";
			if (child?.type === "text") text = child.data;
			return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DomainTerm, {
				term: text,
				explanation
			});
		}
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: esm_default(html, { replace: transform }) });
}
function DomainTerm({ term, explanation }) {
	const [open, setOpen] = (0, import_react.useState)(false);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Popover, {
		open,
		onOpenChange: setOpen,
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(PopoverTrigger, {
			asChild: true,
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
				className: "compph-domain-term text-decoration-underline",
				style: { cursor: "pointer" },
				children: term
			})
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(PopoverContent, {
			className: "popover bs-popover-auto border rounded shadow-sm bg-white p-3",
			style: {
				maxWidth: "320px",
				position: "relative"
			},
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
				type: "button",
				className: "btn btn-link p-0 border-0 text-muted position-absolute",
				style: {
					top: "0.5rem",
					right: "0.5rem",
					outline: "none",
					boxShadow: "none",
					userSelect: "none"
				},
				onClick: () => setOpen(false),
				"aria-label": "Close",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(X, { size: 16 })
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mb-0 text-muted",
				style: { paddingRight: "1.5rem" },
				children: explanation
			})]
		})]
	});
}
//#endregion
//#region src/main/js/components/exercise/feedback.tsx
var Feedback = observer(({ store, showExtendedFeedback }) => {
	const { feedback, isFeedbackVisible, question } = store;
	const isFeedbackLoading = store.questionState === "ANSWER_EVALUATING";
	const isQuestionLoading = store.questionState === "LOADING";
	const { t } = useTranslation();
	if (isFeedbackLoading) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {})
	});
	if (!feedback || isQuestionLoading || !question) return null;
	const defaultFeedbackMessage = {
		type: "SUCCESS",
		message: t("issolved_feeback"),
		violationLaws: []
	};
	const feedbackMessages = feedback.messages;
	if (feedbackMessages !== null && store.questionState === "COMPLETED") feedbackMessages?.push(defaultFeedbackMessage);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "comp-ph-feedback-wrapper mt-2",
		children: isFeedbackVisible && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "mb-3",
			children: feedbackMessages?.map((m, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(FeedbackAlert, {
				message: m,
				supQuestionStore: store.supplementaryQuestion,
				showGenerateSupQuestion: showExtendedFeedback && question.options.showSupplementaryQuestions && m.type === "ERROR" && m.violationLaws?.every((e) => e.canCreateSupplementaryQuestion)
			}, i))
		}), showExtendedFeedback && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [
			feedback.grade !== null && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Badge, {
				className: "comp-ph-feedback-grade",
				bg: "primary",
				children: [
					t("grade_feeback"),
					": ",
					feedback.grade
				]
			}), " "] }),
			feedback.correctSteps !== null && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Badge, {
				bg: "success",
				children: [
					t("correctsteps_feeback"),
					": ",
					feedback.correctSteps
				]
			}), " "] }),
			!isNullOrUndefined(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Badge, {
				className: "comp-ph-feedback-error-steps",
				bg: "danger",
				children: [
					t("stepswitherrors_feeback"),
					":",
					" ",
					feedback.stepsWithErrors
				]
			}), " "] }),
			!isNullOrUndefined(feedback.stepsLeft) && feedback.stepsLeft > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Badge, {
				className: "comp-ph-feedback-remaining-steps",
				bg: "info",
				children: [
					t("stepsleft_feeback"),
					": ",
					feedback.stepsLeft
				]
			}), " "] })
		] })] })
	});
});
var FeedbackAlert = observer((props) => {
	const { supQuestionStore, message } = props;
	const showGenerateSupQuestion = props.showGenerateSupQuestion && supQuestionStore != void 0;
	const variant = message.type === "SUCCESS" ? "success" : "danger";
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Alert_default, {
		variant,
		className: variant === "danger" ? "comp-ph-feedback-error" : "comp-ph-feedback-success",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			"data-domain-laws": message.violationLaws?.map((v) => v.name).join(";"),
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ParsedMessage, { html: message.message })
		}), showGenerateSupQuestion && message.type === "ERROR" && message.violationLaws && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(GenerateSupQuestion, {
			store: supQuestionStore,
			violationLaw: message.violationLaws
		}) || null]
	});
});
//#endregion
//#region src/main/js/components/exercise/question.tsx
var Question = observer((props) => {
	const { store, showExtendedFeedback, onChanged: ParentOnChanged } = props;
	const questionData = store.question;
	if (store.questionState === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {})
	});
	if (!questionData) return null;
	const onChanged = async (newHistory) => {
		if (await store.setFullAnswer(newHistory)) ParentOnChanged?.(newHistory);
	};
	const getAnswer = () => store.lastAnswer;
	const getFeedback = () => store.feedback;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(QuestionComponent, {
		question: questionData,
		answers: store.lastAnswer,
		getAnswers: getAnswer,
		onChanged,
		getFeedback,
		isFeedbackLoading: store.questionState === "ANSWER_EVALUATING",
		isQuestionFreezed: store.isQuestionFreezed
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Feedback, {
		store,
		showExtendedFeedback
	})] });
});
//#endregion
//#region src/main/js/components/exercise/current-question.tsx
var CurrentQuestion = observer(() => {
	const exerciseStore = getExerciseStore();
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Question, {
		store: exerciseStore.currentQuestion,
		showExtendedFeedback: exerciseStore.exercise?.options.supplementaryQuestionsEnabled ?? true
	});
});
//#endregion
//#region src/main/js/components/exercise/generate-next-answer-btn.tsx
var GenerateNextAnswerBtn = observer(({ store }) => {
	const { t } = useTranslation();
	const { question, feedback } = store;
	const isFeedbackLoading = store.questionState === "ANSWER_EVALUATING";
	const isQuestionLoading = store.questionState === "LOADING";
	if (!question || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) return null;
	const onClicked = () => {
		store.generateNextCorrectAnswer();
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
		onClick: onClicked,
		className: "comp-ph-hint-btn",
		variant: "primary",
		children: t("nextCorrectAnswerBtn")
	});
});
//#endregion
//#region src/main/js/components/exercise/generate-next-question-btn.tsx
var GenerateNextQuestionBtn = observer(() => {
	const exerciseStore = getExerciseStore();
	const { t } = useTranslation();
	const [isModalVisible, setIsModalVisible] = (0, import_react.useState)(false);
	const { exercise, currentAttempt } = exerciseStore;
	const { question } = exerciseStore.currentQuestion;
	const isFeedbackLoading = exerciseStore.currentQuestion.questionState === "ANSWER_EVALUATING";
	const isQuestionLoading = exerciseStore.currentQuestion.questionState === "LOADING";
	if (!question || !exercise || !currentAttempt || isQuestionLoading || isFeedbackLoading) return null;
	const onModalClosed = () => {
		setIsModalVisible(false);
	};
	const onClicked = async () => {
		const { questionIds = [] } = currentAttempt;
		const isLastQuestion = questionIds.indexOf(question.questionId) === questionIds.length - 1;
		if (exerciseStore.currentQuestion.feedback?.stepsLeft !== 0 && isLastQuestion) setIsModalVisible(true);
		else await generateOrLoadQuestion();
	};
	const generateOrLoadQuestion = async () => {
		setIsModalVisible(false);
		const { questionIds = [] } = currentAttempt;
		const currentQuestionIdx = questionIds.indexOf(question.questionId);
		if (currentQuestionIdx === questionIds.length - 1) await exerciseStore.generateQuestion();
		else await exerciseStore.currentQuestion.loadQuestion(questionIds[currentQuestionIdx + 1]);
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
		onClick: onClicked,
		variant: "primary",
		className: "comp-ph-next-question-btn",
		children: t("generateNextQuestion_nextQuestion")
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Modal, {
		show: isModalVisible,
		title: t("generateNextQuestion_warning"),
		type: "MODAL",
		size: "lg",
		primaryBtnTitle: t("generateNextQuestion_continueAttempt"),
		handlePrimaryBtnClicked: onModalClosed,
		secondaryBtnTitle: t("generateNextQuestion_nextQuestion"),
		handleSecondaryBtnClicked: generateOrLoadQuestion,
		closeButton: false,
		handleClose: onModalClosed,
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("generateNextQuestion_modalMessage1") }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("generateNextQuestion_modalMessage2") })]
	})] });
});
//#endregion
//#region src/main/js/components/exercise/pagination.tsx
var Pagination = observer(() => {
	const exerciseStore = getExerciseStore();
	if (!exerciseStore.currentQuestion.question || !exerciseStore.currentAttempt) return null;
	const { questionId: currentQuestionId } = exerciseStore.currentQuestion.question;
	const questionIds = exerciseStore.currentAttempt.questionIds.map((id, idx) => ({
		id,
		number: idx + 1
	}));
	if (questionIds.length === 0) return null;
	const maxNumbersInRow = 10;
	const beginEndSliceSize = 7;
	const middleSliceSize = 5;
	const currentQuestionNumber = questionIds.find((v) => v.id === currentQuestionId)?.number ?? -1;
	const currentQuestionPosition = currentQuestionNumber - middleSliceSize <= 0 ? "BEGIN" : currentQuestionNumber + middleSliceSize > questionIds.length ? "END" : "MIDDLE";
	const offset = Math.floor(middleSliceSize / 2);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "d-flex justify-content-center",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Pagination_default, {
			className: "p-3",
			style: { marginBottom: "0 !important" },
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
				isVisible: questionIds.length <= maxNumbersInRow,
				children: questionIds.map((id) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
					active: currentQuestionId === id.id,
					onClick: () => exerciseStore.currentQuestion.loadQuestion(id.id),
					children: id.number
				}, id.number))
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
				isVisible: questionIds.length > maxNumbersInRow,
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.First, {
						disabled: currentQuestionId === questionIds[0].id,
						onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id)
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Prev, {
						disabled: currentQuestionId === questionIds[0].id,
						onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.findIndex((x) => currentQuestionId === x.id) - 1].id)
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
						isVisible: currentQuestionPosition === "BEGIN",
						children: [
							questionIds.filter((id) => id.number <= beginEndSliceSize).map((id) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: currentQuestionId === id.id,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(id.id),
								children: id.number
							}, id.number)),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Ellipsis, { disabled: true }),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: false,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id),
								children: questionIds[questionIds.length - 1].number
							}, questionIds[questionIds.length - 1].number)
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
						isVisible: currentQuestionPosition === "MIDDLE",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: false,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id),
								children: questionIds[0].number
							}, questionIds[0].number),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Ellipsis, { disabled: true }),
							questionIds.filter((id) => id.number >= currentQuestionNumber - offset && id.number <= currentQuestionNumber + offset).map((id) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: currentQuestionId === id.id,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(id.id),
								children: id.number
							}, id.number)),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Ellipsis, { disabled: true }),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: false,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id),
								children: questionIds[questionIds.length - 1].number
							}, questionIds[questionIds.length - 1].number)
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
						isVisible: currentQuestionPosition === "END",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: false,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id),
								children: questionIds[0].number
							}, questionIds[0].number),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Ellipsis, { disabled: true }),
							questionIds.filter((id) => id.number > questionIds.length - beginEndSliceSize).map((id) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Item, {
								active: currentQuestionId === id.id,
								onClick: () => exerciseStore.currentQuestion.loadQuestion(id.id),
								children: id.number
							}, id.number))
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Next, {
						disabled: currentQuestionId === questionIds[questionIds.length - 1].id,
						onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.findIndex((x) => currentQuestionId === x.id) + 1].id)
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination_default.Last, {
						disabled: currentQuestionId === questionIds[questionIds.length - 1].id,
						onClick: () => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id)
					})
				]
			})]
		})
	});
});
//#endregion
//#region src/main/js/components/common/header.tsx
var Header = observer((props) => {
	const { text, pagination, languageHint, language, onLanguageClicked, userHint, user, onUserClicked, userHref, logoutLabel } = props;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Navbar_default, {
		className: "px-0",
		children: [text && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("h5", { children: text }) || null, /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Navbar_default.Collapse, {
			className: "justify-content-end",
			children: [
				pagination,
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Navbar_default.Text, {
					className: "px-2",
					children: [
						languageHint,
						": ",
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
							href: "#",
							onClick: onLanguageClicked ?? void 0,
							children: language
						})
					]
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Navbar_default.Toggle, {}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Navbar_default.Text, {
					className: "px-2",
					children: [
						userHint,
						": ",
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
							href: userHref ?? "#",
							onClick: onUserClicked ?? void 0,
							children: user
						})
					]
				}),
				logoutLabel && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Navbar_default.Text, {
					className: "px-2",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
						href: "/logout",
						children: logoutLabel
					})
				})
			]
		})]
	});
});
//#endregion
//#region src/main/js/stores/session-store.ts
var SessionStore = class {
	user = void 0;
	languages = [];
	isSessionLoading = false;
	error = null;
	get selectedLanguage() {
		return this.user?.language;
	}
	get isSessionLoaded() {
		return this.user !== void 0;
	}
	constructor() {
		makeAutoObservable(this);
	}
	loadSessionInfo = async () => {
		if (this.isSessionLoading) return;
		this.isSessionLoading = true;
		this.error = null;
		const [user, languages] = await Promise.all([userController.getCurrentUser(), userController.getLanguages()]);
		this.isSessionLoading = false;
		if (isLeft(user)) {
			this.error = user.left;
			return;
		}
		if (isLeft(languages)) {
			this.error = languages.left;
			return;
		}
		this.user = user.right;
		this.languages = languages.right;
		if (this.user.language !== instance.language) instance.changeLanguage(this.user.language);
	};
	changeLanguage = async (newLang) => {
		if (!this.user || this.user.language === newLang) return;
		const res = await userController.setLanguage(newLang);
		if (isLeft(res)) {
			console.error("Failed to change language", res.left);
			return;
		}
		this.user.language = res.right;
		instance.changeLanguage(res.right);
	};
};
//#endregion
//#region src/main/js/hooks/session-context.tsx
var SessionContext = (0, import_react.createContext)(null);
var SessionProvider = observer(({ children }) => {
	const [session] = (0, import_react.useState)(() => new SessionStore());
	(0, import_react.useEffect)(() => {
		session.loadSessionInfo();
	}, [session]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SessionContext.Provider, {
		value: session,
		children: session.error && !session.user ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "container pt-3",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadFailure, {
				error: session.error,
				onRetry: () => session.loadSessionInfo()
			})
		}) : children
	});
});
var useSession = () => {
	const session = (0, import_react.useContext)(SessionContext);
	if (!session) throw new Error("useSession must be used within a <SessionProvider>");
	return session;
};
var useCurrentUser = () => {
	const session = useSession();
	if (!session) throw new Error("useCurrentUser must be used within a <SessionProvider>");
	return session.user;
};
//#endregion
//#region src/main/js/components/exercise/header.tsx
var ExerciseHeader = observer(() => {
	const exerciseStore = getExerciseStore();
	const { t } = useTranslation();
	const user = useCurrentUser();
	const { currentAttempt, exercise, currentQuestion } = exerciseStore;
	if (!currentAttempt || !exercise || !user) return null;
	const currentQuestionIdx = currentAttempt.questionIds.findIndex((id) => currentQuestion.question?.questionId === id);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
		text: currentQuestionIdx !== -1 ? t("question_header", { questionNumber: currentQuestionIdx + 1 }) : "",
		pagination: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Pagination, {}),
		languageHint: t("language_header"),
		language: user.language,
		userHint: t("signedin_as_header"),
		user: user.displayName,
		onLanguageClicked: null,
		logoutLabel: t("logout_header")
	});
});
//#endregion
//#region src/main/js/components/exercise/survey.tsx
var SurveyComponent = (props) => {
	const { survey, enabledSurveyQuestions, isCompleted } = props;
	const [surveyState, setSurveyState] = (0, import_react.useState)(isCompleted ? "COMPLETED" : "INITAL");
	const [surveyAnswers, setSurveyAnswers] = (0, import_react.useState)(props.value || {});
	const { t } = useTranslation();
	const surveyQuestions = props.survey.questions.filter((q) => enabledSurveyQuestions.includes(q.id));
	const onAnswered = (questionId, answer) => {
		const newAnswers = {
			...surveyAnswers,
			[questionId]: answer
		};
		setSurveyAnswers(newAnswers);
		console.log(newAnswers);
	};
	const sendAnswers = () => {
		(async () => {
			if (surveyQuestions.filter((x) => x.required).map((x) => x.id).every((id) => surveyAnswers[id])) {
				setSurveyState("SENDING_RESULTS");
				await Promise.all(surveyQuestions.map((q) => surveyController.postSurveyAnswer(q.id, props.questionId, surveyAnswers[q.id])));
				setSurveyState("COMPLETED");
				props.onAnswersSended(survey, props.questionId, surveyAnswers);
			} else setSurveyState("VALIDATION_ERROR");
		})();
	};
	if (!surveyQuestions || !surveyQuestions.length) return null;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "alert alert-warning rounded",
		role: "alert",
		children: [
			surveyQuestions.map((q, idx, qs) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: idx !== qs.length - 1 && "mb-4" || "",
				children: q.type === "yes-no" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SurveyYesNoQuestion, {
					isCompleted,
					question: q,
					onAnswered,
					value: surveyAnswers[q.id]
				}) || q.type === "single-choice" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SingleChoiceSurveyQuestionComponent, {
					isCompleted,
					question: q,
					onAnswered,
					value: surveyAnswers[q.id]
				}) || q.type === "open-ended" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(OpenEndedSurveyQuestionComponent, {
					isCompleted,
					question: q,
					onAnswered,
					value: surveyAnswers[q.id]
				}) || "invalid question type"
			}, idx)),
			surveyState === "SENDING_RESULTS" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mt-2",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, { delay: 100 })
			}),
			surveyState === "VALIDATION_ERROR" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mt-2",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "alert alert-danger rounded",
					children: "Необходимо ответить на все обязательные вопросы"
				})
			}),
			surveyState !== "COMPLETED" && surveyState !== "SENDING_RESULTS" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mt-2",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					onClick: sendAnswers,
					children: t("survey_sendresults")
				})
			})
		]
	});
};
var SurveyYesNoQuestion = (props) => {
	const { question, onAnswered, value } = props;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mb-1",
		children: question.text
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "d-flex flex-row mt-2",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
			variant: "secondary",
			className: "me-2",
			active: question.options.yesValue === value,
			onClick: () => onAnswered(question.id, question.options.yesValue),
			disabled: value !== void 0,
			children: question.options.yesText
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
			variant: "secondary",
			active: question.options.noValue === value,
			onClick: () => onAnswered(question.id, question.options.noValue),
			disabled: value !== void 0,
			children: question.options.noText
		})]
	})] });
};
var SingleChoiceSurveyQuestionComponent = (props) => {
	const { question, onAnswered, value, isCompleted } = props;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mb-1",
		children: question.text
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "d-flex mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: question.options.map((o) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
			disabled: isCompleted,
			checked: value === o.id,
			name: `radio_qid${question.id}`,
			type: "radio",
			id: `radio_qid${question.id}_sqid${o.id}`,
			label: o.text,
			value: o.id,
			onChange: (e) => onAnswered(question.id, e.target.value)
		}) }, o.id)) })
	})] });
};
var OpenEndedSurveyQuestionComponent = (props) => {
	const { question, onAnswered, value, isCompleted } = props;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mb-1",
		children: question.text
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "d-flex flex-row mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Control, {
			disabled: isCompleted,
			value,
			as: "textarea",
			rows: 3,
			onChange: (e) => onAnswered(question.id, e.target.value)
		})
	})] });
};
//#endregion
//#region src/main/js/data/introduction-tour.ts
var steps = [
	{
		id: "tour-exprs-intro",
		title: "tour_exprs_intro_title",
		text: "tour_exprs_intro",
		attachTo: {
			element: ".comp-ph-exercise-body",
			on: "auto"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-pages",
		title: "tour_exprs_pages_title",
		text: "tour_exprs_pages",
		attachTo: {
			element: ".pagination",
			on: "top"
		},
		highlightClass: "comp-ph-next-question-btn",
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-expr",
		title: "tour_exprs_expr_title",
		text: "tour_exprs_expr",
		attachTo: {
			element: ".comp-ph-expr",
			on: "bottom"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-operator",
		title: "tour_exprs_operator_title",
		text: "tour_exprs_operator",
		attachTo: {
			element: ".comp-ph-expr-op-btn",
			on: "bottom"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-selectedop",
		title: "tour_exprs_selectedop_title",
		text: "tour_exprs_selectedop",
		attachTo: {
			element: ".comp-ph-expr-op-btn.disabled",
			on: "right"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-hint",
		title: "tour_exprs_hint_title",
		text: "tour_exprs_hint",
		attachTo: {
			element: ".comp-ph-hint-btn",
			on: "bottom"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-expr-error-hint",
		title: "tour_exprs_error_hint_title",
		text: "tour_exprs_error_hint",
		attachTo: {
			element: ".comp-ph-feedback-error",
			on: "top"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-expr-feedback-grade",
		title: "tour_exprs_feedback_grade_title",
		text: "tour_exprs_feedback_grade",
		attachTo: {
			element: ".comp-ph-feedback-grade",
			on: "top"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-feedback-steps",
		title: "tour_exprs_feedback_steps_title",
		text: "tour_exprs_feedback_steps",
		attachTo: {
			element: ".comp-ph-feedback-remaining-steps",
			on: "top"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-feedback-errors",
		title: "tour_exprs_feedback_errors_title",
		text: "tour_exprs_feedback_errors",
		attachTo: {
			element: ".comp-ph-feedback-error-steps",
			on: "top"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-earlyfinish",
		title: "tour_exprs_earlyfinish_title",
		text: "tour_exprs_earlyfinish",
		attachTo: {
			element: ".comp-ph-complete-btn",
			on: "top"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	},
	{
		id: "tour-exprs-tracevalue",
		title: "tour_exprs_tracevalue_title",
		text: "tour_exprs_tracevalue",
		attachTo: {
			element: ".comp-ph-trace-value",
			on: "right"
		},
		buttons: [{ text: "skip" }, { text: "next" }]
	}
];
//#endregion
//#region src/main/js/utils/TourProvider.tsx
var TourContext = (0, import_react.createContext)(null);
var TourProvider = ({ steps, children }) => {
	const tourRef = (0, import_react.useRef)(void 0);
	const [isReady, setIsReady] = (0, import_react.useState)(false);
	const isTourCompletedRef = (0, import_react.useRef)(false);
	const { t } = useTranslation();
	const bindActions = (tour, steps) => {
		return steps.map((step) => {
			const completeTour = () => {
				tour.complete();
				isTourCompletedRef.current = true;
				if (localStorage.getItem("tour_completed") !== "never") localStorage.setItem("tour_completed", "true");
			};
			const buttons = step.buttons?.map((button) => {
				const text = typeof button.text === "function" ? button.text() : button.text || "";
				if (text === "next") return {
					...button,
					text: "tour_next",
					action: () => tour.next(),
					classes: "shepherd-button-primary"
				};
				if (text === "skip") return {
					...button,
					text: "tour_skip",
					action: () => completeTour(),
					classes: "shepherd-button-secondary"
				};
				if (text === "complete") return {
					...button,
					text: "tour_complete",
					action: () => completeTour(),
					classes: "shepherd-button-success"
				};
				return button;
			});
			return {
				...step,
				buttons
			};
		});
	};
	(0, import_react.useEffect)(() => {
		const tour = new pe.Tour({
			defaultStepOptions: {
				scrollTo: true,
				cancelIcon: { enabled: false }
			},
			useModalOverlay: true
		});
		tourRef.current = tour;
		tour.start = async () => {
			await tour.cancel();
			tour.steps.forEach((step) => step.destroy());
			const pendingSteps = [...bindActions(tour, steps)];
			const showNextStep = async () => {
				if (isTourCompletedRef.current) return;
				if (pendingSteps.length === 0) return;
				for (let i = 0; i < pendingSteps.length; i++) {
					const step = Object.assign({}, pendingSteps[i]);
					step.title = t(step.title);
					step.text = t(step.text);
					step.buttons = step.buttons?.map((button) => ({
						...button,
						text: t(button.text)
					}));
					const selector = typeof step.attachTo === "object" ? step.attachTo?.element : null;
					if (typeof selector === "string") {
						if (document.querySelector(selector)) {
							const completedSteps = new Set(JSON.parse(localStorage.getItem("tour_completed_steps") ?? "[]"));
							if (localStorage.getItem("tour_completed") !== "never" && !step.id?.endsWith("-always")) {
								if (completedSteps.has(step.id)) continue;
								completedSteps.add(step.id);
								localStorage.setItem("tour_completed_steps", JSON.stringify(Array.from(completedSteps)));
							}
							tour.addStep(step);
							tour.show(step.id);
							pendingSteps.splice(i, 1);
							await new Promise((resolve) => {
								const cleanup = () => {
									tour.off("next", cleanup);
									tour.off("cancel", cleanup);
									tour.off("complete", cleanup);
									resolve();
								};
								tour.on("next", cleanup);
								tour.on("cancel", cleanup);
								tour.on("complete", cleanup);
							});
							return showNextStep();
						}
					}
				}
				if (!isTourCompletedRef.current) setTimeout(() => showNextStep(), 500);
			};
			await showNextStep();
		};
		setIsReady(true);
	}, [steps, t]);
	const start = (0, import_react.useCallback)(() => tourRef.current?.start?.(), []);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(TourContext.Provider, {
		value: {
			start,
			tour: tourRef.current,
			isReady
		},
		children
	});
};
var useTour = () => {
	const ctx = (0, import_react.useContext)(TourContext);
	if (!ctx) throw new Error("useTour must be used within TourProvider");
	return ctx;
};
//#endregion
//#region src/main/js/utils/TourLauncher.tsx
var TourLauncher = () => {
	const { start, isReady } = useTour();
	(0, import_react.useEffect)(() => {
		if (!isReady) return;
		if (localStorage.getItem("tour_completed") !== "true") start();
	}, [isReady, start]);
	return null;
};
//#endregion
//#region src/main/js/pages/exercise.tsx
var Exercise = observer(() => {
	const exerciseStore = getExerciseStore();
	const { exerciseState, setExerciseState, storeState: excerciseStoreState, currentQuestion, survey } = exerciseStore;
	const { storeState: currentQuestionStoreState } = currentQuestion;
	const { t } = useTranslation();
	(0, import_react.useEffect)(() => {
		(async () => {
			if (exerciseState === "LAUNCH_ERROR") return;
			if (exerciseStore.currentQuestion.question) {
				setExerciseState("EXERCISE");
				return;
			}
			await exerciseStore.loadExercise();
			if (exerciseStore.isDebug) {
				setExerciseState("EXERCISE");
				createDebugAttemptAndLoadQuestion();
				return;
			}
			const attemptId = exerciseStore.currentAttemptId;
			if (attemptId && Number.isInteger(attemptId)) {
				setExerciseState("EXERCISE");
				getAttemptAndLoadQuestion(+attemptId);
				return;
			}
			if (exerciseStore.exercise?.options.forceNewAttemptCreationEnabled || !await exerciseStore.loadExistingExerciseAttempt()) {
				setExerciseState("EXERCISE");
				createAttemptAndLoadQuestion();
				return;
			}
			setExerciseState("MODAL");
		})();
	}, []);
	const loadQuestion = (0, import_react.useCallback)(() => {
		(async () => {
			if (exerciseStore.currentAttempt?.questionIds.length) {
				const len = exerciseStore.currentAttempt?.questionIds.length;
				await exerciseStore.currentQuestion.loadQuestion(exerciseStore.currentAttempt?.questionIds[len - 1]);
			} else await exerciseStore.generateQuestion();
		})();
	}, [exerciseStore]);
	const createAttemptAndLoadQuestion = (0, import_react.useCallback)(() => {
		(async () => {
			exerciseStore.currentQuestion.setQuestionState("LOADING");
			await exerciseStore.createExerciseAttempt();
			loadQuestion();
		})();
	}, [exerciseStore, loadQuestion]);
	const createDebugAttemptAndLoadQuestion = (0, import_react.useCallback)(() => {
		(async () => {
			exerciseStore.currentQuestion.setQuestionState("LOADING");
			await exerciseStore.createDebugExerciseAttempt();
			loadQuestion();
		})();
	}, [exerciseStore, loadQuestion]);
	const getAttemptAndLoadQuestion = (0, import_react.useCallback)((attemptId) => {
		(async () => {
			exerciseStore.currentQuestion.setQuestionState("LOADING");
			await exerciseStore.loadExerciseAttempt(attemptId);
			loadQuestion();
		})();
	}, [exerciseStore, loadQuestion]);
	const onSurveyAnswered = (0, import_react.useCallback)((survey, questionId, answers) => {
		exerciseStore.setSurveyAnswers(questionId, answers);
	}, [exerciseStore]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(TourProvider, {
		steps,
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(TourLauncher, {}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: `compph-exercise${exerciseStore.isDebug ? " compph-exercise--debug" : ""}`,
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(LoadingWrapper, {
				isLoading: exerciseStore.isExerciseLoading === true || exerciseState === "INITIAL",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
					isVisible: exerciseState === "EXERCISE" || exerciseState === "COMPLETED",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseHeader, {}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mt-5 position-relative comp-ph-exercise-body",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(CurrentQuestion, {}),
							survey != null && (exerciseStore.currentQuestion.questionState === "COMPLETED" || exerciseState === "COMPLETED") && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "mt-2",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SurveyComponent, {
									questionId: exerciseStore.currentQuestion.question?.questionId ?? -1,
									survey: survey.survey,
									enabledSurveyQuestions: exerciseStore.ensureQuestionSurveyExists(currentQuestion.question?.questionId ?? -1),
									value: survey.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.results,
									onAnswersSended: onSurveyAnswered,
									isCompleted: survey.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.status === "COMPLETED"
								})
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
								isVisible: exerciseState === "EXERCISE",
								children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
									isVisible: exerciseStore.currentQuestion.questionState === "LOADED",
									children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
										className: "mt-3",
										children: exerciseStore.exercise?.options.correctAnswerGenerationEnabled && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(GenerateNextAnswerBtn, { store: exerciseStore.currentQuestion })
									})
								}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
									isVisible: survey == null && (exerciseStore.exercise?.options.newQuestionGenerationEnabled || exerciseStore.currentQuestion.questionState === "COMPLETED") || survey != null && survey.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.status === "COMPLETED",
									children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
										className: "mt-2",
										children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(GenerateNextQuestionBtn, {})
									})
								})]
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
								isVisible: exerciseState === "COMPLETED",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
									className: "mt-3",
									children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default, {
										variant: "success",
										children: t("exercise_completed")
									})
								})
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
								isVisible: (exerciseStore.exercise?.options.debugButtonEnabled ?? false) && currentQuestion.question !== void 0,
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DebugButton, {
									metadataId: currentQuestion.question?.questionMetadataId ?? -1,
									attemptId: exerciseStore.currentAttempt?.attemptId
								})
							})
						]
					})]
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
					isVisible: exerciseState === "MODAL",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal, {
						type: "DIALOG",
						title: t("foundExisitingAttempt_title"),
						primaryBtnTitle: t("foundExisitingAttempt_continueattempt"),
						handlePrimaryBtnClicked: () => {
							setExerciseState("EXERCISE");
							loadQuestion();
						},
						secondaryBtnTitle: t("foundExisitingAttempt_newattempt"),
						handleSecondaryBtnClicked: () => {
							setExerciseState("EXERCISE");
							createAttemptAndLoadQuestion();
						},
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", { children: [t("foundExisitingAttempt_descr"), "?"] })
					})
				})]
			}), [excerciseStoreState, currentQuestionStoreState].filter((x) => x.tag === "ERROR").map((x) => x.tag === "ERROR" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mt-2",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(InlineError, { error: x.error })
			}))]
		})]
	});
});
//#endregion
//#region src/main/js/pages/statistics.tsx
var Statistics = () => {
	const [statistics, setStatistics] = (0, import_react.useState)([]);
	const [isLoading, setIsLoading] = (0, import_react.useState)(false);
	(0, import_react.useEffect)(() => {
		(async () => {
			const exerciseId = new URLSearchParams(window.location.search).get("exerciseId");
			if (exerciseId === null || Number.isNaN(+exerciseId)) throw new Error("Invalid exerciseId url param");
			const controller = exerciseController;
			setIsLoading(true);
			const statistics = await controller.getExerciseStatistics(+exerciseId);
			if (isRight(statistics)) setStatistics(statistics.right);
			setIsLoading(false);
		})();
	}, []);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Table, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("thead", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("tr", { children: [
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
			scope: "col",
			children: "AttemptId"
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
			scope: "col",
			children: "QuestionsCount"
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
			scope: "col",
			children: "TotalInteractionsCount"
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
			scope: "col",
			children: "TotalInteractionsWithErrorsCount"
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
			scope: "col",
			children: "AverageGrade"
		})
	] }) }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("tbody", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadingWrapper, {
		isLoading,
		children: statistics.map((s, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("tr", { children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("th", {
				scope: "row",
				children: s.attemptId
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("td", { children: s.questionsCount }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("td", { children: s.totalInteractionsCount }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("td", { children: s.totalInteractionsWithErrorsCount }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("td", { children: s.averageGrade })
		] }, idx))
	}) })] }) });
};
//#endregion
//#region src/main/js/pages/exercises-list.tsx
var ExercisesList = observer(() => {
	const [data, setData] = (0, import_react.useState)([]);
	const [isLoading, setIsLoading] = (0, import_react.useState)(false);
	(0, import_react.useEffect)(() => {
		(async () => {
			setIsLoading(true);
			const dataEither = await exerciseController.getExercises();
			if (isRight(dataEither)) setData(dataEither.right);
			setIsLoading(false);
		})();
	}, []);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadingWrapper, {
		isLoading,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ListGroup_default, { children: data.map((i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ListGroup_default.Item, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
			href: `exercise?exerciseId=${i}`,
			children: i
		}) }, i)) })
	}) });
});
//#endregion
//#region src/main/js/pages/survey.tsx
var SurveyPage = observer(() => {
	const exerciseStore = getExerciseStore();
	const { exerciseState, setExerciseState, storeState: excerciseStoreState, currentQuestion } = exerciseStore;
	const { storeState: currentQuestionStoreState } = currentQuestion;
	const [surveyState] = (0, import_react.useState)("ACTIVE");
	(0, import_react.useEffect)(() => {
		(async () => {
			if (exerciseState === "LAUNCH_ERROR") return;
			if (exerciseStore.currentQuestion.question) {
				setExerciseState("EXERCISE");
				return;
			}
			await exerciseStore.loadExercise();
			setExerciseState("EXERCISE");
			await createAttemptAndLoadQuestion();
		})();
	}, []);
	const loadQuestion = async () => {
		if (exerciseStore.currentAttempt?.questionIds.length) {
			const len = exerciseStore.currentAttempt?.questionIds.length;
			await exerciseStore.currentQuestion.loadQuestion(exerciseStore.currentAttempt?.questionIds[len - 1]);
		} else await exerciseStore.generateQuestion();
	};
	const createAttemptAndLoadQuestion = async () => {
		exerciseStore.currentQuestion.setQuestionState("LOADING");
		await exerciseStore.createExerciseAttempt();
		await loadQuestion();
	};
	const onSurveyAnswered = (0, import_react.useCallback)((_survey, _questionId, _answers) => {
		console.log("лень рефакторить, не работает крч");
	}, []);
	const surveyOptions = exerciseStore.exercise?.options.surveyOptions;
	const currentQuestionId = exerciseStore.currentQuestion.question?.questionId ?? -1;
	if (!surveyOptions?.enabled) return null;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadingWrapper, {
		isLoading: exerciseStore.isExerciseLoading === true || exerciseState === "INITIAL",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Optional, {
			isVisible: exerciseState === "EXERCISE" || exerciseState === "COMPLETED",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseHeader, {}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mt-5",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						style: { pointerEvents: "none" },
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(CurrentQuestion, {})
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
						isVisible: surveyState === "COMPLETED",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "mt-2",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default, {
								variant: "success",
								children: "Спасибо за участие в опросе!"
							})
						})
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Optional, {
						isVisible: surveyState !== "COMPLETED" && currentQuestion.questionState === "LOADED",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "mt-2",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SurveyComponent, {
								questionId: currentQuestionId,
								survey: exerciseStore.survey.survey,
								value: exerciseStore.survey?.questions[currentQuestionId].results,
								enabledSurveyQuestions: exerciseStore.survey?.questions[currentQuestionId].questions ?? [],
								onAnswersSended: onSurveyAnswered
							})
						})
					})
				]
			})]
		})
	}), [excerciseStoreState, currentQuestionStoreState].filter((x) => x.tag === "ERROR").map((x) => x.tag === "ERROR" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "mt-2",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(InlineError, { error: x.error })
	}))] });
});
//#endregion
//#region src/main/js/stores/exercise-settings-store.ts
var ExerciseStageStore = class {
	courseId;
	card;
	concepts;
	laws;
	skills;
	numberOfQuestions;
	bankLoadingState = "NOT_STARTED";
	bankSearchResult = {
		questions: [],
		count: 0,
		topRatedCount: 0
	};
	complexity = .5;
	autorunner;
	abortController = null;
	constructor(courseId, card, stage) {
		this.courseId = courseId;
		this.concepts = stage.concepts;
		this.laws = stage.laws;
		this.skills = stage.skills;
		this.numberOfQuestions = stage.numberOfQuestions;
		this.complexity = stage.complexity;
		this.card = card;
		makeAutoObservable(this);
		this.autorunner = autorun(() => {
			const complexity = this.complexity;
			const laws = this.laws.slice();
			const concepts = this.concepts.slice();
			const skills = this.skills.slice();
			const tags = card.tags.slice();
			untracked(() => this.updateBankStats(concepts, laws, skills, tags, complexity));
		}, { delay: 1e3 });
	}
	async updateBankStats(concepts, laws, skills, tags, complexity) {
		const { card } = this;
		if (this.abortController) {
			this.abortController.abort();
			this.abortController = null;
		}
		const currentAbortController = new AbortController();
		this.abortController = currentAbortController;
		this.bankLoadingState = "IN_PROGRESS";
		const newData = await exerciseSettingsController.search(card.domainId, concepts, laws, skills, tags, complexity, 5, this.courseId, currentAbortController.signal);
		if (isRight(newData)) {
			this.bankSearchResult = newData.right;
			this.bankLoadingState = "COMPLETED";
		}
		if (this.abortController === currentAbortController) this.abortController = null;
	}
	[Symbol.dispose]() {
		if (this.autorunner) this.autorunner();
		if (this.abortController) {
			this.abortController.abort();
			this.abortController = null;
		}
	}
};
var ExerciseSettingsStore = class {
	exercisesLoadStatus = "NONE";
	exercises = null;
	permissions = noExerciseListPermissions;
	domains = null;
	backends = null;
	strategies = null;
	currentCard = null;
	courseId = null;
	storeState = { tag: "VALID" };
	loadToken = 0;
	constructor() {
		makeAutoObservable(this);
	}
	applyExerciseList(list) {
		this.exercises = list.exercises;
		this.permissions = list.permissions;
	}
	get cardLinkType() {
		const card = this.currentCard;
		if (!card) return "global";
		if (this.courseId == null) return card.isPublic ? "global" : "original";
		return card.isPublic ? "inherited" : "original";
	}
	toCardViewModel(card) {
		const cardDomain = this.domains?.find((x) => x.id === card.domainId);
		if (!cardDomain) throw new Error(`The exercise is bound to domain '${card.domainId}', which the server does not list`);
		const result = observable({
			...card,
			tags: card.tags.filter((t) => cardDomain.tags.some((tt) => tt === t)),
			stages: []
		});
		result.stages = pipe(card.stages, map((stage) => new ExerciseStageStore(this.courseId, result, stage)));
		return result;
	}
	fromCardViewModel(card) {
		return {
			...card,
			stages: pipe(card.stages, map((stage) => ({
				concepts: stage.concepts,
				laws: stage.laws,
				skills: stage.skills,
				numberOfQuestions: stage.numberOfQuestions,
				complexity: stage.complexity
			})))
		};
	}
	async loadExercises(courseId = null) {
		if (this.exercisesLoadStatus === "LOADED" || this.exercisesLoadStatus === "LOADING") return;
		this.storeState = { tag: "VALID" };
		this.exercisesLoadStatus = "LOADING";
		this.courseId = courseId;
		const [rawExercises, domains, backends, strategies] = await Promise.all([
			exerciseSettingsController.listExercises(courseId),
			exerciseSettingsController.getDomains(),
			exerciseSettingsController.getBackends(),
			exerciseSettingsController.getStrategies()
		]);
		const failed = [
			rawExercises,
			domains,
			backends,
			strategies
		].find(isLeft);
		if (failed) {
			this.storeState = {
				tag: "ERROR",
				error: failed.left
			};
			this.exercisesLoadStatus = "NONE";
			return;
		}
		this.applyExerciseList(rawExercises.right);
		this.domains = domains.right;
		this.backends = backends.right;
		this.strategies = strategies.right;
		this.exercisesLoadStatus = "LOADED";
	}
	async loadExercise(exerciseId) {
		if (this.exercisesLoadStatus === "NONE" || this.exercisesLoadStatus === "LOADING") throw new Error("Exercises must be loaded first");
		const token = ++this.loadToken;
		this.storeState = { tag: "VALID" };
		this.exercisesLoadStatus = "EXERCISELOADING";
		try {
			const rawExercise = await exerciseSettingsController.getExercise(exerciseId, this.courseId);
			if (token !== this.loadToken) return;
			if (isLeft(rawExercise)) {
				this.storeState = {
					tag: "ERROR",
					error: rawExercise.left
				};
				return;
			}
			this.currentCard = this.toCardViewModel(rawExercise.right);
		} catch (error) {
			this.currentCard = null;
			this.storeState = {
				tag: "ERROR",
				error: { message: error instanceof Error ? error.message : String(error) }
			};
		} finally {
			if (token === this.loadToken) this.exercisesLoadStatus = "LOADED";
		}
	}
	async createNewExecise() {
		if (this.exercisesLoadStatus !== "LOADED") throw new Error("Exercises must be loaded first");
		const newExerciseId = await exerciseSettingsController.createExercise("(empty)", this.domains[0].id, this.strategies[0].id, this.courseId);
		if (!isRight(newExerciseId)) return;
		this.exercisesLoadStatus = "EXERCISELOADING";
		const [rawExercise, newExercisesList] = await Promise.all([exerciseSettingsController.getExercise(newExerciseId.right, this.courseId), exerciseSettingsController.listExercises(this.courseId)]);
		if (isRight(rawExercise) && isRight(newExercisesList)) {
			this.currentCard = this.toCardViewModel(rawExercise.right);
			this.applyExerciseList(newExercisesList.right);
		}
		this.exercisesLoadStatus = "LOADED";
	}
	async cloneCurrentToCourse(targetCourseId) {
		if (!this.currentCard) return;
		const result = await exerciseSettingsController.cloneExercise(this.currentCard.id, targetCourseId);
		if (!isRight(result)) return;
		const newId = result.right;
		const [rawExercise, newExercisesList] = await Promise.all([exerciseSettingsController.getExercise(newId, this.courseId), exerciseSettingsController.listExercises(this.courseId)]);
		if (isRight(rawExercise) && isRight(newExercisesList)) {
			this.currentCard = this.toCardViewModel(rawExercise.right);
			this.applyExerciseList(newExercisesList.right);
		}
	}
	async copyCurrentToPool() {
		if (!this.currentCard) return null;
		const result = await exerciseSettingsController.cloneExercise(this.currentCard.id, null);
		return isRight(result) ? result.right : null;
	}
	async unlinkFromCourse(courseId) {
		if (!this.currentCard) return;
		await courseController.removeExerciseFromCourse(this.currentCard.id, courseId);
		const refreshed = await exerciseSettingsController.listExercises(this.courseId);
		if (isRight(refreshed)) {
			this.applyExerciseList(refreshed.right);
			this.currentCard = null;
		}
	}
	async deleteCurrentExercise() {
		if (!this.currentCard) return;
		const id = this.currentCard.id;
		await exerciseSettingsController.deleteExercise(id, this.courseId);
		const refreshed = await exerciseSettingsController.listExercises(this.courseId);
		if (isRight(refreshed)) {
			this.applyExerciseList(refreshed.right);
			this.currentCard = null;
		}
	}
	async saveCard() {
		if (!this.currentCard) return;
		this.exercisesLoadStatus = "EXERCISELOADING";
		await exerciseSettingsController.saveExercise(this.fromCardViewModel(this.currentCard), this.courseId);
		const newExercisesList = await exerciseSettingsController.listExercises(this.courseId);
		if (isRight(newExercisesList)) this.applyExerciseList(newExercisesList.right);
		this.exercisesLoadStatus = "LOADED";
	}
	setCardName(name) {
		if (!this.currentCard) return;
		this.currentCard.name = name;
	}
	setCardDomain(domainId) {
		if (!this.currentCard) return;
		if (domainId !== this.currentCard.domainId) {
			this.currentCard.stages[0].laws = [];
			this.currentCard.stages[0].concepts = [];
			this.currentCard.stages.splice(1);
			this.currentCard.domainId = domainId;
		}
	}
	setCardStrategy(strategyId) {
		if (!this.currentCard) return;
		if (this.currentCard.strategyId !== strategyId) {
			this.currentCard.stages[0].laws = [];
			this.currentCard.stages[0].concepts = [];
			this.currentCard.stages.splice(1);
			this.currentCard.strategyId = strategyId;
		}
	}
	setCardStageComplexity(stageIdx, rawComplexity) {
		if (!this.currentCard || !this.currentCard.stages[stageIdx]) return;
		const stage = this.currentCard.stages[stageIdx];
		stage.complexity = Number.parseInt(rawComplexity) / 100;
	}
	setCardCommonConceptValue(conceptName, conceptValue) {
		if (!this.currentCard) return;
		for (const stage of this.currentCard.stages) {
			const targetConceptIdx = stage.concepts.findIndex((x) => x.name == conceptName);
			let targetConcept = targetConceptIdx !== -1 ? stage.concepts[targetConceptIdx] : null;
			if (conceptValue === "PERMITTED") {
				if (targetConcept) stage.concepts.splice(targetConceptIdx, 1);
				continue;
			}
			if (!targetConcept) {
				targetConcept = {
					name: conceptName,
					kind: conceptValue
				};
				stage.concepts = [...stage.concepts, targetConcept];
			} else stage.concepts[targetConceptIdx] = {
				...targetConcept,
				kind: conceptValue
			};
		}
	}
	setCardStageConceptValue(stageIdx, conceptName, conceptValue) {
		if (!this.currentCard || !this.currentCard.stages[stageIdx]) return;
		const stage = this.currentCard.stages[stageIdx];
		const targetConceptIdx = stage.concepts.findIndex((x) => x.name == conceptName);
		let targetConcept = targetConceptIdx !== -1 ? stage.concepts[targetConceptIdx] : null;
		if (conceptValue === "PERMITTED") {
			if (targetConcept) stage.concepts.splice(targetConceptIdx, 1);
			return;
		}
		if (!targetConcept) {
			targetConcept = {
				name: conceptName,
				kind: conceptValue
			};
			stage.concepts = [...stage.concepts, targetConcept];
		} else stage.concepts[targetConceptIdx] = {
			...targetConcept,
			kind: conceptValue
		};
	}
	setCardCommonLawValue(lawName, lawValue) {
		if (!this.currentCard) return;
		for (const stage of this.currentCard.stages) {
			const targetLawIdx = stage.laws.findIndex((x) => x.name == lawName);
			let targetLaw = targetLawIdx !== -1 ? stage.laws[targetLawIdx] : null;
			if (lawValue === "PERMITTED") {
				if (targetLaw) stage.laws.splice(targetLawIdx, 1);
				continue;
			}
			if (!targetLaw) {
				targetLaw = {
					name: lawName,
					kind: lawValue
				};
				stage.laws = [...stage.laws, targetLaw];
			} else stage.laws[targetLawIdx] = {
				...targetLaw,
				kind: lawValue
			};
		}
	}
	setCardStageLawValue(stageIdx, lawName, lawValue) {
		if (!this.currentCard || !this.currentCard.stages[stageIdx]) return;
		const stage = this.currentCard.stages[stageIdx];
		const targetLawIdx = stage.laws.findIndex((x) => x.name == lawName);
		let targetLaw = targetLawIdx !== -1 ? stage.laws[targetLawIdx] : null;
		if (lawValue === "PERMITTED") {
			if (targetLaw) stage.laws.splice(targetLawIdx, 1);
			return;
		}
		if (!targetLaw) {
			targetLaw = {
				name: lawName,
				kind: lawValue
			};
			stage.laws = [...stage.laws, targetLaw];
		} else stage.laws[targetLawIdx] = {
			...targetLaw,
			kind: lawValue
		};
	}
	setCardStageSkillValue(stageIdx, skillName, skillValue) {
		if (!this.currentCard || !this.currentCard.stages[stageIdx]) return;
		const stage = this.currentCard.stages[stageIdx];
		const targetSkillIdx = stage.skills.findIndex((x) => x.name == skillName);
		let targetSkill = targetSkillIdx !== -1 ? stage.skills[targetSkillIdx] : null;
		if (skillValue === "PERMITTED") {
			if (targetSkill) stage.skills.splice(targetSkillIdx, 1);
			return;
		}
		if (!targetSkill) {
			targetSkill = {
				name: skillName,
				kind: skillValue
			};
			stage.skills = [...stage.skills, targetSkill];
		} else stage.skills[targetSkillIdx] = {
			...targetSkill,
			kind: skillValue
		};
	}
	setCardCommonSkillValue(skillName, skillValue) {
		if (!this.currentCard) return;
		for (const stage of this.currentCard.stages) {
			const targetSkillIdx = stage.skills.findIndex((x) => x.name == skillName);
			let targetSkill = targetSkillIdx !== -1 ? stage.laws[targetSkillIdx] : null;
			if (skillValue === "PERMITTED") {
				if (targetSkill) stage.laws.splice(targetSkillIdx, 1);
				continue;
			}
			if (!targetSkill) {
				targetSkill = {
					name: skillName,
					kind: skillValue
				};
				stage.skills = [...stage.skills, targetSkill];
			} else stage.skills[targetSkillIdx] = {
				...targetSkill,
				kind: skillValue
			};
		}
	}
	setCardStageNumberOfQuestions(stageIdx, rawNumberOfQuesions) {
		if (!this.currentCard || !this.currentCard.stages[stageIdx]) return;
		const stage = this.currentCard.stages[stageIdx];
		if (!rawNumberOfQuesions.match(/^\d*$/)) return;
		stage.numberOfQuestions = +rawNumberOfQuesions || 1;
	}
	setCardSurveyEnabled(enabled) {
		if (!this.currentCard) return;
		if (!this.currentCard.options.surveyOptions) {
			this.currentCard.options.surveyOptions = {
				enabled,
				surveyId: ""
			};
			return;
		}
		this.currentCard.options.surveyOptions.enabled = enabled;
	}
	setCardSurveyId(surveyId) {
		if (!this.currentCard) return;
		if (!this.currentCard.options.surveyOptions) {
			this.currentCard.options.surveyOptions = {
				enabled: true,
				surveyId
			};
			return;
		}
		this.currentCard.options.surveyOptions.surveyId = surveyId;
	}
	setCardTags(tags) {
		if (!this.currentCard) return;
		this.currentCard.tags = tags;
	}
	setCardOption(optionId, value) {
		if (!this.currentCard) return;
		this.currentCard.options[optionId] = value;
	}
	addStage() {
		if (!this.currentCard || !this.domains) return;
		const card = this.currentCard;
		const newStage = new ExerciseStageStore(this.courseId, card, {
			numberOfQuestions: 10,
			complexity: .5,
			laws: [],
			concepts: [],
			skills: []
		});
		this.currentCard.stages.push(newStage);
	}
	removeStage(stageIdx) {
		if (!this.currentCard) return;
		const length = this.currentCard.stages.length;
		if (stageIdx < 0 || stageIdx >= length) return;
		this.currentCard.stages[stageIdx][Symbol.dispose]();
		this.currentCard.stages.splice(stageIdx, 1);
	}
};
//#endregion
//#region src/main/js/hooks/use-course-id.ts
function useCourseId() {
	const [params] = useSearchParams();
	const raw = params.get("courseId");
	if (!raw || raw === "null") return null;
	const n = Number(raw);
	return Number.isFinite(n) ? n : null;
}
//#endregion
//#region src/main/js/components/exercise/exercise-row-badge.tsx
var variants = {
	global: "bg-warning text-dark",
	original: "bg-success",
	inherited: "bg-info text-dark",
	cloned: "bg-secondary"
};
var ExerciseRowBadge = ({ linkType }) => {
	const { t } = useTranslation();
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
		className: `badge ${variants[linkType]}`,
		children: t(`exerciseBadge_${linkType}`)
	});
};
//#endregion
//#region src/main/js/components/exercise/delete-global-exercise-modal.tsx
var DeleteGlobalExerciseModal = ({ exerciseId, onConfirm, onCancel }) => {
	const { t } = useTranslation();
	const [memberships, setMemberships] = (0, import_react.useState)(null);
	(0, import_react.useEffect)(() => {
		(async () => {
			const r = await courseController.getExerciseMemberships(exerciseId);
			if (isRight(r)) setMemberships(r.right);
		})();
	}, [exerciseId]);
	const byLms = (memberships ?? []).reduce((acc, m) => {
		(acc[m.educationResourceUrl] ??= []).push(m);
		return acc;
	}, {});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Modal, {
		show: true,
		title: t("deleteModal_title"),
		handleClose: onCancel,
		closeButton: true,
		primaryBtnTitle: t("deleteModal_confirm"),
		primaryBtnVariant: "danger",
		handlePrimaryBtnClicked: onConfirm,
		secondaryBtnTitle: t("deleteModal_cancel"),
		handleSecondaryBtnClicked: onCancel,
		children: memberships === null ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("deleteModal_loading") }) : memberships.length === 0 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("deleteModal_noUsages") }) : /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
			className: "text-warning",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("strong", { children: t("deleteModal_warning") }),
				" ",
				t("deleteModal_warningBody")
			]
		}), Object.entries(byLms).map(([lms, courses]) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "mb-2",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "fw-bold",
				children: lms
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", { children: courses.map((c) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: c.name }, c.id)) })]
		}, lms))] })
	});
};
//#endregion
//#region src/main/js/pages/exercise-settings.tsx
var ExerciseSettings = observer(() => {
	const [exerciseStore] = (0, import_react.useState)(() => new ExerciseSettingsStore());
	const { t } = useTranslation();
	const user = useCurrentUser();
	const session = useSession();
	const courseId = useCourseId();
	const canCreate = exerciseStore.permissions.canCreateExercise;
	(0, import_react.useEffect)(() => {
		(async () => {
			await exerciseStore.loadExercises(courseId);
			const currentExercise = new URL(window.location.href).searchParams.get("exerciseId");
			if (currentExercise) await exerciseStore.loadExercise(Number.parseInt(currentExercise));
		})();
	}, [courseId, exerciseStore]);
	const onNewExerciseClicked = (0, import_react.useCallback)(() => {
		(async () => {
			await exerciseStore.createNewExecise();
		})();
	}, [exerciseStore]);
	const onLangClicked = (0, import_react.useCallback)(() => {
		const newLang = user?.language === "RU" ? "EN" : "RU";
		session.changeLanguage(newLang);
	}, [session, user]);
	if (exerciseStore.exercisesLoadStatus === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	if (!user) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "container-fluid",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "pt-1 pb-3",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
				text: t("exercisesettings_title"),
				languageHint: t("language_header"),
				language: user?.language ?? "EN",
				onLanguageClicked: onLangClicked,
				userHint: t("signedin_as_header"),
				user: user.displayName,
				userHref: null,
				logoutLabel: t("logout_header")
			})
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "flex-xl-nowrap row",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "col-xl-3 col-md-3 col-12 d-flex flex-column",
				children: [canCreate && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					className: "mb-3",
					onClick: onNewExerciseClicked,
					children: "Create new"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "list-group",
					children: exerciseStore.exercises?.map((e) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
						className: `list-group-item ${e.id === exerciseStore.currentCard?.id && "active" || ""}`,
						to: `?exerciseId=${e.id}${courseId != null ? `&courseId=${courseId}` : ""}`,
						onClick: () => exerciseStore.loadExercise(e.id),
						title: e.name,
						children: e.name.length > 22 ? `${e.name.substring(0, 22)}...` : e.name
					}, e.id))
				})]
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "col-xl-9 col-md-9 col-12",
				children: [exerciseStore.storeState.tag === "ERROR" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadFailure, { error: exerciseStore.storeState.error }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseCardElement, {
					store: exerciseStore,
					card: exerciseStore.currentCard,
					domains: exerciseStore.domains ?? [],
					backends: exerciseStore.backends ?? [],
					strategies: exerciseStore.strategies ?? []
				})]
			})]
		})]
	});
});
var ExerciseCardElement = observer((props) => {
	const { card, domains, strategies, store } = props;
	const { t } = useTranslation();
	if (store.exercisesLoadStatus === "EXERCISELOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, { delay: 200 });
	if (card == null) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: "No exercise selected" });
	const currentDomain = domains.find((z) => z.id === card.domainId);
	const stageDomainLaws = currentDomain?.laws.filter((l) => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
	const stageDomainConcepts = currentDomain?.concepts.filter((l) => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
	const stageDomainSkills = currentDomain?.skills;
	const cardLaws = card.stages[0].laws.reduce((acc, i) => (acc[i.name] = i, acc), {});
	const cardConcepts = card.stages[0].concepts.reduce((acc, i) => (acc[i.name] = i, acc), {});
	const sharedDomainLaws = currentDomain?.laws.filter((l) => (l.bitflags & DomainConceptFlag.TargetEnabled) === 0);
	const sharedDomainConcepts = currentDomain?.concepts.filter((c) => (c.bitflags & DomainConceptFlag.TargetEnabled) === 0);
	const sharedDomainSkills = [];
	const currentStrategy = strategies.find((s) => s.id === card.strategyId);
	const linkType = store.cardLinkType;
	const canEdit = card.permissions.canEdit;
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseModeBar, {
			store,
			linkType,
			courseId: store.courseId
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)("fieldset", {
			disabled: !canEdit,
			style: !canEdit ? {
				pointerEvents: "none",
				opacity: .65
			} : void 0,
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("form", {
				className: "exercise-settings-form",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							htmlFor: "exampleInputEmail1",
							children: t("exercisesettings_name")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Control, {
							value: card.name,
							type: "email",
							id: "exampleInputEmail1",
							"aria-describedby": "emailHelp",
							placeholder: "Enter email",
							onChange: (e) => store.setCardName(e.target.value)
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
								className: "fw-bold",
								children: t("exercisesettings_domain")
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Select, {
								id: "domainId",
								"aria-describedby": "domainDescription",
								value: card.domainId,
								onChange: (e) => store.setCardDomain(e.target.value),
								title: currentDomain?.displayName,
								children: domains?.map((d) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("option", {
									value: d.id,
									title: d.description ?? d.displayName,
									children: d.displayName
								}, d.id))
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("small", {
								id: "domainDescription",
								className: "form-text text-muted",
								children: currentDomain?.description ?? ""
							})
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
								className: "fw-bold",
								children: t("exercisesettings_strategy")
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Select, {
								id: "strategyId",
								"aria-describedby": "strategyDescription",
								value: card.strategyId,
								onChange: (e) => store.setCardStrategy(e.target.value),
								title: currentStrategy?.displayName,
								children: strategies?.map((d) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("option", {
									value: d.id,
									title: d.description ?? d.displayName,
									children: d.displayName
								}, d.id))
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("small", {
								id: "strategyDescription",
								className: "form-text text-muted",
								children: currentStrategy?.description ?? ""
							})
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
								className: "fw-bold",
								children: t("exercisesettings_qopt")
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
								type: "checkbox",
								id: "forceNewAttemptCreationEnabled",
								label: t("exercisesettings_qopt_forceAttCreation"),
								checked: card.options.forceNewAttemptCreationEnabled,
								onChange: (x) => store.setCardOption("forceNewAttemptCreationEnabled", x.target.checked)
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
								type: "checkbox",
								id: "correctAnswerGenerationEnabled",
								label: t("exercisesettings_qopt_genCorAnsw"),
								checked: card.options.correctAnswerGenerationEnabled,
								onChange: (x) => store.setCardOption("correctAnswerGenerationEnabled", x.target.checked)
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
								type: "checkbox",
								id: "newQuestionGenerationEnabled",
								label: t("exercisesettings_qopt_forceShowGenNextQ"),
								checked: card.options.newQuestionGenerationEnabled,
								onChange: (x) => store.setCardOption("newQuestionGenerationEnabled", x.target.checked)
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
								type: "checkbox",
								id: "supplementaryQuestionsEnabled",
								label: t("exercisesettings_qopt_supQ"),
								checked: card.options.supplementaryQuestionsEnabled,
								onChange: (x) => store.setCardOption("supplementaryQuestionsEnabled", x.target.checked)
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
								type: "checkbox",
								id: "debugButtonEnabled",
								label: t("exercisesettings_qopt_debugBtn"),
								checked: card.options.debugButtonEnabled,
								onChange: (x) => store.setCardOption("debugButtonEnabled", x.target.checked)
							})
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							htmlFor: `maxExpectedConcurrentStudents`,
							children: t("exercisesettings_max_concurrent_students")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Control, {
							type: "number",
							id: `maxExpectedConcurrentStudents`,
							value: store.currentCard?.options.maxExpectedConcurrentStudents,
							onChange: (e) => store.setCardOption("maxExpectedConcurrentStudents", +e.target.value)
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							htmlFor: "survOptions",
							className: "fw-bold",
							children: t("exercisesettings_survey")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "input-group mb-3",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "input-group-text",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
									checked: card.options.surveyOptions?.enabled,
									type: "checkbox",
									"aria-label": "Checkbox for following text input",
									onChange: (x) => store.setCardSurveyEnabled(x.target.checked)
								})
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Control, {
								id: "survOptions",
								type: "text",
								value: card.options.surveyOptions?.surveyId,
								"aria-label": "Text input with checkbox",
								disabled: !card.options.surveyOptions?.enabled,
								onChange: (x) => store.setCardSurveyId(x.target.value)
							})]
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							htmlFor: "exTagsValues",
							className: "fw-bold",
							children: t("exercisesettings_tags")
						}), currentDomain?.tags.map((t, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check, {
							type: "checkbox",
							id: `tag_checkbox_${t}`,
							label: t,
							value: t,
							checked: card.tags.includes(t),
							onChange: (x) => x.target.checked ? store.setCardTags([.../* @__PURE__ */ new Set([...card.tags, x.target.value])]) : store.setCardTags([...card.tags.filter((z) => z !== x.target.value)])
						}, i))]
					}),
					sharedDomainConcepts?.length && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: t("exercisesettings_commonConcepts")
						}) }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "row",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "col-md-12",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
									className: "list-group list-group-flush",
									children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseConcepts, {
										id: "common_concepts",
										store,
										concepts: sharedDomainConcepts,
										cardConcepts,
										onChange: (concept, conceptValue, parent) => {
											store.setCardCommonConceptValue(concept.name, conceptValue);
											if (!parent) concept.childs.forEach((c) => store.setCardCommonConceptValue(c.name, conceptValue));
											else store.setCardCommonConceptValue(parent.name, "PERMITTED");
										}
									})
								})
							})
						})]
					}) || null,
					sharedDomainLaws?.length && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: t("exercisesettings_commonLaws")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "list-group list-group-flush",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseLaws, {
								id: "common_laws",
								store,
								laws: sharedDomainLaws,
								cardLaws,
								onChange: (law, lawValue, parent) => {
									store.setCardCommonLawValue(law.name, lawValue);
									if (!parent) law.childs.forEach((c) => store.setCardCommonLawValue(c.name, lawValue));
									else store.setCardCommonLawValue(parent.name, "PERMITTED");
								}
							})
						})]
					}) || null,
					sharedDomainSkills?.length && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: t("exercisesettings_commonSkills")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "list-group list-group-flush",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseSkills, {
								id: "common_skills",
								store,
								skills: sharedDomainSkills,
								cardSkills: cardLaws,
								onChange: (skill, skillValue, parent) => {
									store.setCardCommonSkillValue(skill.name, skillValue);
									if (!parent) skill.childs.forEach((c) => store.setCardCommonSkillValue(c.name, skillValue));
									else store.setCardCommonSkillValue(parent.name, "PERMITTED");
								}
							})
						})]
					}) || null,
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold mb-2",
							children: t("exercisesettings_stages")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "list-group list-group-flush",
							children: card.stages.map((stage, stageIdx, stages) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseStage, {
								store,
								stage,
								stageIdx,
								showDeleteBtn: stages.length > 1,
								strategy: currentStrategy,
								stageDomainConcepts,
								stageDomainSkills,
								stageDomainLaws
							}, stageIdx))
						})]
					}),
					currentStrategy?.options.multiStagesEnabled ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						style: { marginTop: "-1rem" },
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "success",
							onClick: () => store.addStage(),
							children: t("exercisesettings_addStage")
						})
					}) : null
				]
			})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "mt-5",
			children: [
				canEdit && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					className: "me-2",
					onClick: () => store.saveCard(),
					children: t("exercisesettings_save")
				}),
				canEdit && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					className: "me-2",
					onClick: () => store.saveCard().then(() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ""}`, "_blank")?.focus()),
					children: t("exercisesettings_saveNopen")
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					className: "me-2",
					onClick: () => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ""}`, "_blank")?.focus(),
					children: t("exercisesettings_open")
				}),
				canEdit && currentStrategy?.options.multiStagesEnabled && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					className: "me-2",
					onClick: () => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ""}&debug`, "_blank")?.focus(),
					children: t("exercisesettings_genDebugAtt")
				})
			]
		})
	] });
});
var ExerciseModeBar = observer(({ store, linkType, courseId }) => {
	const { t } = useTranslation();
	const [showDeleteModal, setShowDeleteModal] = (0, import_react.useState)(false);
	const [busy, setBusy] = (0, import_react.useState)(false);
	const card = store.currentCard;
	if (!card) return null;
	const { canCloneToCourse, canUnlinkFromCourse, canCopyToGlobalPool, canDelete } = card.permissions;
	const showToolbar = canCloneToCourse || canUnlinkFromCourse || canCopyToGlobalPool || canDelete;
	const onConvertToClone = async () => {
		if (courseId == null) return;
		setBusy(true);
		await store.cloneCurrentToCourse(courseId);
		setBusy(false);
	};
	const onUnlink = async () => {
		if (courseId == null) return;
		setBusy(true);
		await store.unlinkFromCourse(courseId);
		setBusy(false);
	};
	const onCopyToPool = async () => {
		setBusy(true);
		const newId = await store.copyCurrentToPool();
		setBusy(false);
		if (newId != null) window.location.href = `/pages/exercise-settings?exerciseId=${newId}`;
	};
	const onDeleteClick = () => {
		if (card.isPublic) setShowDeleteModal(true);
		else if (window.confirm(t("exerciseModeBar_confirmDelete"))) store.deleteCurrentExercise();
	};
	const onConfirmGlobalDelete = async () => {
		setShowDeleteModal(false);
		setBusy(true);
		await store.deleteCurrentExercise();
		setBusy(false);
	};
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "mb-3",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mb-2",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseRowBadge, { linkType })
			}),
			showToolbar && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "btn-toolbar",
				role: "toolbar",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "btn-group btn-group-sm flex-wrap",
					role: "group",
					style: { gap: "0.25rem" },
					children: [
						canCloneToCourse && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "warning",
							disabled: busy,
							onClick: onConvertToClone,
							children: t("exerciseModeBar_convertToClone")
						}),
						canUnlinkFromCourse && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "outline-danger",
							disabled: busy,
							onClick: onUnlink,
							children: t("exerciseModeBar_unlinkFromCourse")
						}),
						canCopyToGlobalPool && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "info",
							disabled: busy,
							onClick: onCopyToPool,
							children: t("exerciseModeBar_copyToPool")
						}),
						canDelete && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "danger",
							disabled: busy,
							onClick: onDeleteClick,
							children: t("exerciseModeBar_deleteExercise")
						})
					]
				})
			}),
			showDeleteModal && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DeleteGlobalExerciseModal, {
				exerciseId: card.id,
				onConfirm: onConfirmGlobalDelete,
				onCancel: () => setShowDeleteModal(false)
			})
		]
	});
});
var ExerciseStage = observer((props) => {
	const { t } = useTranslation();
	const { store, stage, strategy, stageIdx, showDeleteBtn, stageDomainConcepts, stageDomainLaws, stageDomainSkills } = props;
	if (!store.currentCard) throw new Error("card not set");
	const cardConcepts = stage.concepts.reduce((acc, i) => (acc[i.name] = i, acc), {});
	const cardLaws = stage.laws.reduce((acc, i) => (acc[i.name] = i, acc), {});
	const cardSkills = stage.skills.reduce((acc, i) => (acc[i.name] = i, acc), {});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "card mb-3",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "card-body",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					style: {
						display: "flex",
						justifyContent: "space-between"
					},
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("exercisesettings_stageN", { stageNumber: stageIdx + 1 }) }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: [t("exercisesettings_questionsInBank"), ":\xA0"] }), stage.bankLoadingState === "IN_PROGRESS" || stage.bankSearchResult === null ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {
						styleOverride: {
							width: "1rem",
							height: "1rem"
						},
						delay: 0
					}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: `${stage.bankSearchResult.count} (${stage.bankSearchResult?.topRatedCount})` })] })]
				}),
				strategy?.options.multiStagesEnabled && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold",
						htmlFor: `numberOfQuestions_stage${stageIdx}`,
						children: t("exercisesettings_stageN_qnumber")
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Control, {
						type: "text",
						id: `numberOfQuestions_stage${stageIdx}`,
						value: stage.numberOfQuestions,
						onChange: (e) => store.setCardStageNumberOfQuestions(stageIdx, e.target.value)
					})]
				}) || null,
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold",
						children: t("exercisesettings_qcomplexity")
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "d-flex",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
							type: "range",
							className: "form-range",
							id: `complexity_stage${stageIdx}`,
							value: (stage.complexity ?? .5) * 100,
							onChange: (e) => store.setCardStageComplexity(stageIdx, e.target.value)
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "ms-2",
							children: stage.complexity.toFixed(2)
						})]
					})]
				}),
				stageDomainConcepts && stageDomainConcepts.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold",
						children: t("exercisesettings_stageN_concepts")
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "list-group list-group-flush",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseConcepts, {
							id: `stage${stageIdx}_concepts`,
							store,
							concepts: stageDomainConcepts,
							cardConcepts,
							onChange: (concept, conceptValue, parent) => {
								store.setCardStageConceptValue(stageIdx, concept.name, conceptValue);
								if (!parent) concept.childs.forEach((c) => store.setCardStageConceptValue(stageIdx, c.name, conceptValue));
								else store.setCardStageConceptValue(stageIdx, parent.name, "PERMITTED");
							}
						})
					})]
				}) || null,
				stageDomainLaws && stageDomainLaws.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold",
						children: t("exercisesettings_stageN_laws")
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "list-group list-group-flush",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseLaws, {
							id: `stage${stageIdx}_laws`,
							store,
							laws: stageDomainLaws,
							cardLaws,
							onChange: (law, lawValue, parent) => {
								store.setCardStageLawValue(stageIdx, law.name, lawValue);
								if (!parent) law.childs.forEach((c) => store.setCardStageLawValue(stageIdx, c.name, lawValue));
								else store.setCardStageLawValue(stageIdx, parent.name, "PERMITTED");
							}
						})
					})]
				}) || null,
				stageDomainSkills && stageDomainSkills.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold",
						children: t("exercisesettings_stageN_skills")
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "list-group list-group-flush",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseSkills, {
							id: `stage${stageIdx}_skills`,
							store,
							skills: stageDomainSkills,
							cardSkills,
							onChange: (skill, skillValue, parent) => {
								store.setCardStageSkillValue(stageIdx, skill.name, skillValue);
								if (!parent) skill.childs.forEach((c) => store.setCardStageSkillValue(stageIdx, c.name, skillValue));
								else store.setCardStageSkillValue(stageIdx, parent.name, "PERMITTED");
							}
						})
					})]
				}) || null,
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "mb-3",
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: t("exercisesettings_stageN_matchedQuestionExamples")
						}),
						stage.bankLoadingState === "IN_PROGRESS" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {
							styleOverride: {
								marginLeft: "0.5rem",
								width: "1rem",
								height: "1rem"
							},
							delay: 0
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "list-group",
							children: stage.bankSearchResult.questions.length === 0 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "list-group-item",
								children: t("exercisesettings_noQuestionsFound")
							}) : stage.bankSearchResult.questions.map((q, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "list-group-item",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
									target: "_blank",
									href: `/pages/question?metadataId=${q.metadataId}`,
									children: q.name
								})
							}, i))
						})
					]
				}),
				showDeleteBtn && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "d-flex justify-content-end",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
						variant: "danger",
						onClick: () => store.removeStage(stageIdx),
						children: t("exercisesettings_removeStage")
					})
				}) || null
			]
		})
	});
});
var ExerciseConcepts = observer((props) => {
	const { id, store, concepts, cardConcepts, onChange } = props;
	const { t } = useTranslation();
	const card = store.currentCard;
	if (!card) throw new Error("card not set");
	const conceptFlagNames = (0, import_react.useMemo)(() => {
		return [
			t("exercisesettings_optDenied"),
			t("exercisesettings_optAllowed"),
			t("exercisesettings_optTarget")
		];
	}, [t]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: concepts.map((coreConcept, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "list-group-item p-0 bg-transparent pt-2 pb-2",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: `d-flex flex-row align-items-center`,
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
				id: `concept_${id}_toggle_${card.id}_${coreConcept.name}_${idx}`,
				selected: mapKindToValue(cardConcepts[coreConcept.name]?.kind),
				values: getConceptFlags(coreConcept),
				valueStyles: [
					{ backgroundColor: "#eb2828" },
					null,
					{ backgroundColor: "#009700" }
				],
				displayNames: conceptFlagNames,
				onChange: (val) => onChange(coreConcept, mapValueToKind(val))
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				style: { marginLeft: "15px" },
				children: coreConcept.displayName
			})]
		}) }), coreConcept.childs.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
			className: "",
			children: coreConcept.childs.map((childConcept, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
				className: `d-flex flex-row align-items-centers mt-3`,
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
					id: `concept_${id}_toggle_${card.id}_${childConcept.name}_${idx}_${i}`,
					selected: mapKindToValue(cardConcepts[childConcept.name]?.kind),
					values: getConceptFlags(childConcept),
					valueStyles: [
						{ backgroundColor: "#eb2828" },
						null,
						{ backgroundColor: "#009700" }
					],
					displayNames: conceptFlagNames,
					onChange: (val) => onChange(childConcept, mapValueToKind(val), coreConcept)
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					style: { marginLeft: "15px" },
					children: childConcept.displayName
				})]
			}, i) }))
		})]
	}, idx)) });
});
var ExerciseLaws = observer((props) => {
	const { id, store, laws, cardLaws, onChange } = props;
	const { t } = useTranslation();
	const card = store.currentCard;
	if (!card) throw new Error("card not set");
	const lawFlagNames = (0, import_react.useMemo)(() => {
		return [
			t("exercisesettings_optDenied"),
			t("exercisesettings_optAllowed"),
			t("exercisesettings_optTarget")
		];
	}, [t]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: laws.map((coreLaw, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "list-group-item p-0 bg-transparent pt-2 pb-2",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: `d-flex flex-row align-items-center`,
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
				id: `law_${id}_toggle_${card.id}_${idx}`,
				selected: mapKindToValue(cardLaws[coreLaw.name]?.kind),
				values: [
					"Denied",
					"Allowed",
					"Target"
				],
				valueStyles: [
					{ backgroundColor: "#eb2828" },
					null,
					{ backgroundColor: "#009700" }
				],
				displayNames: lawFlagNames,
				onChange: (val) => onChange(coreLaw, mapValueToKind(val))
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				style: { marginLeft: "15px" },
				children: coreLaw.displayName
			})]
		}) }), coreLaw.childs.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
			className: "",
			children: coreLaw.childs.map((childLaw, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
				className: `d-flex flex-row align-items-centers mt-3`,
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
					id: `law_${id}_toggle_${card.id}_${idx}_${i}`,
					selected: mapKindToValue(cardLaws[childLaw.name]?.kind),
					values: [
						"Denied",
						"Allowed",
						"Target"
					],
					valueStyles: [
						{ backgroundColor: "#eb2828" },
						null,
						{ backgroundColor: "#009700" }
					],
					displayNames: lawFlagNames,
					onChange: (val) => onChange(childLaw, mapValueToKind(val), coreLaw)
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					style: { marginLeft: "15px" },
					children: childLaw.displayName
				})]
			}, i) }))
		})]
	}, idx)) });
});
var ExerciseSkills = observer((props) => {
	const { id, store, skills, cardSkills, onChange } = props;
	const { t } = useTranslation();
	const card = store.currentCard;
	if (!card) throw new Error("card not set");
	const skillFlagNames = (0, import_react.useMemo)(() => {
		return [
			t("exercisesettings_optDenied"),
			t("exercisesettings_optAllowed"),
			t("exercisesettings_optTarget")
		];
	}, [t]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: skills.map((coreSkill, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "list-group-item p-0 bg-transparent pt-2 pb-2",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: `d-flex flex-row align-items-center`,
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
				id: `skill_${id}_toggle_${card.id}_${idx}`,
				selected: mapKindToValue(cardSkills[coreSkill.name]?.kind),
				values: [
					"Denied",
					"Allowed",
					"Target"
				],
				valueStyles: [
					{ backgroundColor: "#eb2828" },
					null,
					{ backgroundColor: "#009700" }
				],
				displayNames: skillFlagNames,
				onChange: (val) => onChange(coreSkill, mapValueToKind(val))
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				style: { marginLeft: "15px" },
				children: coreSkill.displayName
			})]
		}) }), coreSkill.childs.length > 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
			className: "",
			children: coreSkill.childs.map((childSkill, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(import_jsx_runtime.Fragment, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
				className: `d-flex flex-row align-items-centers mt-3`,
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
					id: `skill_${id}_toggle_${card.id}_${idx}_${i}`,
					selected: mapKindToValue(cardSkills[childSkill.name]?.kind),
					values: [
						"Denied",
						"Allowed",
						"Target"
					],
					valueStyles: [
						{ backgroundColor: "#eb2828" },
						null,
						{ backgroundColor: "#009700" }
					],
					displayNames: skillFlagNames,
					onChange: (val) => onChange(childSkill, mapValueToKind(val), coreSkill)
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					style: { marginLeft: "15px" },
					children: childSkill.displayName
				})]
			}, i) }))
		})]
	}, idx)) });
});
function mapKindToValue(kind) {
	return kind === "FORBIDDEN" ? "Denied" : kind === "TARGETED" ? "Target" : "Allowed";
}
function mapValueToKind(value) {
	return value === "Denied" ? "FORBIDDEN" : value === "Target" ? "TARGETED" : "PERMITTED";
}
function getConceptFlags(c) {
	return (c.bitflags & DomainConceptFlag.TargetEnabled) > 0 ? [
		"Denied",
		"Allowed",
		"Target"
	] : ["Denied", "Allowed"];
}
//#endregion
//#region src/main/js/pages/strategy-settings.tsx
var db = {
	concepts: [
		"literal",
		"variable",
		"arithmetic operator",
		"assignment operator",
		"comparison operator",
		"logical operator",
		"bitwise operator",
		"array access operator",
		"pointer operator",
		"function call"
	],
	laws: [
		"Expression contains two operators with different precedence in a row",
		"Expression contains two operators with the same precedence and left associativity in a row",
		"Expression contains two operators with the same precedence and right associativity in a row",
		"Expression contains operators inside parentheses or another operator",
		"Expression contains operators evaluating their operands in a strict order"
	]
};
var StrategySettings = observer(() => {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "container-fluid",
		style: {
			color: "black",
			fontSize: "18px"
		},
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "row",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "col-md-8",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("form", { children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: "Domain"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("select", {
							className: "form-select",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("option", { children: "Order of Expression Evaluation" })
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "row",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "col-md-6",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
								className: "mb-3",
								children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
									className: "fw-bold",
									children: "Question complexity"
								}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
									type: "range",
									className: "form-range",
									id: "formControlRange1"
								}) })]
							})
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
							className: "col-md-6",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
								className: "mb-3",
								children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
									className: "fw-bold",
									children: "Answer length"
								}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
									type: "range",
									className: "form-range",
									id: "formControlRange1"
								}) })]
							})
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: "Concepts"
						}) }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "row",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "col-md-6",
								children: db.concepts.filter((_, i) => i % 2 === 0).map((c, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
									className: "d-flex flex-row align-items-center",
									style: { marginBottom: "10px" },
									children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
										id: `concept_toggle_${c}_${idx}`,
										selected: "Allowed",
										values: [
											"Denied",
											"Allowed",
											"Target"
										],
										valueStyles: [
											{ backgroundColor: "#eb2828" },
											null,
											{ backgroundColor: "#009700" }
										],
										onChange: () => 0
									}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
										style: { marginLeft: "15px" },
										children: c
									})]
								}))
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								className: "col-md-6",
								children: db.concepts.filter((_, i) => i % 2 !== 0).map((c, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
									className: "d-flex flex-row align-items-center",
									style: { marginBottom: "10px" },
									children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
										id: `concept_toggle_${c}_${idx}`,
										selected: "Allowed",
										values: [
											"Denied",
											"Allowed",
											"Target"
										],
										valueStyles: [
											{ backgroundColor: "#eb2828" },
											null,
											{ backgroundColor: "#009700" }
										],
										onChange: () => 0
									}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
										style: { marginLeft: "15px" },
										children: c
									})]
								}))
							})]
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
							className: "fw-bold",
							children: "Laws"
						}), db.laws.map((c, idx) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "d-flex flex-row align-items-start justify-content-start",
							style: { marginBottom: "10px" },
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ToggleSwitch, {
								id: `law_toggle_${idx}`,
								selected: "Allowed",
								values: [
									"Denied",
									"Allowed",
									"Target"
								],
								valueStyles: [
									{ backgroundColor: "#eb2828" },
									null,
									{ backgroundColor: "#009700" }
								],
								onChange: () => 0
							}) }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
								style: { marginLeft: "15px" },
								children: c
							})]
						}))]
					})
				] })
			})
		})
	});
});
//#endregion
//#region src/main/js/pages/question.tsx
var QuestionPage = observer(() => {
	const [question] = (0, import_react.useState)(() => new QuestionStore());
	(0, import_react.useEffect)(() => {
		const metadataId = new URLSearchParams(window.location.search).get("metadataId");
		if (!metadataId || Number.isNaN(Number(metadataId))) throw new Error("no metadataId provided in the URL");
		question.generateQuestionByMetadata(+metadataId);
	}, [question]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Question, {
		store: question,
		showExtendedFeedback: true
	}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "mt-3 position-relative",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(GenerateNextAnswerBtn, { store: question }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DebugButton, { metadataId: question.question?.questionMetadataId ?? -1 })]
	})] });
});
//#endregion
//#region src/main/js/stores/global-pool-store.ts
var GlobalPoolStore = class {
	exercises = [];
	permissions = noExerciseListPermissions;
	loadStatus = "NONE";
	error = null;
	constructor() {
		makeAutoObservable(this);
	}
	async loadGlobalPool() {
		this.loadStatus = "LOADING";
		this.error = null;
		const r = await exerciseSettingsController.listExercises(null);
		if (isLeft(r)) {
			this.error = r.left;
			this.loadStatus = "FAILED";
			return;
		}
		this.exercises = r.right.exercises;
		this.permissions = r.right.permissions;
		this.loadStatus = "LOADED";
	}
	async importToCourse(exerciseId, targetCourseId, mode) {
		if (mode === "INHERIT") {
			const r = await courseController.addExerciseToCourse(exerciseId, targetCourseId);
			return isRight(r);
		}
		const r = await exerciseSettingsController.cloneExercise(exerciseId, targetCourseId);
		return isRight(r);
	}
};
//#endregion
//#region src/main/js/pages/global-pool.tsx
var GlobalPool = observer(() => {
	const [store] = (0, import_react.useState)(() => new GlobalPoolStore());
	const navigate = useNavigate();
	const user = useCurrentUser();
	const session = useSession();
	const { t } = useTranslation();
	(0, import_react.useEffect)(() => {
		store.loadGlobalPool();
	}, [store]);
	const onLangClicked = () => {
		const newLang = user?.language === "RU" ? "EN" : "RU";
		session.changeLanguage(newLang);
	};
	if (!user || store.loadStatus === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "container-fluid",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "pt-1 pb-3",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
					text: t("globalPool_page_title"),
					languageHint: t("language_header"),
					language: user?.language ?? "EN",
					onLanguageClicked: onLangClicked,
					userHint: t("signedin_as_header"),
					user: user.displayName,
					userHref: null,
					logoutLabel: t("logout_header")
				})
			}),
			store.loadStatus === "FAILED" && store.error && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadFailure, {
				error: store.error,
				onRetry: () => store.loadGlobalPool()
			}),
			store.permissions.canCreateExercise && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mb-3",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					onClick: () => navigate("/pages/exercise-settings"),
					children: t("globalPool_page_createBtn")
				})
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "list-group",
				children: [store.exercises.map((e) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
					className: "list-group-item",
					to: `/pages/exercise-settings?exerciseId=${e.id}`,
					children: e.name
				}, e.id)), store.exercises.length === 0 && store.loadStatus === "LOADED" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("a", {
					className: "list-group-item text-muted",
					children: t("globalPool_page_empty")
				})]
			})
		]
	});
});
//#endregion
//#region src/main/js/stores/course-store.ts
var CourseStore = class {
	courseId = null;
	exercises = [];
	permissions = noExerciseListPermissions;
	loadStatus = "NONE";
	error = null;
	constructor() {
		makeAutoObservable(this);
	}
	async loadCourse(courseId) {
		this.loadStatus = "LOADING";
		this.courseId = courseId;
		this.error = null;
		const r = await courseController.getCourseExercises(courseId);
		if (isLeft(r)) {
			this.error = r.left;
			this.loadStatus = "FAILED";
			return;
		}
		this.exercises = r.right.exercises;
		this.permissions = r.right.permissions;
		this.loadStatus = "LOADED";
	}
};
//#endregion
//#region src/main/js/components/exercise/import-from-global-modal.tsx
var ImportFromGlobalModal = observer(({ courseId, canInherit, canClone, onClose, onImported }) => {
	const { t } = useTranslation();
	const [store] = (0, import_react.useState)(() => new GlobalPoolStore());
	const [mode, setMode] = (0, import_react.useState)(canInherit ? "INHERIT" : "CLONE");
	const [busyId, setBusyId] = (0, import_react.useState)(null);
	const modeAllowed = mode === "INHERIT" ? canInherit : canClone;
	(0, import_react.useEffect)(() => {
		store.loadGlobalPool();
	}, [store]);
	const onImportClick = async (exerciseId) => {
		setBusyId(exerciseId);
		const ok = await store.importToCourse(exerciseId, courseId, mode);
		setBusyId(null);
		if (ok) {
			onImported?.();
			onClose();
		}
	};
	const inheritWarning = /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "alert alert-warning py-1 px-2 mb-0 mt-2 small",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("strong", { children: t("importModal_inherit_label") }),
			" ",
			t("importModal_inherit_body")
		]
	});
	const cloneHint = /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "alert alert-info py-1 px-2 mb-0 mt-2 small",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("strong", { children: t("importModal_clone_label") }),
			" ",
			t("importModal_clone_body")
		]
	});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Modal, {
		show: true,
		size: "lg",
		title: t("importModal_title"),
		closeButton: true,
		handleClose: onClose,
		secondaryBtnTitle: t("importModal_cancel"),
		handleSecondaryBtnClicked: onClose,
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mb-3",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("label", {
						className: "fw-bold me-2",
						children: t("importModal_modeLabel")
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "btn-group",
						role: "group",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: mode === "INHERIT" ? "warning" : "outline-warning",
							size: "sm",
							disabled: !canInherit,
							onClick: () => setMode("INHERIT"),
							children: t("importModal_inherit_btn")
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: mode === "CLONE" ? "success" : "outline-success",
							size: "sm",
							disabled: !canClone,
							onClick: () => setMode("CLONE"),
							children: t("importModal_clone_btn")
						})]
					}),
					mode === "INHERIT" ? inheritWarning : cloneHint
				]
			}),
			store.loadStatus === "LOADING" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("importModal_loading") }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
				className: "list-group",
				children: store.exercises.map((e) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
					className: "list-group-item d-flex justify-content-between align-items-center",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: e.name }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
						variant: "success",
						size: "sm",
						disabled: busyId !== null || !modeAllowed,
						onClick: () => onImportClick(e.id),
						children: busyId === e.id ? t("importModal_importing") : t("importModal_import")
					})]
				}, e.id))
			})
		]
	});
});
//#endregion
//#region src/main/js/pages/course.tsx
/** Hidden form that auto-POSTs the signed deep-linking response back to Moodle. */
var DeepLinkReturnForm = ({ jwt, returnUrl }) => {
	const formRef = (0, import_react.useRef)(null);
	(0, import_react.useEffect)(() => {
		formRef.current?.submit();
	}, []);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("form", {
		ref: formRef,
		method: "post",
		action: returnUrl,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
			type: "hidden",
			name: "JWT",
			value: jwt
		})
	});
};
/**
* Shown inside Moodle's "Select content" iframe: pick course exercises and push them
* back to Moodle, which creates the External Tool activities.
*/
var DeepLinkSelection = ({ exercises }) => {
	const { t } = useTranslation();
	const [selected, setSelected] = (0, import_react.useState)(/* @__PURE__ */ new Set());
	const [existing, setExisting] = (0, import_react.useState)(/* @__PURE__ */ new Set());
	const [submitting, setSubmitting] = (0, import_react.useState)(false);
	const [error, setError] = (0, import_react.useState)(null);
	const [payload, setPayload] = (0, import_react.useState)(null);
	(0, import_react.useEffect)(() => {
		(async () => {
			const res = await deepLinkingController.existing();
			if (isRight(res)) setExisting(new Set(res.right.exerciseIds));
		})();
	}, []);
	const toggle = (id) => {
		setSelected((prev) => {
			const next = new Set(prev);
			if (next.has(id)) next.delete(id);
			else next.add(id);
			return next;
		});
	};
	const submit = async () => {
		if (selected.size === 0) {
			setError(t("deeplink_selectAtLeastOne"));
			return;
		}
		setSubmitting(true);
		setError(null);
		const res = await deepLinkingController.build(Array.from(selected));
		if (isRight(res)) setPayload(res.right);
		else {
			setError(t("deeplink_error"));
			setSubmitting(false);
		}
	};
	if (payload) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DeepLinkReturnForm, {
		jwt: payload.jwt,
		returnUrl: payload.returnUrl
	});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "container-fluid p-3",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h5", { children: t("deeplink_title") }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-muted",
				children: t("deeplink_hint")
			}),
			exercises.length === 0 && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "text-muted mb-3",
				children: t("deeplink_empty")
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
				className: "list-group mb-3",
				children: exercises.map((e) => {
					const already = existing.has(e.id);
					return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
						className: "list-group-item d-flex align-items-center",
						style: { gap: "0.5rem" },
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Form_default.Check.Input, {
								type: "checkbox",
								className: "mt-0",
								checked: selected.has(e.id),
								disabled: already || submitting,
								onChange: () => toggle(e.id)
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: e.name }),
							already && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
								className: "badge bg-secondary ms-auto",
								children: t("deeplink_added")
							})
						]
					}, e.id);
				})
			}),
			error && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "alert alert-danger",
				children: error
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
				variant: "primary",
				disabled: submitting || selected.size === 0,
				onClick: submit,
				children: submitting ? t("deeplink_submitting") : t("deeplink_addBtn")
			})
		]
	});
};
var CoursePage = observer(() => {
	const [store] = (0, import_react.useState)(() => new CourseStore());
	const navigate = useNavigate();
	const user = useCurrentUser();
	const session = useSession();
	const courseId = useCourseId();
	const [searchParams] = useSearchParams();
	const [showImportModal, setShowImportModal] = (0, import_react.useState)(false);
	const { t } = useTranslation();
	const isDeepLink = searchParams.get("lti") === "deeplink";
	const inIframe = typeof window !== "undefined" && window.self !== window.top;
	(0, import_react.useEffect)(() => {
		if (courseId != null) store.loadCourse(courseId);
	}, [courseId, store]);
	const onLangClicked = () => {
		const newLang = user?.language === "RU" ? "EN" : "RU";
		session.changeLanguage(newLang);
	};
	if (!user) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	if (courseId == null) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { children: t("course_page_courseIdRequired") });
	if (store.loadStatus === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	if (isDeepLink && inIframe) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DeepLinkSelection, { exercises: store.exercises });
	const { canCreateExercise, canImportInherit, canImportClone } = store.permissions;
	const canImport = canImportInherit || canImportClone;
	const reload = () => store.loadCourse(courseId);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "container-fluid",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "pt-1 pb-3",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
					text: t("course_page_title", { id: courseId }),
					languageHint: t("language_header"),
					language: user?.language ?? "EN",
					onLanguageClicked: onLangClicked,
					userHint: t("signedin_as_header"),
					user: user.displayName,
					userHref: null,
					logoutLabel: t("logout_header")
				})
			}),
			isDeepLink && !inIframe && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "alert alert-info",
				children: t("deeplink_blockHint")
			}),
			store.loadStatus === "FAILED" && store.error && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadFailure, {
				error: store.error,
				onRetry: reload
			}),
			(canCreateExercise || canImport) && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mb-3 d-flex",
				style: { gap: "0.5rem" },
				children: [canCreateExercise && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "primary",
					onClick: () => navigate(`/pages/exercise-settings?courseId=${courseId}`),
					children: t("course_page_createExerciseBtn")
				}), canImport && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "secondary",
					onClick: () => setShowImportModal(true),
					children: t("course_page_importBtn")
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("ul", {
				className: "list-group",
				children: [store.exercises.map((e) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", {
					className: "list-group-item",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
						to: `/pages/exercise-settings?exerciseId=${e.id}&courseId=${courseId}`,
						children: e.name
					})
				}, e.id)), store.exercises.length === 0 && store.loadStatus === "LOADED" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", {
					className: "list-group-item text-muted",
					children: t("course_page_empty")
				}, "undefined")]
			}),
			canImport && showImportModal && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ImportFromGlobalModal, {
				courseId,
				canInherit: canImportInherit,
				canClone: canImportClone,
				onClose: () => setShowImportModal(false),
				onImported: reload
			})
		]
	});
});
//#endregion
//#region src/main/js/stores/courses-store.ts
var CoursesStore = class {
	courses = [];
	loadStatus = "NONE";
	error = null;
	constructor() {
		makeAutoObservable(this);
	}
	async loadMyCourses() {
		this.loadStatus = "LOADING";
		this.error = null;
		const r = await courseController.getMyCourses();
		if (isLeft(r)) {
			this.error = r.left;
			this.loadStatus = "FAILED";
			return;
		}
		this.courses = r.right;
		this.loadStatus = "LOADED";
	}
};
//#endregion
//#region src/main/js/pages/courses.tsx
var CoursesPage = observer(() => {
	const [store] = (0, import_react.useState)(() => new CoursesStore());
	const navigate = useNavigate();
	const user = useCurrentUser();
	const session = useSession();
	const { t } = useTranslation();
	(0, import_react.useEffect)(() => {
		store.loadMyCourses();
	}, [store]);
	const onLangClicked = () => {
		const newLang = user?.language === "RU" ? "EN" : "RU";
		session.changeLanguage(newLang);
	};
	if (!user || store.loadStatus === "LOADING") return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Loader, {});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "container-fluid",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "pt-1 pb-3",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
					text: t("courses_page_title"),
					languageHint: t("language_header"),
					language: user.language ?? "EN",
					onLanguageClicked: onLangClicked,
					userHint: t("signedin_as_header"),
					user: user.displayName,
					userHref: null,
					logoutLabel: t("logout_header")
				})
			}),
			user.permissions.canViewGlobalPool && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "mb-3",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "outline-primary",
					onClick: () => navigate("/pages/global-pool"),
					children: t("courses_page_globalPoolBtn")
				})
			}),
			store.loadStatus === "FAILED" && store.error ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LoadFailure, {
				error: store.error,
				onRetry: () => store.loadMyCourses()
			}) : store.courses.length === 0 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "alert alert-info",
				children: t("courses_page_empty")
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "row row-cols-1 row-cols-md-2 row-cols-lg-3",
				children: store.courses.map((c) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "col mb-4",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "card h-100",
						role: "button",
						onClick: () => navigate(`/pages/course?courseId=${c.id}`),
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "card-body",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h5", {
								className: "card-title",
								children: c.name
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("h6", {
								className: "card-subtitle text-muted",
								children: c.educationResourceUrl || `#${c.educationResourceId}`
							})]
						})
					})
				}, c.id))
			})
		]
	});
});
//#endregion
//#region src/main/js/components/common/error-boundary.tsx
var RenderFailure = ({ error }) => {
	const { t } = useTranslation();
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "container pt-3",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Alert_default, {
			variant: "danger",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Alert_default.Heading, {
					as: "h6",
					children: t("error_boundary_title")
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "comp-ph-error-notification-message",
					children: error.message
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "outline-danger",
					size: "sm",
					className: "mt-2",
					onClick: () => window.location.reload(),
					children: t("error_boundary_reload")
				})
			]
		})
	});
};
/**
* Catches exceptions thrown while rendering.
*/
var ErrorBoundary = class extends import_react.Component {
	state = { error: null };
	static getDerivedStateFromError(error) {
		return { error };
	}
	componentDidCatch(error, info) {
		console.error("Render failed:", error, info.componentStack);
	}
	render() {
		return this.state.error !== null ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)(RenderFailure, { error: this.state.error }) : this.props.children;
	}
};
//#endregion
//#region src/main/js/index.tsx
var Home = () => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ErrorNotifications, {}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ErrorBoundary, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SessionProvider, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
	className: "container comp-ph-container",
	children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(BrowserRouter, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Routes, { children: [
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/statistics",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Statistics, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/exercise",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Exercise, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/exercise-settings",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExerciseSettings, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/strategy-settings",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(StrategySettings, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/survey",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SurveyPage, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/question",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(QuestionPage, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/exercises-list",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ExercisesList, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/global-pool",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(GlobalPool, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/course",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(CoursePage, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/pages/courses",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(CoursesPage, {})
		}),
		/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Route, {
			path: "/",
			element: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Navigate, {
				to: "/pages/courses",
				replace: true
			})
		})
	] }) })
}) }) })] });
/**
* `npm run dev:mock` answers the whole api from src/main/js/mocks via a service worker.
* The branch is dead code in a production build, so msw never reaches the bundle.
*/
async function startMocking() {}
var container = document.getElementById("root");
var root = (0, import_client.createRoot)(container);
startMocking().catch((err) => console.error("[mocks] could not start, the api is NOT mocked:", err)).finally(() => root.render(/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Home, {})));
//#endregion
