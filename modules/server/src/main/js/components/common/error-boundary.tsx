import React from "react";
import { Alert, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";

const RenderFailure = ({ error }: { error: Error }) => {
    const { t } = useTranslation();

    return (
        <div className="container pt-3">
            <Alert variant="danger">
                <Alert.Heading as="h6">{t('error_boundary_title')}</Alert.Heading>
                <div className="comp-ph-error-notification-message">{error.message}</div>
                <Button variant="outline-danger" size="sm" className="mt-2"
                        onClick={() => window.location.reload()}>
                    {t('error_boundary_reload')}
                </Button>
            </Alert>
        </div>
    );
};

type ErrorBoundaryProps = { children: React.ReactNode };
type ErrorBoundaryState = { error: Error | null };

/**
 * Catches exceptions thrown while rendering.
 */
export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
    state: ErrorBoundaryState = { error: null };

    static getDerivedStateFromError(error: Error): ErrorBoundaryState {
        return { error };
    }

    componentDidCatch(error: Error, info: React.ErrorInfo) {
        console.error('Render failed:', error, info.componentStack);
    }

    render() {
        return this.state.error !== null
            ? <RenderFailure error={this.state.error} />
            : this.props.children;
    }
}
