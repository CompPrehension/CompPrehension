import React, { useEffect, useRef, useState } from 'react';
import { observer } from 'mobx-react';
import { Button, Form } from 'react-bootstrap';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { CourseStore } from '../stores/course-store';
import { Header } from '../components/common/header';
import { Loader } from '../components/common/loader';
import { LoadFailure } from '../components/common/errors';
import { useCurrentUser, useSession } from '../hooks/session-context';
import { useCourseId } from '../hooks/use-course-id';
import { ImportFromGlobalModal } from '../components/exercise/import-from-global-modal';
import { useTranslation } from 'react-i18next';
import { deepLinkingController } from '../controllers';
import * as E from 'fp-ts/lib/Either';

/** Hidden form that auto-POSTs the signed deep-linking response back to Moodle. */
const DeepLinkReturnForm: React.FC<{ jwt: string; returnUrl: string }> = ({ jwt, returnUrl }) => {
    const formRef = useRef<HTMLFormElement>(null);
    useEffect(() => {
        formRef.current?.submit();
    }, []);
    return (
        <form ref={formRef} method="post" action={returnUrl}>
            <input type="hidden" name="JWT" value={jwt} />
        </form>
    );
};

/**
 * Shown inside Moodle's "Select content" iframe: pick course exercises and push them
 * back to Moodle, which creates the External Tool activities.
 */
const DeepLinkSelection: React.FC<{ exercises: { id: number; name: string }[] }> = ({ exercises }) => {
    const { t } = useTranslation();
    const [selected, setSelected] = useState<Set<number>>(new Set());
    const [existing, setExisting] = useState<Set<number>>(new Set());
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [payload, setPayload] = useState<{ jwt: string; returnUrl: string } | null>(null);

    useEffect(() => {
        (async () => {
            const res = await deepLinkingController.existing();
            if (E.isRight(res)) setExisting(new Set(res.right.exerciseIds));
        })();
    }, []);

    const toggle = (id: number) => {
        setSelected(prev => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const submit = async () => {
        if (selected.size === 0) {
            setError(t('deeplink_selectAtLeastOne'));
            return;
        }
        setSubmitting(true);
        setError(null);
        const res = await deepLinkingController.build(Array.from(selected));
        if (E.isRight(res)) {
            setPayload(res.right); // mounts DeepLinkReturnForm -> navigates the iframe to Moodle
        } else {
            setError(t('deeplink_error'));
            setSubmitting(false);
        }
    };

    if (payload) {
        return <DeepLinkReturnForm jwt={payload.jwt} returnUrl={payload.returnUrl} />;
    }

    return (
        <div className="container-fluid p-3">
            <h5>{t('deeplink_title')}</h5>
            <p className="text-muted">{t('deeplink_hint')}</p>
            {exercises.length === 0 && <div className="text-muted mb-3">{t('deeplink_empty')}</div>}
            <ul className="list-group mb-3">
                {exercises.map(e => {
                    const already = existing.has(e.id);
                    return (
                        <li key={e.id} className="list-group-item d-flex align-items-center" style={{ gap: '0.5rem' }}>
                            <Form.Check.Input type="checkbox"
                                   className="mt-0"
                                   checked={selected.has(e.id)}
                                   disabled={already || submitting}
                                   onChange={() => toggle(e.id)} />
                            <span>{e.name}</span>
                            {already && <span className="badge bg-secondary ms-auto">{t('deeplink_added')}</span>}
                        </li>
                    );
                })}
            </ul>
            {error && <div className="alert alert-danger">{error}</div>}
            <Button variant="primary"
                    disabled={submitting || selected.size === 0}
                    onClick={submit}>
                {submitting ? t('deeplink_submitting') : t('deeplink_addBtn')}
            </Button>
        </div>
    );
};

export const CoursePage = observer(() => {
    const [store] = useState(() => new CourseStore());
    const navigate = useNavigate();
    const user = useCurrentUser();
    const session = useSession();
    const courseId = useCourseId();
    const [searchParams] = useSearchParams();
    const [showImportModal, setShowImportModal] = useState(false);
    const { t } = useTranslation();

    const isDeepLink = searchParams.get('lti') === 'deeplink';
    const inIframe = typeof window !== 'undefined' && window.self !== window.top;

    useEffect(() => {
        if (courseId != null) store.loadCourse(courseId);
    }, [courseId, store]);

    const onLangClicked = () => {
        const newLang = user?.language === 'RU' ? 'EN' : 'RU';
        session.changeLanguage(newLang);
    };

    if (!user) return <Loader />;
    if (courseId == null) return <div>{t('course_page_courseIdRequired')}</div>;
    if (store.loadStatus === 'LOADING') return <Loader />;

    // Deep-linking launch rendered inside Moodle's "Select content" iframe: focused picker.
    // Authorization is enforced server-side on /api/lti/deep-link/build.
    if (isDeepLink && inIframe) {
        return <DeepLinkSelection exercises={store.exercises} />;
    }

    const { canCreateExercise, canImportInherit, canImportClone } = store.permissions;
    const canImport = canImportInherit || canImportClone;
    const reload = () => store.loadCourse(courseId);

    return (
        <div className="container-fluid">
            <div className="pt-1 pb-3">
                <Header text={t('course_page_title', { id: courseId })}
                        languageHint={t('language_header')}
                        language={user?.language ?? "EN"}
                        onLanguageClicked={onLangClicked}
                        userHint={t('signedin_as_header')}
                        user={user.displayName}
                        userHref={null}
                        logoutLabel={t('logout_header')} />
            </div>
            {isDeepLink && !inIframe && (
                <div className="alert alert-info">{t('deeplink_blockHint')}</div>
            )}
            {store.loadStatus === 'FAILED' && store.error && (
                <LoadFailure error={store.error} onRetry={reload} />
            )}
            {(canCreateExercise || canImport) && (
                <div className="mb-3 d-flex" style={{ gap: '0.5rem' }}>
                    {canCreateExercise && (
                        <Button variant="primary"
                                onClick={() => navigate(`/pages/exercise-settings?courseId=${courseId}`)}>
                            {t('course_page_createExerciseBtn')}
                        </Button>
                    )}
                    {canImport && (
                        <Button variant="secondary"
                                onClick={() => setShowImportModal(true)}>
                            {t('course_page_importBtn')}
                        </Button>
                    )}
                </div>
            )}
            <ul className="list-group">
                {store.exercises.map(e =>
                    <li key={e.id} className="list-group-item">
                        <Link to={`/pages/exercise-settings?exerciseId=${e.id}&courseId=${courseId}`}>{e.name}</Link>
                    </li>
                )}
                {store.exercises.length === 0 && store.loadStatus === 'LOADED' && (
                    <li key={'undefined'} className="list-group-item text-muted">{t('course_page_empty')}</li>
                )}
            </ul>
            {canImport && showImportModal && (
                <ImportFromGlobalModal
                    courseId={courseId}
                    canInherit={canImportInherit}
                    canClone={canImportClone}
                    onClose={() => setShowImportModal(false)}
                    onImported={reload} />
            )}
        </div>
    );
});
