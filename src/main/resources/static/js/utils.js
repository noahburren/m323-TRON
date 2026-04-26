/**
 * Pure function composition: pipe(f, g, h)(x) === h(g(f(x)))
 * @param {...function} fns
 * @returns {function(*): *}
 */
export const pipe = (...fns) => (x) => fns.reduce((v, f) => f(v), x);
