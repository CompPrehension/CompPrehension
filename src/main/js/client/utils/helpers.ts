import * as io from 'io-ts'

export function TOptionalRequestResult<T>(type: io.Type<T>, name?: string): io.Type<T | null | undefined | ''> {
    return io.union([type, io.null, io.undefined, io.literal('')], name ?? type.name);
}

export function notNullOrUndefinded<T>(value: T | null | undefined): value is T {
    return value !== null && value !== undefined;
}
