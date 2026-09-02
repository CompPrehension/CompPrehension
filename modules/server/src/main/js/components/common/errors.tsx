import { observer } from "mobx-react";
import React, { useEffect } from "react";
import { Alert, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ErrorNotification, notifications } from "../../stores/notifications-store";
import { RequestError } from "../../types/request-error";

/** `403 Forbidden`, `500`, or nothing when the request never reached the backend. */
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
                {count > 1 && <span className="badge badge-light ml-2">{count}</span>}
            </Alert.Heading>
            <div className="comp-ph-error-notification-message">{error.message}</div>
            {error.path && <small className="text-muted d-block mt-1">{error.path}</small>}
        </Alert>
    );
});

/**
 * Shows every failed request, whatever its status code, as a self-dismissing alert.
 *
 * `utils/ajax` reports here for all endpoints at once, so an error is never lost just
 * because the calling store had nowhere to put it.
 */
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

/**
 * Claims an error for the page that renders it, so the same error does not additionally
 * pop up as a notification. Every component below does this, so a page that has a slot
 * for errors keeps them there.
 */
export function useHandledError(error: RequestError | null | undefined) {
    useEffect(() => {
        if (error) {
            notifications.handled(error);
        }
    }, [error]);
}

/** Plain in-place error, for pages that already have a place to put one. */
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

/**
 * In-place replacement for content that could not be loaded. Unlike the notifications
 * above it stays put, so the page never pretends the data is simply empty.
 */
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
