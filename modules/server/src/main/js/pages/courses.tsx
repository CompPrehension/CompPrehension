import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { Button } from 'react-bootstrap';
import { useNavigate } from 'react-router';
import { CoursesStore } from '../stores/courses-store';
import { PageLayout } from '../components/common/page-layout';
import { Loader } from '../components/common/loader';
import { LoadFailure } from '../components/common/errors';
import { useCurrentUser } from '../hooks/session-context';
import { useTranslation } from 'react-i18next';

export const CoursesPage = observer(() => {
    const [store] = useState(() => new CoursesStore());
    const navigate = useNavigate();
    const user = useCurrentUser();
    const { t } = useTranslation();

    useEffect(() => { store.loadMyCourses(); }, [store]);

    if (!user || store.loadStatus === 'LOADING') return <Loader />;

    return (
        <PageLayout>
            {(user.permissions.canViewGlobalPool || user.permissions.canRegisterLms) && (
                <div className="mb-3 d-flex" style={{ gap: '0.5rem' }}>
                    {user.permissions.canViewGlobalPool && (
                        <Button variant="outline-primary"
                                onClick={() => navigate('/pages/global-pool')}>
                            {t('globalPool_page_title')}
                        </Button>
                    )}
                    {user.permissions.canRegisterLms && (
                        <Button variant="outline-secondary"
                                onClick={() => navigate('/pages/lti-registrations')}>
                            {t('ltiRegistrations_page_title')}
                        </Button>
                    )}
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
                                        {c.educationResourceUrl || `#${c.educationResourceId}`}
                                    </h6>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </PageLayout>
    );
});
