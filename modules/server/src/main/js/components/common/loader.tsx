import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { Spinner } from "react-bootstrap";

type LoaderProps = {
    delay?: number,
    styleOverride?: React.CSSProperties,
}
export const Loader = observer((props: LoaderProps) => {
    const delay = props.delay ?? 0;
    const [enabled, setEnabled] = useState(delay === 0);
    useEffect(() => {
        if (delay > 0) {
            setTimeout(() => !enabled && setEnabled(true), delay);
        }
    });

    if (!enabled) {
        return null;
    }
    return <Spinner style={{ ...props.styleOverride }} animation="border" variant="primary" />
});

type LoadingWrapperProps = LoaderProps & {
    isLoading: boolean,
    children?: React.ReactNode[] | React.ReactNode,
}
export const LoadingWrapper = observer((props: LoadingWrapperProps) => {
    const { children, isLoading } = props;
    if (isLoading) {
        return <Loader {...props} />;
    }
    return <>{children}</>;
});
