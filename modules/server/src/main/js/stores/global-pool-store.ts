import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { ExerciseSettingsController } from '../controllers/exercise/exercise-settings';
import { CourseController } from '../controllers/course/course-controller';
import { ExerciseListItem, ExerciseListPermissions, noExerciseListPermissions } from '../types/exercise-settings';
import { RequestError } from '../types/request-error';

export type ImportMode = 'INHERIT' | 'CLONE';

@injectable()
export class GlobalPoolStore {
    exercises: ExerciseListItem[] = [];
    permissions: ExerciseListPermissions = noExerciseListPermissions;
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor(
        @inject(ExerciseSettingsController) private readonly settingsController: ExerciseSettingsController,
        @inject(CourseController) private readonly courseController: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadGlobalPool() {
        runInAction(() => { this.loadStatus = 'LOADING'; this.error = null; });
        const r = await this.settingsController.listExercises(null);
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
            const r = await this.courseController.addExerciseToCourse(exerciseId, targetCourseId);
            return E.isRight(r);
        }
        const r = await this.settingsController.cloneExercise(exerciseId, targetCourseId);
        return E.isRight(r);
    }
}
