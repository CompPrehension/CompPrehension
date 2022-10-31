import { action, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import { ExerciseCard, ExerciseListItem } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";


@injectable()
export class ExerciseSettingsStore {
    @observable exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';
    @observable exercises: ExerciseListItem[] | null = null;
    @observable currentCard: ExerciseCard | null = null;

    constructor(@inject(ExerciseSettingsController) private readonly exerciseSettingsController: ExerciseSettingsController) {
        makeObservable(this);
    }


    async loadExercises() {
        if (this.exercisesLoadStatus === 'LOADED' || this.exercisesLoadStatus === 'LOADING')
            return;

        runInAction(() => this.exercisesLoadStatus = 'LOADING');
        const rawExercises = await this.exerciseSettingsController.getAllExercises();
        if (E.isRight(rawExercises)) {
            runInAction(() => this.exercises = rawExercises.right);
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }

    async loadExercise(exerciseId : number) {
        if (this.exercisesLoadStatus !== 'LOADED')
            throw new Error("Exercises must be loaded first");
        
        
    }
}