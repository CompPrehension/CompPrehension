import * as React from 'react';
import { SiteHeader } from './site-header';

export type PageLayoutProps = {
    title?: string | null,
    parent?: { label: string, to: string } | null,
    children: React.ReactNode,
}

/** Общая обвязка сайтовых страниц. */
export const PageLayout = ({ title, parent, children }: PageLayoutProps) => (
    <div className="container-fluid">
        <div className="pt-1 pb-3">
            <SiteHeader title={title} parent={parent} />
        </div>
        {children}
    </div>
);
