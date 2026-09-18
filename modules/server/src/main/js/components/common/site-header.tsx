import { observer } from 'mobx-react';
import * as React from 'react';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Header, HeaderCrumb } from './header';
import { useCurrentUser, useSession } from '../../hooks/session-context';

export type SiteHeaderProps = {
    title?: string | null,
    parent?: { label: string, to: string } | null,
}

/**
 * Общий хедер сайтовых страниц.
 */
export const SiteHeader = observer(({ title, parent }: SiteHeaderProps) => {
    const user = useCurrentUser();
    const session = useSession();
    const navigate = useNavigate();
    const { t } = useTranslation();

    if (!user) {
        return null;
    }

    const { isLtiMode } = user.permissions;

    const onLanguageClicked = () => {
        session.changeLanguage(user.language === 'RU' ? 'EN' : 'RU');
    };

    const crumbs: HeaderCrumb[] = [];
    if (!isLtiMode) {
        crumbs.push({ label: t('courses_page_title'), onClick: () => navigate('/pages/courses') });
    }
    if (parent) {
        crumbs.push({ label: parent.label, onClick: () => navigate(parent.to) });
    }
    if (title) {
        crumbs.push({ label: title });
    }

    return (
        <Header
            crumbs={crumbs}
            languageHint={t('language_header')}
            language={user.language}
            onLanguageClicked={onLanguageClicked}
            userHint={t('signedin_as_header')}
            user={user.displayName}
            userHref={null}
            logoutLabel={!isLtiMode ? t('logout_header') : null}
        />
    );
});
