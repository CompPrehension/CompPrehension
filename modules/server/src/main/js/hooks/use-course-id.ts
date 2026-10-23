import { useSearchParams } from 'react-router';

export function useCourseId(): number | null {
    const [params] = useSearchParams();
    const raw = params.get('courseId');
    if (!raw || raw === 'null') return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
}
