import { Fetcher } from "fetcher-ts";
import { Either, left, right } from "fp-ts/lib/Either";
import { fst } from "fp-ts/lib/Tuple";

type RequestError = {
    error?: string,
    message: string,
    path?: string,
    status?: number,
    timestamp?: string,
    trace?: string,
}

export function ajaxGet<T>(url: string) : Promise<Either<RequestError, T>> {
    console.log(`ajax get: ${url}`);

    type FetcherResults = 
        | { code: 200, payload: T } 
        | { code: 500, payload: RequestError };
    const fetcher = new Fetcher<FetcherResults, Either<RequestError, T>>(url)
        .handle(200, data => (console.log(data), right(data)))
        .handle(500, error => (console.error(error), left(error)))
        .discardRest(() => (console.error("Unhandled request error"), left({ message: "Unhandled request error" })));
    return fetcher.run().then(fst);
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
    type FetcherResults = 
        | { code: 200, payload: T } 
        | { code: 500, payload: RequestError };
    const fetcher = new Fetcher<FetcherResults, Either<RequestError, T>>(url, params)
        .handle(200, data => (console.log(data), right(data)))
        .handle(500, error => (console.error(error), left(error)))
        .discardRest(() => (console.error("Unhandled request error"), left({ message: "Unhandled request error" })));
    return fetcher.run().then(fst);
}
