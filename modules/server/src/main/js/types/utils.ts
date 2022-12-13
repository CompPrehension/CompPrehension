import { NonEmptyArray, fromArray, map } from 'fp-ts/lib/NonEmptyArray'
import { isNonEmpty } from 'fp-ts/lib/Array'
import { pipe } from 'fp-ts/lib/function'
import { chain } from 'fp-ts/lib/Either'
import { isNone } from 'fp-ts/lib/Option'
import * as io from 'io-ts'

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
    ? { [K in keyof T]: MergeIntersectionsDeep<T[K]> }
    : T

export type KeysWithValsOfType<T,V> = keyof { [ P in keyof T as T[P] extends V ? P : never ] : P };


/**
 * @since 0.5.0
 */
 export interface NonEmptyArrayC<C extends io.Mixed>
  extends io.Type<NonEmptyArray<io.TypeOf<C>>, NonEmptyArray<io.OutputOf<C>>, unknown> {}

/**
* @since 0.5.0
*/
export function nonEmptyArray<C extends io.Mixed>(
 codec: C,
 name: string = `NonEmptyArray<${codec.name}>`
): NonEmptyArrayC<C> {
 const arr = io.array(codec)
 return new io.Type(
   name,
   (u): u is NonEmptyArray<io.TypeOf<C>> => arr.is(u) && isNonEmpty(u),
   (u, c) =>
     pipe(
       arr.validate(u, c),
       chain(as => {
         const onea = fromArray(as)
         return isNone(onea) ? io.failure(u, c) : io.success(onea.value)
       })
     ),
   map(codec.encode)
 )
}

export function getUrlParameterByName(name: string) : string | null {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(name);
}
