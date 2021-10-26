import "reflect-metadata"
import { container, Lifecycle } from "tsyringe";
import { ExerciseController, IExerciseController } from "./controllers/exercise/exercise-controller";
import { TestExerciseController } from "./controllers/exercise/test-exercise-controller";
import { ExerciseStore } from "./stores/exercise-store";
import { QuestionStore } from "./stores/question-store";
import i18next from "i18next";
import { initReactI18next } from "react-i18next";

// init DI container
container.register(ExerciseController, { 
    useFactory: () => (new URLSearchParams(window.location.search).get('sandbox') ?? null) !== null
        ? new TestExerciseController() 
        : new ExerciseController()
});
container.register(QuestionStore, QuestionStore);
container.registerSingleton(ExerciseStore);

// init localisation
const resources = {
    EN: {
        translation: {
            question_header: "Question",
            language_header: "Language",
            signedin_as_header: "Signed in as",
            nextCorrectAnswerBtn: "I'm confused, tell me the next correct step",
            generateNextQuestion_nextQuestion: "Next question",
            generateNextQuestion_warning: "Warning",
            generateNextQuestion_continueAttempt: "Continue attempt",
            generateNextQuestion_modalMessage1: "It is recommended to move on to the next question only after solving all the previous ones.",
            generateNextQuestion_modalMessage2: "Are you sure you want to move on to the next question?",
            generateSupQuestion_gotit: "Got it",
            generateSupQuestion_details: "More details",
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
        },
    },
    RU: {
        translation: {
            question_header: "Вопрос",
            language_header: "Язык",
            signedin_as_header: "Пользователь",
            nextCorrectAnswerBtn: "Я в замешательстве, подскажи следующий корректный шаг",
            generateNextQuestion_nextQuestion: "Следующий вопрос",
            generateNextQuestion_warning: "Предупреждение",
            generateNextQuestion_continueAttempt: "Продолжить попытку",
            generateNextQuestion_modalMessage1: "Рекомендуется переходить к следующему вопросу только после решения всех предыдущих.",
            generateNextQuestion_modalMessage2: "Вы действительно хотите перейти к следующему вопросу?",
            generateSupQuestion_gotit: "Понятно",
            generateSupQuestion_details: "Разобраться подробнее",
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
        },
    },
    PL: {
        translation: {
            question_header: "Pytanie",
            language_header: "Język",
            signedin_as_header: "Zalogowany jako",
            nextCorrectAnswerBtn: "Nie wiem co robić dalej, podpowiedz mi następny poprawny krok",
            generateNextQuestion_nextQuestion: "Następne pytanie",
            generateNextQuestion_warning: "Ostrzeżenie",
            generateNextQuestion_continueAttempt: "Kontynuuj podejście",
            generateNextQuestion_modalMessage1: "Zaleca się przejście do następnego pytania dopiero po rozwiązaniu wszystkich poprzednich.",
            generateNextQuestion_modalMessage2: "Czy na pewno chcesz przejść do następnego pytania?",
            generateSupQuestion_gotit: "Oczywiście!",
            generateSupQuestion_details: "Zobacz szczegóły",
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
