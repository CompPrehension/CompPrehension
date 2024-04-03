import "reflect-metadata"
import {container} from "tsyringe";
import {ExerciseController} from "./controllers/exercise/exercise-controller";
import {TestExerciseController} from "./controllers/exercise/test-exercise-controller";
import {ExerciseStore} from "./stores/exercise-store";
import {QuestionStore} from "./stores/question-store";
import i18next from "i18next";
import {initReactI18next} from "react-i18next";
import {SurveyController} from "./controllers/exercise/survey-controller";
import {ExerciseSettingsController} from "./controllers/exercise/exercise-settings";

// init DI container
container.register(ExerciseController, { 
    useFactory: () => (new URLSearchParams(window.location.search).get('sandbox') ?? null) !== null
        ? new TestExerciseController() 
        : new ExerciseController()
});
container.register(QuestionStore, QuestionStore);
container.registerSingleton(ExerciseStore);
container.registerSingleton(SurveyController);
container.registerSingleton(ExerciseSettingsController);

// init localisation
const resources = {
    EN: {
        translation: {
            question_header: "Question #{{questionNumber}}",
            language_header: "Language",
            signedin_as_header: "Signed in as",
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
            issolved_feeback: "Solved",
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
            exercisesettings_survey: "Survey",
            exercisesettings_tags: "Tags",
            exercisesettings_commonConcepts: "Common concepts",
            exercisesettings_commonLaws: "Common laws",
            exercisesettings_stages: "Stage",
            exercisesettings_stageN: "Stage #{{stageNumber}}",
            exercisesettings_stageN_qnumber: "Number of questions",
            exercisesettings_stageN_concepts: "Concepts",
            exercisesettings_stageN_laws: "Laws",
            exercisesettings_addStage: "Add stage",
            exercisesettings_save: "Save",
            exercisesettings_saveNopen: "Save & Open",
            exercisesettings_open: "Open",
            exercisesettings_genDebugAtt: "Generate debug attempt",            
            exercisesettings_optDenied: "Denied",
            exercisesettings_optAllowed: "Allowed",
            exercisesettings_optTarget: "Target",
            exercisesettings_questionsInBank: "Qustions in bank",

            survey_sendresults: "Send survey results",
        },
    },
    RU: {
        translation: {
            question_header: "Вопрос #{{questionNumber}}",
            language_header: "Язык",
            signedin_as_header: "Пользователь",
            nextCorrectAnswerBtn: "Я в замешательстве, подскажи следующий корректный шаг",
            generateNextQuestion_nextQuestion: "Следующий вопрос",
            generateNextQuestion_warning: "Предупреждение",
            generateNextQuestion_continueAttempt: "Продолжить попытку",
            generateNextQuestion_modalMessage1: "Рекомендуется переходить к следующему вопросу только после решения всех предыдущих.",
            generateNextQuestion_modalMessage2: "Вы действительно хотите перейти к следующему вопросу?",
            grade_feeback: "Оценка",
            correctsteps_feeback: "Правильных шагов",
            stepswitherrors_feeback: "Шагов с ошибками",
            stepsleft_feeback: "Шагов осталось",
            issolved_feeback: "Задача решена",
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
            exercisesettings_survey: "Опрос",
            exercisesettings_tags: "Теги",
            exercisesettings_commonConcepts: "Общие концепты",
            exercisesettings_commonLaws: "Общие законы",
            exercisesettings_stages: "Стадии упражнения",
            exercisesettings_stageN: "Стадия #{{stageNumber}}",
            exercisesettings_stageN_qnumber: "Количество вопросов",
            exercisesettings_stageN_concepts: "Концепты",
            exercisesettings_stageN_laws: "Законы",
            exercisesettings_addStage: "Добавить стадию",
            exercisesettings_save: "Сохранить",
            exercisesettings_saveNopen: "Сохранить & Открыть",
            exercisesettings_open: "Открыть",
            exercisesettings_genDebugAtt: "Создать отладочную попытку",
            exercisesettings_optDenied: "Запрет",
            exercisesettings_optAllowed: "Разреш.",
            exercisesettings_optTarget: "Цель",
            exercisesettings_questionsInBank: "Вопросов в банке задач",

            survey_sendresults: "Отправить результаты опроса",
        },
    },
    PL: {
        translation: {
            question_header: "Pytanie #{{questionNumber}}",
            language_header: "Język",
            signedin_as_header: "Zalogowany jako",
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
            exercisesettings_survey: "Survey",
            exercisesettings_tags: "Tags",
            exercisesettings_commonConcepts: "Common concepts",
            exercisesettings_commonLaws: "Common laws",
            exercisesettings_stages: "Stage",
            exercisesettings_stageN: "Stage #{{stageNumber}}",
            exercisesettings_stageN_qnumber: "Number of questions",
            exercisesettings_stageN_concepts: "Concepts",
            exercisesettings_stageN_laws: "Laws",
            exercisesettings_addStage: "Add stage",
            exercisesettings_save: "Save",
            exercisesettings_saveNopen: "Save & Open",
            exercisesettings_open: "Open",
            exercisesettings_genDebugAtt: "Generate debug attempt",
            exercisesettings_optDenied: "Denied",
            exercisesettings_optAllowed: "Allowed",
            exercisesettings_optTarget: "Target",
            exercisesettings_questionsInBank: "Qustions in bank",

            survey_sendresults: "Send survey results",
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
