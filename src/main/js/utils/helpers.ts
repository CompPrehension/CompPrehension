import * as io from 'io-ts'

export function TOptionalRequestResult<T>(type: io.Type<T>, name?: string): io.Type<T | null | undefined | ''> {
    return io.union([type, io.null, io.undefined, io.literal('')], name ?? type.name);
}

export function delayPromise(timeout: number): Promise<void> {
    return new Promise(resolve => 
        setTimeout(() => resolve(), timeout));
}

export function renderIfNotNull<T>(value: T | null | undefined, render: (value: T) => React.ReactNode[] | React.ReactNode | null) {
    if (notNulAndUndefinded(value)) {
        return render(value);
    }
    return null;
}

export function notNulAndUndefinded<T>(value: T | null | undefined): value is T {
    return value !== null && value !== undefined;
}

export function isNullOrUndefinded<T>(value: T | null | undefined): value is null | undefined {
    return value === null || value === undefined;
}
