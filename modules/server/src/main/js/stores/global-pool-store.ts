import { makeAutoObservable } from 'mobx';
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
        this.loadStatus = 'LOADING';
        this.error = null;

        const r = await exerciseSettingsController.listExercises(null);
        if (E.isLeft(r)) {
            this.error = r.left;
            this.loadStatus = 'FAILED';
            return;
        }

        this.exercises = r.right.exercises;
        this.permissions = r.right.permissions;
        this.loadStatus = 'LOADED';
    }

    /** Returns the id of the exercise now in the course (the pool exercise itself or its clone), or null on failure. */
    async importToCourse(exerciseId: number, targetCourseId: number, mode: ImportMode): Promise<number | null> {
        if (mode === 'INHERIT') {
            const r = await courseController.addExerciseToCourse(exerciseId, targetCourseId);
            return E.isRight(r) ? exerciseId : null;
        }
        const r = await exerciseSettingsController.cloneExercise(exerciseId, targetCourseId);
        return E.isRight(r) ? r.right : null;
    }
}
