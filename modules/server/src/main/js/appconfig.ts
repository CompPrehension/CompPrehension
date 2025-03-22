
/** url or web api application */
export const API_URL = process.env.NODE_ENV === 'development' ? "https://localhost:8433" : (window.location.protocol + '//' + window.location.host);
