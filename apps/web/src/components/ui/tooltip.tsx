"use client";

import {
    useId,
    type ReactNode,
} from "react";

type TooltipProps = {
    content: string;
    children: ReactNode;
    side?: "top" | "right" | "bottom" | "left";
};

export function Tooltip({
    content,
    children,
    side = "top",
}: TooltipProps) {
    const tooltipId = useId();

    return (
        <span className="ui-tooltip">
            <span
                className="ui-tooltip__trigger"
                aria-describedby={tooltipId}
            >
                {children}
            </span>
            <span
                id={tooltipId}
                className={`ui-tooltip__content ui-tooltip__content--${side}`}
                role="tooltip"
            >
                {content}
            </span>
        </span>
    );
}
