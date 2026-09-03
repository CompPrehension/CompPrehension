import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { CoursesStore } from '../stores/courses-store';
import { Header } from '../components/common/header';
import { Loader } from '../components/common/loader';
import { LoadFailure } from '../components/common/errors';
import { useCurrentUser, useSession } from '../hooks/session-context';
import { useTranslation } from 'react-i18next';

export const CoursesPage = observer(() => {
    const [store] = useState(() => new CoursesStore());
    const navigate = useNavigate();
    const user = useCurrentUser();
    const session = useSession();
    const { t } = useTranslation();

    useEffect(() => { store.loadMyCourses(); }, [store]);

    const onLangClicked = () => {
        const newLang = user?.language === 'RU' ? 'EN' : 'RU';
        session.changeLanguage(newLang);
    };

    if (!user || store.loadStatus === 'LOADING') return <Loader />;

    return (
        <div className="container-fluid">
            <div className="pt-1 pb-3">
                <Header text={t('courses_page_title')}
                        languageHint={t('language_header')}
                        language={user.language ?? 'EN'}
                        onLanguageClicked={onLangClicked}
                        userHint={t('signedin_as_header')}
                        user={user.displayName}
                        userHref={null}
                        logoutLabel={t('logout_header')} />
            </div>
            {user.permissions.canViewGlobalPool && (
                <div className="mb-3">
                    <Button variant="outline-primary"
                            onClick={() => navigate('/pages/global-pool')}>
                        {t('courses_page_globalPoolBtn')}
                    </Button>
                </div>
            )}
            {store.loadStatus === 'FAILED' && store.error ? (
                <LoadFailure error={store.error} onRetry={() => store.loadMyCourses()} />
            ) : store.courses.length === 0 ? (
                <div className="alert alert-info">{t('courses_page_empty')}</div>
            ) : (
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3">
                    {store.courses.map(c => (
                        <div key={c.id} className="col mb-4">
                            <div className="card h-100"
                                 role="button"
                                 onClick={() => navigate(`/pages/course?courseId=${c.id}`)}>
                                <div className="card-body">
                                    <h5 className="card-title">{c.name}</h5>
                                    <h6 className="card-subtitle text-muted">
                                        {c.educationResourceName || `#${c.educationResourceId}`}
                                    </h6>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
});
