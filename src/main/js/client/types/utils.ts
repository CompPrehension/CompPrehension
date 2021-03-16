
/**
 * Merges type intersection to simple type
 * @example
 * type test = MergeIntersections<{ first: number } & { second: number }> 
 * // type test = { first: number, second: number }
 */
export type MergeIntersections<T> = 
 T extends object 
 ? { [K in keyof T]: T[K] }
 : T

/**
 * Recursive merge type intersection to simple type
 * @example
 * type test = MergeIntersections<{ first: number } & { second: number }> 
 * // type test = { first: number, second: number }
 */
export type MergeIntersectionsDeep<T> = 
  T extends object 
    ? { [K in keyof T]: MergeIntersections<T[K]> }
    : T
