import { Fetcher } from "fetcher-ts";
import { Either, left, right } from "fp-ts/lib/Either";
import * as O from "fp-ts/lib/Option";
import * as io from "io-ts";
import { PathReporter } from 'io-ts/lib/PathReporter';
import { MergeIntersections } from "../types/utils";

type RequestError = {
    error?: string,
    message: string,
    path?: string,
    status?: number,
    timestamp?: string,
    trace?: string,
}

/**
 * Do GET request
 * @param {string} url Target url
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export function ajaxGet<T>(url: string, validator?: io.Type<T, T, unknown>) : Promise<Either<RequestError, T>> {
    console.log(`ajax get: ${url}`);

    type FetcherResults = 
        | { code: 200, payload: T } 
        | { code: 500, payload: RequestError };
    const fetcher = new Fetcher<FetcherResults, Either<RequestError, T>>(url)
        .handle(200, data => (console.log(data), right(data)), validator)
        .handle(500, error => (console.error(error), left(error)))
        .discardRest(() => (console.error("Unhandled request error"), left({ message: "Unhandled request error" })));    
        
    const result = fetcher.run()
        .then(([data, errors]) => {
            if (O.isSome(errors)) {                
                const error = { message: `Types inconsistency: ${PathReporter.report(left(errors.value)).join("\n")}` };
                return (console.error(error), left(error));     
            }
            return data;
        });
    return result;
}

/**
 * Do POST request
 * @param {string} url Target url
 * @param {object} body Request body
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export function ajaxPost<T>(url: string, body: object, validator?: io.Type<T, T, unknown>) : Promise<Either<RequestError, T>> {
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
        .handle(200, data => (console.log(data), right(data)), validator)
        .handle(500, error => (console.error(error), left(error)))
        .discardRest(() => (console.error("Unhandled request error"), left({ message: "Unhandled request error" })));
    
    const result = fetcher.run()
        .then(([data, errors]) => {
            if (O.isSome(errors)) {
                const error = { message: `Types inconsistency: ${PathReporter.report(left(errors.value)).join("\n")}` };
                return (console.error(error), left(error));    
            }
            return data;
        });
    return result;
}
