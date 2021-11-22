import { Fetcher } from "fetcher-ts";
import { Either, left, right } from "fp-ts/lib/Either";
import * as O from "fp-ts/lib/Option";
import * as E from "fp-ts/lib/Either";
import * as io from "io-ts";
import { pipe } from "fp-ts/lib/function";
import { RequestError } from "../types/request-error";
import { string } from "fp-ts";

export type PromiseEither<E, A> = Promise<Either<E, A>>

/**
 * Do GET request
 * @param {string} url Target url
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export async function ajaxGet<T = unknown>(url: string, validator?: io.Type<T, T, unknown>) : PromiseEither<RequestError, T> {
    console.log(`ajax get: ${url}`);
    return await ajax(url, undefined, validator);    
}

export async function ajaxGetWithParams<T = unknown>(url: string, params: Record<string, string>, validator?: io.Type<T>) : PromiseEither<RequestError, T> {
    const preparedUrl = new URL(url);
    preparedUrl.search = new URLSearchParams(params).toString();

    console.log(`ajax get: ${preparedUrl}`);
    return await ajax(preparedUrl.toString(), undefined, validator);    
}

/**
 * Do POST request
 * @param {string} url Target url
 * @param {object} body Request body
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export async function ajaxPost<T = unknown>(url: string, body: object, validator?: io.Type<T, T, unknown>) : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
    };
    console.log(`ajax post: ${url}`, body);
    return await ajax(url, params, validator);
}

async function ajax<T = unknown>(url: string, params?: RequestInit, validator?: io.Type<T, T, unknown>): PromiseEither<RequestError, T> {
    type FetcherResults = 
        | { code: 200, payload: T } 
        | { code: 500, payload: RequestError };
    const fetcher = new Fetcher<FetcherResults, Either<RequestError, T>>(url, params)
        .handle(200, data => right(data), validator)
        .handle(500, error => left(error))
        .discardRest(() => left({ message: "Unexpected server error" }));
    
    const [data, validationErrors] = await fetcher.run()
        .catch(async () => [left<RequestError, T>({ message: "Connection error"}), O.none as O.Option<io.Errors>] as const);
    E.fold(err => console.error(err), val => console.log(val))(data);
        
    if (O.isSome(validationErrors)) {    
        const error = { message: `Type inconsistency for properties of ${validator?.name} type: ${getPaths(validationErrors.value).join(', ')}` };
        return (console.error(error), left(error));      
    }    
    return data;
}

const getPaths = (errors: io.Errors): Array<string> => {
    return errors.map((error) => 
        error.context.map(({ key }) => key).join('.'));
}
