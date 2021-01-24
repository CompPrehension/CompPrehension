


export function ajaxGet<T>(url: string) : Promise<T> {
    console.log(`ajax get: ${url}`);
    return fetch(url)
        .then(v => v.json())
        .then(v => (console.log(v), v))
        .catch(e => console.error(e));
}

export function ajaxPost<T>(url: string, body: object) : Promise<T> {
    const params = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'                
        },
        body: JSON.stringify(body),
    };

    console.log(`ajax post: ${url}`, params.body);
    return fetch(url, params)
        .then(v => v.json())
        .then(v => (console.log(v), v))
        .catch(e => console.error(e));
}
