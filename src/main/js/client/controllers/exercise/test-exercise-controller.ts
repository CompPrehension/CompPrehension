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
        return E.right("");
    }
    async createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return E.right({
            attemptId: -1,
            exerciseId: -1,
            questionIds: [1, 2],
        });
    }
    async getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        let result: Question | undefined;
        if (questionId === 1) {
            result = {
                type: 'SINGLE_CHOICE',
                attemptId: -1,
                questionId: -1,
                text: 'question text',
                answers: [
                    { id: 0, text: 'answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                    { id: 1, text: 'answer2answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ' },
                ],
                responses: [],
                feedback: null,
                options: {
                    requireContext: false,
                    displayMode: 'switch',              
                }
            }
        }
        if (questionId === 2) {
            result = {
                type: 'SINGLE_CHOICE',
                attemptId: -1,
                questionId: -1,
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

        if (result)
            return E.right(result);
        return E.left({ message: "No such question" });
    }
    generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        throw new Error("Method not implemented.");
    }
    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        throw new Error("Method not implemented.");
    }
    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        throw new Error("Method not implemented.");
    }
    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        throw new Error("Method not implemented.");
    }
}
