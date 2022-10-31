import { action, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import { Domain, ExerciseCard, ExerciseListItem } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";


@injectable()
export class ExerciseSettingsStore {
    @observable exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';
    @observable exercises: ExerciseListItem[] | null = null;
    @observable domains: Domain[] | null = null;
    @observable backends: string[] | null = null;
    @observable strategies: string[] | null = null;
    @observable currentCard: ExerciseCard | null = null;

    constructor(@inject(ExerciseSettingsController) private readonly exerciseSettingsController: ExerciseSettingsController) {
        makeObservable(this);
    }


    async loadExercises() {
        if (this.exercisesLoadStatus === 'LOADED' || this.exercisesLoadStatus === 'LOADING')
            return;

        runInAction(() => this.exercisesLoadStatus = 'LOADING');
        const rawExercises = await this.exerciseSettingsController.getAllExercises();
        const domains      = await this.exerciseSettingsController.getDomains();
        const backends     = await this.exerciseSettingsController.getBackends();
        const strategies   = await this.exerciseSettingsController.getStrategies();

        if (E.isRight(rawExercises) && E.isRight(domains) && E.isRight(backends) && E.isRight(strategies)) {
            runInAction(() => {
                this.exercises = rawExercises.right;
                this.domains = domains.right;
                this.backends = backends.right;
                this.strategies = strategies.right;
            });
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }

    async loadExercise(exerciseId : number) {
        if (this.exercisesLoadStatus !== 'LOADED')
            throw new Error("Exercises must be loaded first");
        
        const rawExercise = await this.exerciseSettingsController.getExercise(exerciseId);
        if (E.isRight(rawExercise)) {
            runInAction(() => {
                this.currentCard = rawExercise.right;
            });
        }
    }


    async saveCard() {
        console.log(this.currentCard)
    }

    @action
    setCardName(name: string) {
        if (!this.currentCard)
            return;
        this.currentCard.name = name;
    }
    @action
    setCardDomain(domainId: string) {
        if (!this.currentCard)
            return;
        this.currentCard.domainId = domainId;
    }
    @action
    setCardStrategy(strategyId: string) {
        if (!this.currentCard)
            return;
        this.currentCard.strategyId = strategyId;
    }
    @action
    setCardQuestionComplexity(rawComplexity: string) {
        if (!this.currentCard)
            return;
        
        const complexity = Number.parseInt(rawComplexity);        
        this.currentCard.complexity = complexity / 100.0;
    }
    @action
    setCardAnswerLength(rawLength: string) {
        if (!this.currentCard)
            return;
        const length = Number.parseInt(rawLength);
        this.currentCard.numberOfQuestions = length;
    }
}