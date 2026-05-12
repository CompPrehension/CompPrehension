import * as io from 'io-ts';

export const TCourseDto = io.type({
    id: io.number,
    name: io.string,
    educationResourceId: io.number,
    educationResourceName: io.string,
});

export type CourseDto = io.TypeOf<typeof TCourseDto>;
