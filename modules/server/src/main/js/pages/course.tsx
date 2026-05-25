import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { container } from 'tsyringe';
import { Link, useNavigate } from 'react-router-dom';
import { CourseStore } from '../stores/course-store';
import { Header } from '../components/common/header';
import { Loader } from '../components/common/loader';
import { useCurrentUser, useSession } from '../hooks/session-context';
import { useCourseId } from '../hooks/use-course-id';
import { ImportFromGlobalModal } from '../components/exercise/import-from-global-modal';
import { useTranslation } from 'react-i18next';
import { UserController } from '../controllers/exercise/user-controller';
import * as E from 'fp-ts/lib/Either';
import { canEditExercises } from '../utils/roles';

export const CoursePage = observer(() => {
    const [store] = useState(() => container.resolve(CourseStore));
    const navigate = useNavigate();
    const user = useCurrentUser();
    const session = useSession();
    const courseId = useCourseId();
    const [showImportModal, setShowImportModal] = useState(false);
    const [contextRoles, setContextRoles] = useState<string[]>([]);
    const { t } = useTranslation();

    useEffect(() => {
        if (courseId != null) store.loadCourse(courseId);
    }, [courseId]);

    useEffect(() => {
        if (courseId == null) return;
        (async () => {
            const res = await container.resolve(UserController).getCurrentUser(courseId);
            if (E.isRight(res)) {
                setContextRoles(res.right.roles);
            }
        })();
    }, [courseId]);

    const onLangClicked = () => {
        const newLang = user?.language === 'RU' ? 'EN' : 'RU';
        session.changeLanguage(newLang);
    };

    if (!user) return <Loader />;
    if (courseId == null) return <div>{t('course_page_courseIdRequired')}</div>;
    if (store.loadStatus === 'LOADING') return <Loader />;

    const isPriv = canEditExercises(contextRoles);
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
            {isPriv && (
                <div className="mb-3 d-flex" style={{ gap: '0.5rem' }}>
                    <button type="button"
                            className="btn btn-primary"
                            onClick={() => navigate(`/pages/exercise-settings?courseId=${courseId}`)}>
                        {t('course_page_createExerciseBtn')}
                    </button>
                    <button type="button"
                            className="btn btn-secondary"
                            onClick={() => setShowImportModal(true)}>
                        {t('course_page_importBtn')}
                    </button>
                </div>
            )}
            <ul className="list-group">
                {store.exercises.map(e =>
                    <li key={e.id} className="list-group-item">
                        <Link to={`/pages/exercise-settings?exerciseId=${e.id}&courseId=${courseId}`}>{e.name}</Link>
                    </li>
                )}
                {store.exercises.length === 0 && store.loadStatus === 'LOADED' && (
                    <li className="list-group-item text-muted">{t('course_page_empty')}</li>
                )}
            </ul>
            {isPriv && showImportModal && (
                <ImportFromGlobalModal
                    courseId={courseId}
                    onClose={() => setShowImportModal(false)}
                    onImported={reload} />
            )}
        </div>
    );
});
