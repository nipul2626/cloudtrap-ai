"use client";

import {
    cloneElement,
    isValidElement,
    useId,
    type HTMLAttributes,
    type ReactElement,
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

    const trigger = isValidElement(children)
        ? cloneElement(
              children as ReactElement<HTMLAttributes<HTMLElement>>,
              {
                  "aria-describedby": tooltipId,
              },
          )
        : <span>{children}</span>;

    return (
        <span className="ui-tooltip">
            <span className="ui-tooltip__trigger">
                {trigger}
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
