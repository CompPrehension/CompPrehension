import type { Either } from "fp-ts/lib/Either";
import * as E from "fp-ts/lib/Either";
import * as io from "io-ts";
import { notifications } from "../stores/notifications-store";
import { RequestError } from "../types/request-error";

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
    const preparedUrl = new URL(url, window.location.origin);
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
export async function ajaxPost<T = unknown>(url: string, body: object, validator?: io.Type<T, T, unknown>, signal?: AbortSignal, payloadType?: 'json' | 'raw') : PromiseEither<RequestError, T> {
    const params: RequestInit = {
        ...commonParams,
        method: 'POST',
        body: JSON.stringify(body),
        signal,
    };
    return await ajax(url, params, validator, payloadType);
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

type ErrorBody = {
    title?: unknown, detail?: unknown, instance?: unknown,
    error?: unknown, message?: unknown, path?: unknown, timestamp?: unknown, trace?: unknown,
};

const statusTexts: Record<number, string> = {
    400: "Bad request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not found",
    405: "Method not allowed",
    409: "Conflict",
    413: "Payload too large",
    415: "Unsupported media type",
    422: "Unprocessable entity",
    429: "Too many requests",
    500: "Internal server error",
    502: "Bad gateway",
    503: "Service unavailable",
    504: "Gateway timeout",
};

const asText = (value: unknown): string | undefined =>
    typeof value === 'string' && value.trim() !== '' ? value.trim() : undefined;

function parseErrorBody(body: string): ErrorBody | undefined {
    try {
        const parsed: unknown = JSON.parse(body);
        return typeof parsed === 'object' && parsed !== null ? parsed as ErrorBody : undefined;
    } catch {
        return undefined;
    }
}

function plainTextBody(body: string): string | undefined {
    const text = asText(body);
    return text !== undefined && text.length <= 300 && !text.startsWith('<') ? text : undefined;
}

async function toRequestError(response: Response): Promise<RequestError> {
    const body = await response.text().catch(() => '');
    const parsed = parseErrorBody(body);
    const title = asText(parsed?.title) ?? asText(parsed?.error);

    return {
        status: response.status,
        message: asText(parsed?.detail)                              // ProblemDetail
            ?? asText(parsed?.message)                               // default Spring error body
            ?? title
            ?? (parsed === undefined ? plainTextBody(body) : undefined)
            ?? statusTexts[response.status]
            ?? `Request failed with status ${response.status}`,
        title,
        path: asText(parsed?.instance) ?? asText(parsed?.path),
        timestamp: asText(parsed?.timestamp),
        trace: asText(parsed?.trace),
    };
}

async function readPayload(response: Response, payloadType: 'json' | 'raw'): Promise<unknown> {
    const body = await response.text();
    if (payloadType === 'raw') {
        return body;
    }
    // an endpoint returning null writes no body at all; TOptionalRequestResult accepts ''
    return body.trim() === '' ? '' : JSON.parse(body);
}

function fail(error: RequestError): Either<RequestError, never> {
    console.error(error);
    notifications.report(error);
    return E.left(error);
}

const isAbort = (err: unknown): boolean =>
    err instanceof DOMException && err.name === 'AbortError';

async function ajax<T = unknown>(url: string, params?: RequestInit, validator?: io.Type<T, T, unknown>, payloadType?: 'json' | 'raw'): PromiseEither<RequestError, T> {
    payloadType ??= 'json';

    let response: Response;
    try {
        response = await fetch(url, params);
    } catch (err: unknown) {
        // superseded requests are a normal part of the ui lifecycle, not something to show
        if (isAbort(err)) {
            return E.left({ message: "Request aborted" });
        }
        return fail({ message: `Network error: ${err instanceof Error ? err.message : String(err)}` });
    }

    if (!response.ok) {
        // TODO a 401 means the session is gone; reloading would let the backend restart the
        // Keycloak login, but it would also throw away unsaved state - needs a decision
        return fail(await toRequestError(response));
    }

    let payload: unknown;
    try {
        payload = await readPayload(response, payloadType);
    } catch (err: unknown) {
        if (isAbort(err)) {
            return E.left({ message: "Request aborted" });
        }
        return fail({
            status: response.status,
            message: `Malformed response body: ${err instanceof Error ? err.message : String(err)}`,
        });
    }

    const decoded = validator ? validator.decode(payload) : io.success(payload as T);
    if (E.isLeft(decoded)) {
        return fail({
            status: response.status,
            message: `Type inconsistency for properties of ${validator?.name} type: ${getPaths(decoded.left).join(', ')}`,
        });
    }

    return E.right(decoded.right);
}

const getPaths = (errors: io.Errors): Array<string> => {
    return errors.map((error) =>
        error.context.map(({ key }) => key).join('.'));
}
