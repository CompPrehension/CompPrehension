// Roles whose holders may modify exercises (mirrors backend Role permissions:
// GLOBAL_ADMIN / EDUCATION_RESOURCE_ADMIN / TEACHER / ASSISTANT hold CREATE/EDIT/DELETE_EXERCISE).
// Used to gate editing UI; the server still enforces every action.
export const EDITOR_ROLES = ['GLOBAL_ADMIN', 'EDUCATION_RESOURCE_ADMIN', 'TEACHER', 'ASSISTANT'];

export const canEditExercises = (roles: string[]): boolean => roles.some(r => EDITOR_ROLES.includes(r));
