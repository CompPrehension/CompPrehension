import React from "react";
import { useState } from "react";

import { Button, Modal as RBModal } from "react-bootstrap";


export type ModalProps = {
    title?: string,
    primaryBtnTitle?: string,
    handlePrimaryBtnClicked?: () => void,
    secondaryBtnTitle?: string,
    handleSecondaryBtnClicked?: () => void,
    children?: React.ReactNode[] | React.ReactNode,
}

export const Modal = (props: ModalProps) => {
    const { title, primaryBtnTitle,
        handlePrimaryBtnClicked,
        secondaryBtnTitle,
        handleSecondaryBtnClicked,
        children,
    } = props;

    return (
        <RBModal.Dialog>
            <RBModal.Header closeButton>
                <RBModal.Title>{title}</RBModal.Title>
            </RBModal.Header>

            <RBModal.Body>
                {children}
            </RBModal.Body>

            <RBModal.Footer>
                {secondaryBtnTitle ? <Button variant="secondary" onClick={handleSecondaryBtnClicked}>{secondaryBtnTitle}</Button> : null}
                {primaryBtnTitle ? <Button variant="primary" onClick={handlePrimaryBtnClicked}>{primaryBtnTitle}</Button> : null}
            </RBModal.Footer>
        </RBModal.Dialog>
    );
}
