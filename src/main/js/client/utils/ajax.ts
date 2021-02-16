import { Either, left, right } from "fp-ts/lib/Either";

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
    return new Promise((res) => {
        fetch(url)
            .then(async resp => {
                var json = await resp.json();                
                if (resp.ok) {
                    console.log(json);
                    return json;
                }
                console.error(json);
                throw json;
            })
            .then(v => res(right(v)))
            .catch(e => res(left(e)));
    });
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
    return new Promise((res) => {        
        fetch(url, params)
            .then(async resp => {
                var json = await resp.json();               
                if (resp.ok) {
                    console.log(json);
                    return json;
                }
                console.error(json);
                throw json;
            })
            .then(v => res(right(v)))
            .catch(e => res(left(e)));
    });
}
