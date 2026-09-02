import { makeAutoObservable, runInAction } from 'mobx';
import * as E from 'fp-ts/lib/Either';
import { courseController } from '../controllers';
import { CourseDto } from '../types/course';
import { RequestError } from '../types/request-error';

export class CoursesStore {
    courses: CourseDto[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async loadMyCourses() {
        runInAction(() => { this.loadStatus = 'LOADING'; this.error = null; });
        const r = await courseController.getMyCourses();
        runInAction(() => {
            if (E.isLeft(r)) {
                this.error = r.left;
                this.loadStatus = 'FAILED';
                return;
            }
            this.courses = r.right;
            this.loadStatus = 'LOADED';
        });
    }
}
