import { useState } from "react";

// 값은 저장할 때의 검색에서만 유효하다. 검색이 바뀌면(검색 버튼, 뒤로 가기) null.
// 저장할 때 검색 키를 함께 넣어서, 쓰는 쪽은 키를 다루지 않는다
export function useSearchScopedState<T>(currentSearchKey: string) {
    const [saved, setSaved] = useState<{
        searchKey: string;
        value: T;
    } | null>(null);
    const value = saved?.searchKey === currentSearchKey ? saved.value : null;

    function set(next: T) {
        setSaved({ searchKey: currentSearchKey, value: next });
    }

    function clear() {
        setSaved(null);
    }

    return [value, set, clear] as const;
}
