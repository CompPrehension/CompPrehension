


export function ajaxGet<T>(url: string) : Promise<T> {
    return fetch(url)
        .then(v => v.json())
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

    return fetch(url, params)
        .then(v => v.json())
        .catch(e => console.error(e));
}
