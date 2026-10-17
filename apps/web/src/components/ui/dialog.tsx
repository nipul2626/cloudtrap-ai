"use client";

import {
    useEffect,
    useId,
    useRef,
    type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { Icon } from "./icon";

type DialogProps = {
    open: boolean;
    onClose: () => void;
    title: string;
    description?: string;
    children: ReactNode;
    footer?: ReactNode;
    size?: "sm" | "md" | "lg";
};

export function Dialog({
    open,
    onClose,
    title,
    description,
    children,
    footer,
    size = "md",
}: DialogProps) {
    const titleId = useId();
    const descriptionId = useId();
    const dialogRef = useRef<HTMLDivElement>(null);
    const previousFocusRef = useRef<HTMLElement | null>(null);

    useEffect(() => {
        if (!open) return;

        previousFocusRef.current =
            document.activeElement instanceof HTMLElement
                ? document.activeElement
                : null;

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        const frame = window.requestAnimationFrame(() => {
            const dialog = dialogRef.current;
            const target = dialog?.querySelector<HTMLElement>(
                [
                    "button:not(:disabled)",
                    "a[href]",
                    "input:not(:disabled)",
                    "select:not(:disabled)",
                    "textarea:not(:disabled)",
                    '[tabindex]:not([tabindex="-1"])',
                ].join(","),
            );

            (target ?? dialog)?.focus();
        });

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape") {
                event.preventDefault();
                onClose();
                return;
            }

            if (event.key !== "Tab" || !dialogRef.current) return;

            const focusable = Array.from(
                dialogRef.current.querySelectorAll<HTMLElement>(
                    [
                        "button:not(:disabled)",
                        "a[href]",
                        "input:not(:disabled)",
                        "select:not(:disabled)",
                        "textarea:not(:disabled)",
                        '[tabindex]:not([tabindex="-1"])',
                    ].join(","),
                ),
            ).filter((element) => element.getAttribute("aria-hidden") !== "true");

            if (focusable.length === 0) {
                event.preventDefault();
                dialogRef.current.focus();
                return;
            }

            const first = focusable[0];
            const last = focusable[focusable.length - 1];

            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (
                !event.shiftKey &&
                document.activeElement === last
            ) {
                event.preventDefault();
                first.focus();
            }
        }

        document.addEventListener("keydown", handleKeyDown);

        return () => {
            window.cancelAnimationFrame(frame);
            document.removeEventListener("keydown", handleKeyDown);
            document.body.style.overflow = previousOverflow;
            previousFocusRef.current?.focus();
        };
    }, [open, onClose]);

    if (!open || typeof document === "undefined") return null;

    return createPortal(
        <div
            className="ui-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) onClose();
            }}
        >
            <section
                ref={dialogRef}
                className={`ui-dialog ui-dialog--${size}`}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                aria-describedby={description ? descriptionId : undefined}
                tabIndex={-1}
            >
                <header className="ui-dialog__header">
                    <div className="ui-dialog__heading">
                        <h2 id={titleId}>{title}</h2>
                        {description ? (
                            <p id={descriptionId}>{description}</p>
                        ) : null}
                    </div>

                    <button
                        type="button"
                        className="ui-icon-button"
                        aria-label="Close dialog"
                        onClick={onClose}
                    >
                        <Icon name="close" />
                    </button>
                </header>

                <div className="ui-dialog__body">{children}</div>

                {footer ? (
                    <footer className="ui-dialog__footer">{footer}</footer>
                ) : null}
            </section>
        </div>,
        document.body,
    );
}
