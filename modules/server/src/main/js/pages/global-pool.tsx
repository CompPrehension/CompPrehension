import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { container } from 'tsyringe';
import { Link, useNavigate } from 'react-router-dom';
import { GlobalPoolStore } from '../stores/global-pool-store';
import { Header } from '../components/common/header';
import { Loader } from '../components/common/loader';
import { useCurrentUser, useSession } from '../hooks/session-context';
import { useTranslation } from 'react-i18next';

export const GlobalPool = observer(() => {
    const [store] = useState(() => container.resolve(GlobalPoolStore));
    const navigate = useNavigate();
    const user = useCurrentUser();
    const session = useSession();
    const { t } = useTranslation();

    useEffect(() => { store.loadGlobalPool(); }, []);

    const onLangClicked = () => {
        const newLang = user?.language === 'RU' ? 'EN' : 'RU';
        session.changeLanguage(newLang);
    };

    if (!user || store.loadStatus === 'LOADING') return <Loader />;

    return (
        <div className="container-fluid">
            <div className="pt-1 pb-3">
                <Header text={t('globalPool_page_title')}
                        languageHint={t('language_header')}
                        language={user?.language ?? "EN"}
                        onLanguageClicked={onLangClicked}
                        userHint={t('signedin_as_header')}
                        user={user.displayName}
                        userHref={null}
                        logoutLabel={t('logout_header')} />
            </div>
            <div className="mb-3">
                <button type="button"
                        className="btn btn-primary"
                        onClick={() => navigate('/pages/exercise-settings')}>
                    {t('globalPool_page_createBtn')}
                </button>
            </div>
            <ul className="list-group">
                {store.exercises.map(e =>
                    <li key={e.id} className="list-group-item">
                        <Link to={`/pages/exercise-settings?exerciseId=${e.id}`}>{e.name}</Link>
                    </li>
                )}
                {store.exercises.length === 0 && store.loadStatus === 'LOADED' && (
                    <li className="list-group-item text-muted">{t('globalPool_page_empty')}</li>
                )}
            </ul>
        </div>
    );
});
