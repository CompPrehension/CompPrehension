
export type RequestError = {
    /** http status of the response */
    status?: number,
    /** text message */
    message: string,
    /** short summary of the status */
    title?: string,
    /** requested path */
    path?: string,
    timestamp?: string,
    trace?: string,
}
