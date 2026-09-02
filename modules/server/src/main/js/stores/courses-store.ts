import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../controllers/course/course-controller';
import { CourseDto } from '../types/course';
import { RequestError } from '../types/request-error';

@injectable()
export class CoursesStore {
    courses: CourseDto[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor(
        @inject(CourseController) private readonly api: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadMyCourses() {
        runInAction(() => { this.loadStatus = 'LOADING'; this.error = null; });
        const r = await this.api.getMyCourses();
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
