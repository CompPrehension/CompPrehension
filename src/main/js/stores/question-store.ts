import { action, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import { Feedback } from "../types/feedback";
import { Question } from "../types/question";
import * as E from "fp-ts/lib/Either";
import { Interaction } from "../types/interaction";
import { SupplementaryQuestionRequest } from "../types/supplementary-question-request";
import * as NEArray from 'fp-ts/lib/NonEmptyArray'
import { pipe } from "fp-ts/lib/function";
import * as O from 'fp-ts/lib/Option'

/**
 * Store question data
 */
@injectable()
export class QuestionStore {
    @observable isQuestionLoading?: boolean = false;
    @observable answersHistory: [number, number][] = [];
    @observable isFeedbackLoading?: boolean = false;
    @observable isFeedbackVisible: boolean = true;
    @observable feedback?: Feedback = undefined;
    @observable question?: Question = undefined;

    constructor(@inject(ExerciseController) private exerciseController: IExerciseController) {
        makeObservable(this);        
    }

    private onQuestionLoaded = (question: Question) => {
        runInAction(() => {
            // add question id to answers
            if (question.options.requireContext) {
                // regex searchs all tags with id='answer_id' and prepends them with question id
                question.text = question.text.replaceAll(/(\<.*?\sid\s*?\=([\'\"]))\s*(answer_.+?\2)(.*?\>)/igm, `$1question_${question.questionId}_$3$4`)
            }
            
            this.question = question;
            this.feedback = question.feedback ?? undefined;
            this.isFeedbackVisible = true;
            this.answersHistory = question.responses ?? [];            
        });
    }
    
    loadQuestion = async (questionId: number): Promise<void> => {        
        runInAction(() => this.isQuestionLoading = true);
        const dataEither = await this.exerciseController.getQuestion(questionId);
        runInAction(() => this.isQuestionLoading = false);

        if (E.isLeft(dataEither)) {
            throw (dataEither.left);
        }
        
        this.onQuestionLoaded(dataEither.right);
    }

    generateQuestion = async (attemptId: number): Promise<void> => {        
        runInAction(() => this.isQuestionLoading = true);
        const dataEither = await this.exerciseController.generateQuestion(attemptId);
        runInAction(() => this.isQuestionLoading = false);

        if (E.isLeft(dataEither)) {
            throw dataEither.left;
        }

        this.onQuestionLoaded(dataEither.right);
    }

    generateSupplementaryQuestion = async (attemptId: number, questionId: number, violationLaws: string[]): Promise<void> => {
        const questionRequest: SupplementaryQuestionRequest = {
            exerciseAttemptId: attemptId,
            questionId: questionId,
            violationLaws: pipe(
                NEArray.fromArray(violationLaws), 
                O.getOrElse(() => ["invalid_law"] as NEArray.NonEmptyArray<string>),
            ),
        };

        runInAction(() => this.isQuestionLoading = true);
        const dataEither = await this.exerciseController.generateSupplementaryQuestion(questionRequest);
        runInAction(() => this.isQuestionLoading = false);

        if (E.isLeft(dataEither)) {
            throw dataEither.left;
        }

        if (dataEither.right || false) {
            this.onQuestionLoaded(dataEither.right);
        }
    }

    generateNextCorrectAnswer = async (): Promise<void> => {
        const { question } = this;
        if (!question) {
            throw new Error("Current question not found");
        }

        runInAction(() => this.isFeedbackLoading = true);
        const feedbackEither = await this.exerciseController.generateNextCorrectAnswer(question.questionId);
        runInAction(() => this.isFeedbackLoading = false);
        
        if (E.isLeft(feedbackEither)) {
            throw (feedbackEither.left);
        }

        const feedback = feedbackEither.right;
        runInAction(() => {            
            this.feedback = feedback;
            this.isFeedbackVisible = true;
            if (feedback && feedback.correctAnswers && this.isHistoryChanged(feedback.correctAnswers)) {
                this.answersHistory = feedback.correctAnswers;                    
            }            
        });
    }

    private sendAnswersImpl = async (attemptId: number, questionId: number, answers: [number, number][]): Promise<void> => {
        const body: Interaction = toJS({
            attemptId,
            questionId,
            answers: toJS(answers),
        })

        runInAction(() => this.isFeedbackLoading = true);
        const feedbackEither = await this.exerciseController.addQuestionAnswer(body);
        runInAction(() => this.isFeedbackLoading = false);
       
        if (E.isLeft(feedbackEither)) {
            throw (feedbackEither.left);
        }

        const feedback = feedbackEither.right;
        runInAction(() => {            
            this.feedback = feedback;
            this.isFeedbackVisible = true;
            if (feedback && feedback.correctAnswers && this.isHistoryChanged(feedback.correctAnswers)) {
                this.answersHistory = feedback.correctAnswers;                    
            }            
        });
    }
     
    @action 
    sendAnswers = async () : Promise<void> => {
        const { question, answersHistory } = this;      
        if (!question) {
            return;
        }
        await this.sendAnswersImpl(question.attemptId, question.questionId, toJS(answersHistory));        
    }

    @action 
    onAnswersChanged = async (answer: [number, number], sendAnswers: boolean = true): Promise<void> => {
        this.answersHistory.push(answer);
        if (sendAnswers) {
            await this.sendAnswers();
        }
    }

    @action
    updateAnswersHistory = async (newHistory: [number, number][], sendAnswers: boolean = true): Promise<void> => {
        this.answersHistory = [ ...newHistory ];
        if (sendAnswers) {
            await this.sendAnswers();
        }
    }

    isHistoryChanged = (newHistory: [number, number][]): boolean => {
        const { answersHistory } = this;
        return newHistory.length !== answersHistory.length || JSON.stringify([...newHistory].sort()) !== JSON.stringify([...answersHistory].sort());
    }
}
