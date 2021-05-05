import { injectable } from "tsyringe";
import { ExerciseAttempt } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem } from "../../types/exercise-statistics";
import { Feedback } from "../../types/feedback";
import { Interaction } from "../../types/interaction";
import { Question } from "../../types/question";
import { SessionInfo } from "../../types/session-info";
import { PromiseEither, RequestError } from "../../utils/ajax";
import { IExerciseController } from "./exercise-controller";
import * as E from "fp-ts/lib/Either";

@injectable()
export class TestExerciseController implements IExerciseController {
    async loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        console.log(`loadSessionInfo`);
        return E.right({
            sessionId: 'test_session',
            exerciseId: -1,
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
        return E.right({
            attemptId: -1,
            exerciseId: -1,
            questionIds: [1, 2, 3, 4, 5, 6],
        });
    }
    async getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        console.log(`getQuestion?questionId=${questionId}`);
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
                    displayMode: 'switch',              
                }
            }
        }

        if (questionId === 3) {
            result = {
                type: 'SINGLE_CHOICE',
                attemptId: -1,
                questionId: 3,
                text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>',
                answers: [],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
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
                        text: '<div style="width:50px;">group2 group2 group2 group2<div/>'
                    },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: false,
                    displayMode: 'dragNdrop',
                    multipleSelectionEnabled: true,
                    dropzoneStyle: '{ "display": "inline-block", "minHeight": "40px", "minWidth": "80px" }',
                }
            } 
        }

        if (questionId === 6) {
            result = {
                type: 'MATCHING',
                attemptId: -1,
                questionId: 6,
                text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>',
                answers: [],
                groups: [
                    {
                        id: 0,
                        text: '<div style="width:70px; height: 40px;">group1<div/>'
                    },
                    {
                        id: 1,
                        text: '<div style="width:50px;">group2 group2 group2 group2<div/>'
                    },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: true,
                    displayMode: 'dragNdrop',
                    multipleSelectionEnabled: true,
                    dropzoneStyle: '{ "display": "inline-block", "minHeight": "40px", "minWidth": "80px" }',
                }
            } 
        }


        if (result)
            return E.right(result);
        return E.left({ message: "No such question" });
    }
    async generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        console.log(`generateQuestion?attemptId=${attemptId}`);
        return E.left({ message:"Method not implemented."});
    }
    async generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        console.log(`generateNextCorrectAnswer?questionId=${questionId}`);
        return E.left({ message:"Method not implemented."});
    }
    async addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        console.log('addQuestionAnswer', interaction);
        return E.left({ message:"Method not implemented."});
    }
    async getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        console.log(`getExerciseStatistics?exerciseId=${exerciseId}`);
        return E.left({ message:"Method not implemented."});
    }
}
