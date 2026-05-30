// Roles whose holders may modify exercises (mirrors backend Role permissions:
// GLOBAL_ADMIN / EDUCATION_RESOURCE_ADMIN / TEACHER hold CREATE/EDIT/DELETE_EXERCISE).
// ASSISTANT (Moodle Non-editing teacher) is view+solve only and is intentionally excluded.
// Used to gate editing UI; the server still enforces every action.
export const EDITOR_ROLES = ['GLOBAL_ADMIN', 'EDUCATION_RESOURCE_ADMIN', 'TEACHER'];

export const canEditExercises = (roles: string[]): boolean => roles.some(r => EDITOR_ROLES.includes(r));
