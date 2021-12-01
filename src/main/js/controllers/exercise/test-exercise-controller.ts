import { injectable } from "tsyringe";
import { ExerciseAttempt } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem } from "../../types/exercise-statistics";
import { Feedback } from "../../types/feedback";
import { Interaction } from "../../types/interaction";
import { Question } from "../../types/question";
import { SessionInfo } from "../../types/session-info";
import { PromiseEither } from "../../utils/ajax";
import { IExerciseController } from "./exercise-controller";
import * as E from "fp-ts/lib/Either";
import { SupplementaryQuestionRequest } from "../../types/supplementary-question-request";
import { RequestError } from "../../types/request-error";
import { delayPromise } from "../../utils/helpers";

@injectable()
export class TestExerciseController implements IExerciseController {
    async loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        console.log(`loadSessionInfo`);
        await delayPromise(1000);
        return E.right({
            sessionId: 'test_session',
            exercise: {
                id: -1,
                options: {
                    correctAnswerGenerationEnabled: true,
                    newQuestionGenerationEnabled: true,
                    supplementaryQuestionsEnabled: true,
                },
            },
            language: "EN",
            user: {
                id: 999999,
                displayName: 'front user',
                email: 'test@mail.ru',
                roles: ['ADMIN'],
            },            
        });
    }
    async getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, "" | ExerciseAttempt | null | undefined> {
        console.log(`getExistingExerciseAttempt?exerciseId=${exerciseId}`);
        return E.right("");
    }
    async createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        console.log(`createExerciseAttempt?exerciseId=${exerciseId}`);
        await delayPromise(3000);
        return E.right({
            attemptId: -1,
            exerciseId: -1,
            questionIds: [1, 2, 3, 4, 5, 6, 7],
        });
    }
    async getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        console.log(`getQuestion?questionId=${questionId}`);
        await delayPromise(3000);
        let result: Question | undefined;
        if (questionId === 1) {
            result = {
                type: 'SINGLE_CHOICE',
                attemptId: -1,
                questionId: 1,
                text: 'question text',
                answers: [
                    { id: 0, text: 'answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                    { id: 1, text: 'answer2answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                    { id: 2, text: 'answer2answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: false,
                    showSupplementaryQuestions: true,
                    displayMode: 'radio',              
                }
            }
        }
        if (questionId === 2) {
            result = {
                type: 'MULTI_CHOICE',
                attemptId: -1,
                questionId: 2,
                text: 'question text',
                answers: [
                    { id: 0, text: 'answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                    { id: 1, text: 'answer2answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                    { id: 2, text: 'answer2answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: false,
                    showSupplementaryQuestions: true,
                    displayMode: 'switch',              
                }
            }
        }

        if (questionId === 3) {
            result = {
                type: 'SINGLE_CHOICE',
                attemptId: -1,
                questionId: 3,
                text: 'question text with <span id="answer_0">select1</span> and <span id="answer_1">select2</span>',
                answers: [],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
                    showSupplementaryQuestions: true,
                    displayMode: 'radio',              
                }
            }
        }
        if (questionId === 4) {
            result = {
                type: 'MULTI_CHOICE',
                attemptId: -1,
                questionId: 4,
                text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>',
                answers: [],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
                    showSupplementaryQuestions: true,
                    displayMode: 'switch',              
                }
            }
        }
        if (questionId === 5) {
            result = {
                type: 'MATCHING',
                attemptId: -1,
                questionId: 5,
                text: 'question text ',
                answers: [
                    {
                        id: 0,
                        text: 'test1'
                    },
                    {
                        id: 1,
                        text: 'test2'
                    },
                    {
                        id: 3,
                        text: 'test3'
                    },
                ],
                groups: [
                    {
                        id: 0,
                        text: '<div style="width:70px; height: 40px;">group1<div/>'
                    },
                    {
                        id: 1,
                        text: '<div style="width:50px;height: 100px;">group2 group2 group2 group2<div/>'
                    },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: false,
                    showSupplementaryQuestions: true,
                    displayMode: 'dragNdrop',
                    multipleSelectionEnabled: true,
                    dropzoneStyle: '{ "display": "inline-block", "minHeight": "40px", "minWidth": "80px" }',
                    dropzoneHtml: 'drop',
                    draggableStyle: '{ "padding": "10px", "border": "5px solid", "borderRadius": "5px", "borderColor": "black", "backgroundColor": "white" }',
                }
            } 
        }

        if (questionId === 6) {
            result = {
                type: 'MATCHING',
                attemptId: -1,
                questionId: 6,
                text: 'question text with <span id="answer_0">drop</span> and <span id="answer_1">drop</span>',
                answers: [],
                groups: [
                    {
                        id: 0,
                        text: '<div style="width:70px; height: 40px;">group1<div/>'
                    },
                    {
                        id: 1,
                        text: '<div style="width:50px;height: 100px;">group2 group2 group2 group2<div/>'
                    },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
                    showSupplementaryQuestions: true,
                    displayMode: 'dragNdrop',
                    multipleSelectionEnabled: false,
                    dropzoneStyle: '{ "display": "inline-block", "minHeight": "40px", "minWidth": "80px" }',
                    dropzoneHtml: 'drop',
                    draggableStyle: '{ "padding": "10px", "border": "5px solid", "borderRadius": "5px", "borderColor": "black", "backgroundColor": "white" }',
                }
            } 
        }

        if (questionId === 7) {
            result = {
                type: 'MULTI_CHOICE',
                attemptId: -1,
                questionId: 7,
                text: `question text with <span id="answer_0"></span> and <span id="answer_1"></span>`,
                answers: [],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
                    showSupplementaryQuestions: true,
                    displayMode: 'dragNdrop',
                    dropzoneStyle: '{ "display": "inline-block", "height": "20px", "width": "20px" }',
                    dropzoneHtml: '',
                    draggableStyle: '{ "height": "20px", "width": "20px" }',
                }
            }
        }


        if (result)
            return E.right(result);
        return E.left({ message: "No such question" });
    }
    async generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        console.log(`generateQuestion?attemptId=${attemptId}`);
        await delayPromise(3000);
        return E.left({ message:"Method not implemented."});
    }
    async generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, Question> {
        console.log(`generateSupplementaryQuestion`, questionRequest);
        await delayPromise(3000);
        return E.left({ message:"Method not implemented."});
    }
    async generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        console.log(`generateNextCorrectAnswer?questionId=${questionId}`);
        await delayPromise(3000);
        return E.left({ message:"Method not implemented."});
    }
    async addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        console.log('addQuestionAnswer', interaction);
        await delayPromise(3000);
        return E.left({ message:"Method not implemented."});
    }
    async getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        console.log(`getExerciseStatistics?exerciseId=${exerciseId}`);
        return E.left({ message:"Method not implemented."});
    }
    async getExercises(): PromiseEither<RequestError, number[]> {
        console.log(`getExercises`);
        return E.left({ message: "Method not implemented."});;
    }
}
