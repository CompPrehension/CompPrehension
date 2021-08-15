import "reflect-metadata"
import { container, Lifecycle } from "tsyringe";
import { ExerciseController, IExerciseController } from "./controllers/exercise/exercise-controller";
import { TestExerciseController } from "./controllers/exercise/test-exercise-controller";
import { ExerciseStore } from "./stores/exercise-store";
import { QuestionStore } from "./stores/question-store";
import i18next from "i18next";
import { initReactI18next } from "react-i18next";

const urlParams = new URLSearchParams(window.location.search);
const isTest = urlParams.get('sandbox') !== null && urlParams.get('sandbox') !== undefined;

// init DI container
container.register(ExerciseController, isTest ? TestExerciseController : ExerciseController);
container.register(QuestionStore, QuestionStore);
container.registerSingleton(ExerciseStore);

// init localisation
const resources = {
    EN: {
        translation: {
            question_header: "Question",
            language_header: "Language",
            signedin_as_header: "Signed in as",
            nextCorrectAnswerBtn: "Next correct answer",
            generateNextQuestionBtn: "Next question",
            generateSupQuestionBtn: "Supplementary question",
            grade_feeback: "Grade",
            correctsteps_feeback: "Correct steps",
            stepswitherrors_feeback: "Steps with errors",
            stepsleft_feeback: "Steps left",
            foundExisitingAttempt_title: "Found existing attempt",
            foundExisitingAttempt_descr: "Would you like to continue the existing attempt or start a new one",
            foundExisitingAttempt_continueattempt: "Continue",
            foundExisitingAttempt_newattempt: "New",
        },
    },
    RU: {
        translation: {
            question_header: "Вопрос",
            language_header: "Язык",
            signedin_as_header: "Пользователь",
            nextCorrectAnswerBtn: "Следующий правильный ответ",
            generateNextQuestionBtn: "Следующий вопрос",
            generateSupQuestionBtn: "Вспомогательный вопрос",
            grade_feeback: "Оценка",
            correctsteps_feeback: "Правильных шагов",
            stepswitherrors_feeback: "Шагов с ошибками",
            stepsleft_feeback: "Шагов осталось",
            foundExisitingAttempt_title: "Найдена неоконченная попытка",            
            foundExisitingAttempt_descr: "Вы хотите продолжить существующую попытку или начать новую",
            foundExisitingAttempt_continueattempt: "Продолжить",
            foundExisitingAttempt_newattempt: "Новая",
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
