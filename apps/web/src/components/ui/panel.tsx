
import type { HTMLAttributes, ReactNode } from "react";
import { Icon, type IconName } from "./icon";

type PanelProps = HTMLAttributes<HTMLElement> & {
    children: ReactNode;
};

export function Panel({
                          className = "",
                          children,
                          ...props
                      }: PanelProps) {
    return (
        <section
            {...props}
            className={`panel ${className}`.trim()}
        >
            {children}
        </section>
    );
}

export function PanelHeading({
                                 title,
                                 description,
                                 action,
                             }: {
    title: string;
    description?: string;
    action?: ReactNode;
}) {
    return (
        <div className="panel-heading">
            <div>
                <h2 className="panel-title">{title}</h2>
                {description ? (
                    <p className="panel-description">{description}</p>
                ) : null}
            </div>
            {action}
        </div>
    );
}

export function EmptyState({
                               icon = "info",
                               title,
                               description,
                               action,
                           }: {
    icon?: IconName;
    title: string;
    description: string;
    action?: ReactNode;
}) {
    return (
        <div className="empty-state" role="status">
      <span className="empty-icon">
        <Icon name={icon} size={20} />
      </span>
            <p className="empty-title">{title}</p>
            <p className="empty-description">{description}</p>
            {action ? <div className="empty-state__action">{action}</div> : null}
        </div>
    );
}
