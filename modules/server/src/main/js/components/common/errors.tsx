import { observer } from "mobx-react";
import React, { useEffect } from "react";
import { Alert, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ErrorNotification, notifications } from "../../stores/notifications-store";
import { RequestError } from "../../types/request-error";

function statusLine(error: RequestError): string | null {
    if (error.status === undefined) {
        return null;
    }
    return error.title ? `${error.status} ${error.title}` : `${error.status}`;
}

const ErrorNotificationAlert = observer(({ notification }: { notification: ErrorNotification }) => {
    const { t } = useTranslation();
    const { error, count } = notification;
    const status = statusLine(error);

    return (
        <Alert variant="danger" dismissible onClose={() => notifications.dismiss(notification.id)}>
            <Alert.Heading as="h6" className="d-flex align-items-center">
                <span>{status ?? t('error_notification_title')}</span>
                {count > 1 && <span className="badge bg-light text-dark ms-2">{count}</span>}
            </Alert.Heading>
            <div className="comp-ph-error-notification-message">{error.message}</div>
            {error.path && <small className="text-muted d-block mt-1">{error.path}</small>}
        </Alert>
    );
});

export const ErrorNotifications = observer(() => {
    if (notifications.notifications.length === 0) {
        return null;
    }

    return (
        <div className="comp-ph-error-notifications">
            {notifications.notifications.map(n => (
                <ErrorNotificationAlert key={n.id} notification={n} />
            ))}
        </div>
    );
});

export function useHandledError(error: RequestError | null | undefined) {
    useEffect(() => {
        if (error) {
            notifications.handled(error);
        }
    }, [error]);
}

export const InlineError = observer(({ error }: { error: RequestError }) => {
    useHandledError(error);
    // const status = statusLine(error);

    return (
        <Alert variant="danger">
            {error.message}
        </Alert>
    );
});

type LoadFailureProps = {
    error: RequestError,
    onRetry?: () => void,
}

export const LoadFailure = observer(({ error, onRetry }: LoadFailureProps) => {
    const { t } = useTranslation();
    const status = statusLine(error);
    useHandledError(error);

    return (
        <Alert variant="danger">
            <Alert.Heading as="h6">{t('error_page_title')}</Alert.Heading>
            <div className="comp-ph-error-notification-message">
                {status ? `${status} — ${error.message}` : error.message}
            </div>
            {onRetry && (
                <Button variant="outline-danger" size="sm" className="mt-2" onClick={onRetry}>
                    {t('error_page_retry')}
                </Button>
            )}
        </Alert>
    );
});
