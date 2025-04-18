import * as io from 'io-ts'

export const TOptionalRequestResultV = io.union([io.null, io.undefined, io.literal('')]);
export function TOptionalRequestResult<T>(type: io.Type<T>, name?: string): io.Type<T | null | undefined | ''> {
    return io.union([type, io.null, io.undefined, io.literal('')], name ?? type.name);
}
export function TNullOrUndefined<T>(type: io.Type<T>, name?: string): io.Type<T | null | undefined> {
    return io.union([type, io.null, io.undefined], name ?? type.name);
}

export function delayPromise(timeout: number): Promise<void> {
    return new Promise(resolve => 
        setTimeout(() => resolve(), timeout));
}

export function renderIfNotNull<T>(value: T | null | undefined, render: (value: T) => React.ReactNode[] | React.ReactNode | null) {
    if (!isNullOrUndefined(value)) {
        return render(value);
    }
    return null;
}

export function isNullOrUndefined<T>(value: T | null | undefined): value is null | undefined {
    return value === null || value === undefined;
}

export function getRandomInt(min: number, max: number) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
