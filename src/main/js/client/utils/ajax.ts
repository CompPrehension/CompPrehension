import { Either } from "fp-ts/lib/Either";
import * as TE from "fp-ts/lib/TaskEither";

type RequestError = {
    error: string,
    message: string,
    path: string,
    status: number,
    timestamp: string,
    trace: string,
}


export function ajaxGet<T>(url: string) : Promise<Either<RequestError, T>> {
    console.log(`ajax get: ${url}`);
    const lazyPromise = async () => {
        const resp = await fetch(url);
        const json = await resp.json();
        if (resp.ok) {
            console.log(json);
            return json;
        }
        console.error(json);
        throw json;
    };
    return TE.tryCatch(lazyPromise, rej => rej as RequestError)();
}

export function ajaxPost<T>(url: string, body: object) : Promise<Either<RequestError, T>> {
    const params = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'                
        },
        body: JSON.stringify(body),
    };

    console.log(`ajax post: ${url}`, params.body);
    const lazyPromise = async () => {
        const resp = await fetch(url, params);
        const json = await resp.json();
        if (resp.ok) {
            console.log(json);
            return json;
        }
        console.error(json);
        throw json;
    };
    return TE.tryCatch(lazyPromise, rej => rej as RequestError)();
}
