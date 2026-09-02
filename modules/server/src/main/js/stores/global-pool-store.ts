import { makeAutoObservable, runInAction } from 'mobx';
import * as E from 'fp-ts/lib/Either';
import { courseController, exerciseSettingsController } from '../controllers';
import { ExerciseListItem, ExerciseListPermissions, noExerciseListPermissions } from '../types/exercise-settings';
import { RequestError } from '../types/request-error';

export type ImportMode = 'INHERIT' | 'CLONE';

export class GlobalPoolStore {
    exercises: ExerciseListItem[] = [];
    permissions: ExerciseListPermissions = noExerciseListPermissions;
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async loadGlobalPool() {
        runInAction(() => { this.loadStatus = 'LOADING'; this.error = null; });
        const r = await exerciseSettingsController.listExercises(null);
        runInAction(() => {
            if (E.isLeft(r)) {
                this.error = r.left;
                this.loadStatus = 'FAILED';
                return;
            }
            this.exercises = r.right.exercises;
            this.permissions = r.right.permissions;
            this.loadStatus = 'LOADED';
        });
    }

    async importToCourse(exerciseId: number, targetCourseId: number, mode: ImportMode): Promise<boolean> {
        if (mode === 'INHERIT') {
            const r = await courseController.addExerciseToCourse(exerciseId, targetCourseId);
            return E.isRight(r);
        }
        const r = await exerciseSettingsController.cloneExercise(exerciseId, targetCourseId);
        return E.isRight(r);
    }
}
