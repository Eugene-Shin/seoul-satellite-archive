// 조건 없음은 항상 undefined로: 아무것도 고르지 않은 여럿 선택은 빈 배열 대신 undefined
export function emptyToUndefined<T>(values: T[]): T[] | undefined {
    return values.length > 0 ? values : undefined;
}
