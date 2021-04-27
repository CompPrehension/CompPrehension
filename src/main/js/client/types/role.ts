import * as io from 'io-ts'

export type Role = "STUDENT" | "TEACHER" | "ADMIN"
export const TRole: io.Type<Role> = io.keyof({
    "STUDENT": null,
    "TEACHER": null,
    "ADMIN": null,
}, 'Role')
