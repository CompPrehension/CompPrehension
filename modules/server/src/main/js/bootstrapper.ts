import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import "reflect-metadata";
import { container } from "tsyringe";
import { ExerciseController } from "./controllers/exercise/exercise-controller";
import { ExerciseSettingsController } from "./controllers/exercise/exercise-settings";
import { QuestionController } from "./controllers/exercise/question-controller";
import { SurveyController } from "./controllers/exercise/survey-controller";
import { TestExerciseController } from "./controllers/exercise/test-exercise-controller";
import { UserController } from "./controllers/exercise/user-controller";
import { CourseController } from "./controllers/course/course-controller";
import { ExerciseStore } from "./stores/exercise-store";
import { QuestionStore } from "./stores/question-store";

// init DI container
const isSandbox = () => (new URLSearchParams(window.location.search).get('sandbox') ?? null) !== null;
container.register(ExerciseController, { 
    useFactory: () => isSandbox()
        ? new TestExerciseController() 
        : new ExerciseController()
});
container.register(QuestionController, { 
    useFactory: () => isSandbox()
        ? new TestExerciseController() 
        : new QuestionController()
});
container.register(UserController, { 
    useFactory: () => isSandbox()
        ? new TestExerciseController() 
        : new UserController()
});
container.register(QuestionStore, QuestionStore);
container.registerSingleton(ExerciseStore);
container.registerSingleton(SurveyController);
container.registerSingleton(ExerciseSettingsController);
container.registerSingleton(CourseController);

// init localisation
const resources = {
    EN: {
        translation: {
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
            exercisesettings_qopt_preferDTsup: "Prefer Decision-Tree-based approach to supplementary question generation",
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
            exercisesettings_noQuestionsFound: 'No suitable questions found',

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
            importModal_cancel: "Cancel"
        },
    },
    RU: {
        translation: {
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
            issolved_feeback: "Все операторы уже вычислены. Задача решена",
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
            exercisesettings_qopt_preferDTsup: "Предпочитать генерацию вспомогательных вопросов по дереву рассуждений",
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
            exercisesettings_noQuestionsFound: 'Подходящих вопросов не найдено',

            survey_sendresults: "Отправить результаты опроса",
            // tour_skip: "Пропустить обучение",
            tour_skip: "Не показывать",
            tour_complete: "Завершить",
            // tour_next: "Продолжить",
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
            importModal_cancel: "Отмена"
        },
    },
    PL: {
        translation: {
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
            exercisesettings_qopt_preferDTsup: "Prefer Decision-Tree-based approach to supplementary question generation",
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
            exercisesettings_noQuestionsFound: 'No suitable questions found',

            survey_sendresults: "Send survey results",

            importModal_title: "Import from global pool",
            importModal_modeLabel: "Mode:",
            importModal_inherit_body: "the course will use the shared pool entry. Any change the author makes in the pool will be immediately reflected here and may break ongoing student attempts if the content changes. Convenient for synchronization, but risky during active use. If unsure — choose Clone.",
            importModal_clone_body: "an independent copy is created. The course then works with its own version; the author's changes in the pool do not affect this copy.",
            importModal_loading: "Loading…",
            importModal_importing: "Importing…",
            importModal_import: "Import",
            importModal_cancel: "Cancel"
        },
    },
};
i18next
    .use(initReactI18next)
    .init({
        resources,
        lng: "EN",
        interpolation: {
            escapeValue: false,
        },
    });
