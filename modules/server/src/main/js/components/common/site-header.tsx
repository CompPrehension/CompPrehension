import { observer } from 'mobx-react';
import * as React from 'react';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Header, HeaderCrumb } from './header';
import { Modal } from './modal';
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
    const [isLogoutConfirmationShown, setLogoutConfirmationShown] = React.useState(false);

    if (!user) {
        return null;
    }

    const { isLtiMode } = user.permissions;

    const onLanguageClicked = () => {
        session.changeLanguage(user.language === 'RU' ? 'EN' : 'RU');
    };

    const crumbs: HeaderCrumb[] = [{ label: t('courses_page_title'), onClick: () => navigate('/pages/courses') }];
    if (parent) {
        crumbs.push({ label: parent.label, onClick: () => navigate(parent.to) });
    }
    if (title) {
        crumbs.push({ label: title });
    }

    const hideLogoutConfirmation = () => setLogoutConfirmationShown(false);

    return (
        <>
            <Header
                crumbs={crumbs}
                languageHint={t('language_header')}
                language={user.language}
                onLanguageClicked={onLanguageClicked}
                userHint={t('signedin_as_header')}
                user={user.displayName}
                userHref={null}
                logoutLabel={t('logout_header')}
                onLogoutClicked={() => setLogoutConfirmationShown(true)}
            />
            <Modal
                show={isLogoutConfirmationShown}
                title={t('logoutModal_title')}
                closeButton={true}
                handleClose={hideLogoutConfirmation}
                primaryBtnTitle={t('logout_header')}
                primaryBtnVariant="danger"
                handlePrimaryBtnClicked={() => window.location.assign(`/logout?returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`)}
                secondaryBtnTitle={t('logoutModal_cancel')}
                handleSecondaryBtnClicked={hideLogoutConfirmation}
            >
                <p className={isLtiMode ? undefined : 'mb-0'}>{t('logoutModal_question')}</p>
                {isLtiMode && <div className="alert alert-warning mb-0">{t('logoutModal_lmsWarning')}</div>}
            </Modal>
        </>
    );
});
