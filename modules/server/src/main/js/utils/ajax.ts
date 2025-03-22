import type { Either } from "fp-ts/lib/Either";
import * as E from "fp-ts/lib/Either";
import * as io from "io-ts";
import { RequestError } from "../types/request-error";
import { API_URL } from "../appconfig";

export type PromiseEither<E, A> = Promise<Either<E, A>>

const commonParams: RequestInit = {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
    },
    //redirect: 'manual',
}

/**
 * Do GET request
 * @param {string} url Target url
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export async function ajaxGet<T = unknown>(url: string, validator?: io.Type<T, T, unknown>, signal?: AbortSignal) : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        ...commonParams,
        signal,
    };
    return await ajax(url, params, validator);    
}

export async function ajaxGetWithParams<T = unknown>(url: string, params: Record<string, string>, validator?: io.Type<T>) : PromiseEither<RequestError, T> {
    const preparedUrl = new URL(url);
    preparedUrl.search = new URLSearchParams(params).toString();

    return await ajax(preparedUrl.toString(), commonParams, validator);    
}

/**
 * Do POST request
 * @param {string} url Target url
 * @param {object} body Request body
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
export async function ajaxPost<T = unknown>(url: string, body: object, validator?: io.Type<T, T, unknown>, signal?: AbortSignal) : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        ...commonParams,
        method: 'POST',
        body: JSON.stringify(body),
        signal,
    };
    return await ajax(url, params, validator);
}

/**
 * Do PUT request
 * @param {string} url Target url
 * @param {object} body Request body
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
 export async function ajaxPut<T = unknown>(url: string, body: object, validator?: io.Type<T, T, unknown>, signal?: AbortSignal) : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        ...commonParams,
        method: 'PUT',
        body: JSON.stringify(body),
        signal,
    };
    return await ajax(url, params, validator);
}

/**
 * Do DELETE request
 * @param {string} url Target url
 * @param {object} body Request body
 * @param {io.Type<T, T, unknown>} [validator] Optional response validator
 * @returns Pair of either RequestError or ResposeBody
 */
 export async function ajaxDelete<T = unknown>(url: string, validator?: io.Type<T, T, unknown>, signal?: AbortSignal) : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        ...commonParams,
        method: 'DELETE',
        signal,
    };
    return await ajax(url, params, validator);
}

async function ajax<T = unknown>(url: string, params?: RequestInit, validator?: io.Type<T, T, unknown>): PromiseEither<RequestError, T> {
    const result = await fetch(url, params)
        .then(async (data) => {
            if (data.ok) {                
                return { status: 'ok', payload: validator && validator.decode(await data.json()) || io.success<T>(await data.json()) } as const;
            }

            if (data.status === 401) {
                return { status: 'unauthorized' } as const;
            }

            if (data.status === 500) {
                return { status: 'server_error', payload: await data.json() as RequestError } as const;
            }

            return { status: 'unexpected', payload: { message: "Unexpected error" } as RequestError } as const
        })
        .catch((err: unknown) => ({ status: 'unexpected', payload: { message:"Unexpected error " + err } as RequestError } as const));


    if (result.status === 'ok') {
        if (E.isLeft(result.payload)) {    
            const error = { message: `Type inconsistency for properties of ${validator?.name} type: ${getPaths(result.payload.left).join(', ')}` };
            return (console.error(error), E.left(error));
        }
        return E.right(result.payload.right);
    }

    if (result.status === 'unauthorized') {
        // const authUrl = `${API_URL}/oauth2/authorization/keycloak?redirect_uri=${window.location.href}`; // TODO найти другой способ без хардкода адреса
        // console.log(`redirect to ${authUrl}`);
        // window.location.replace(authUrl);
        return E.left({ message: "Unauthorized" });
    }

    return E.left(result.payload ?? { message: "Unexpected error " });
}

const getPaths = (errors: io.Errors): Array<string> => {
    return errors.map((error) => 
        error.context.map(({ key }) => key).join('.'));
}
