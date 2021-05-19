import React from "react";


export const Optional = (props: { isVisible: boolean, children?: React.ReactNode[] | React.ReactNode }) => {
    const { isVisible, children } = props;
    if (!isVisible) {
        return null;        
    }
    return (<>{children}</>);
}
