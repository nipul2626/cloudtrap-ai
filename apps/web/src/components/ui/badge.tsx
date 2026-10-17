
import type { HTMLAttributes } from "react";

export type BadgeTone =
    | "neutral"
    | "info"
    | "success"
    | "warning"
    | "critical";

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
    tone?: BadgeTone;
    dot?: boolean;
};

export function Badge({
                          tone = "neutral",
                          dot = false,
                          className = "",
                          children,
                          ...props
                      }: BadgeProps) {
    return (
        <span
            {...props}
            className={`ui-badge ui-badge--${tone} ${className}`.trim()}
        >
      {dot ? <span className="ui-badge__dot" aria-hidden="true" /> : null}
            {children}
    </span>
    );
}

export function SeverityBadge({
                                  severity,
                              }: {
    severity: "critical" | "high" | "medium" | "low" | "info";
}) {
    const tone: Record<typeof severity, BadgeTone> = {
        critical: "critical",
        high: "critical",
        medium: "warning",
        low: "success",
        info: "info",
    };

    return (
        <Badge tone={tone[severity]} dot>
            {severity.charAt(0).toUpperCase() + severity.slice(1)}
        </Badge>
    );
}

export function StatusBadge({
                                status,
                            }: {
    status: "queued" | "running" | "completed" | "failed" | "open" | "resolved";
}) {
    const tone: Record<typeof status, BadgeTone> = {
        queued: "neutral",
        running: "info",
        completed: "success",
        failed: "critical",
        open: "warning",
        resolved: "success",
    };

    return (
        <Badge tone={tone[status]} dot>
            {status.charAt(0).toUpperCase() + status.slice(1)}
        </Badge>
    );
}
