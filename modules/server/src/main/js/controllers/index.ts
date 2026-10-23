import { CourseController } from './course/course-controller';
import { ExerciseController } from './exercise/exercise-controller';
import { ExerciseSettingsController } from './exercise/exercise-settings';
import { QuestionController } from './exercise/question-controller';
import { SurveyController } from './exercise/survey-controller';
import { UserController } from './exercise/user-controller';
import { DeepLinkingController } from './lti/deep-linking-controller';
import { LtiRegistrationController } from './lti/lti-registration-controller';

export const courseController = new CourseController();
export const deepLinkingController = new DeepLinkingController();
export const exerciseController = new ExerciseController();
export const ltiRegistrationController = new LtiRegistrationController();
export const exerciseSettingsController = new ExerciseSettingsController();
export const questionController = new QuestionController();
export const surveyController = new SurveyController();
export const userController = new UserController();
