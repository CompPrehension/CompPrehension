import React from "react";


export const Optional = (props: { condition: boolean, children?: React.ReactNode[] | React.ReactNode }) => {
    const { condition, children } = props;
    if (!condition) {
        return null;        
    }
    return (<>{children}</>);
}
