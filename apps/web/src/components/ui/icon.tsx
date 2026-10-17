
import type { ReactNode, SVGProps } from "react";

export const iconNames = [
    "shield",
    "dashboard",
    "boxes",
    "alert",
    "network",
    "bot",
    "sliders",
    "history",
    "report",
    "settings",
    "search",
    "plus",
    "arrow",
    "activity",
    "cloud",
    "database",
    "lock",
    "menu",
    "close",
    "check",
    "chevron-down",
    "chevron-right",
    "external-link",
    "filter",
    "refresh",
    "copy",
    "clock",
    "info",
    "check-circle",
    "x-circle",
    "loader",
    "file",
    "download",
    "command",
    "sparkles",
    "shield-alert",
] as const;

export type IconName = (typeof iconNames)[number];

const paths: Record<IconName, ReactNode> = {
    shield: <>
        <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
        <path d="m9 12 2 2 4-4" />
    </>,
    dashboard: <>
        <rect x="3" y="3" width="8" height="8" rx="2" />
        <rect x="13" y="3" width="8" height="5" rx="2" />
        <rect x="13" y="10" width="8" height="11" rx="2" />
        <rect x="3" y="13" width="8" height="8" rx="2" />
    </>,
    boxes: <>
        <path d="m12 3 9 5-9 5-9-5 9-5Z" />
        <path d="m3 12 9 5 9-5M3 16l9 5 9-5" />
    </>,
    alert: <>
        <path d="m10.3 3.9-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0Z" />
        <path d="M12 9v4M12 17h.01" />
    </>,
    network: <>
        <rect x="9" y="2" width="6" height="6" rx="2" />
        <rect x="2" y="16" width="6" height="6" rx="2" />
        <rect x="16" y="16" width="6" height="6" rx="2" />
        <path d="M12 8v4M5 16v-4h14v4" />
    </>,
    bot: <>
        <rect x="3" y="7" width="18" height="14" rx="4" />
        <path d="M12 3v4M8 13h.01M16 13h.01M9 17h6" />
    </>,
    sliders: <>
        <path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3" />
        <path d="M2 14h4M10 8h4M18 16h4" />
    </>,
    history: <>
        <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
        <path d="M3 3v5h5M12 7v5l3 2" />
    </>,
    report: <>
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" />
        <path d="M14 2v6h6M8 13h8M8 17h8M8 9h2" />
    </>,
    settings: <>
        <circle cx="12" cy="12" r="3" />
        <path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.6a8 8 0 0 1-1.8 1l-.3 1.8h-2.8l-.3-1.8a8 8 0 0 1-1.8-1l-1.7.6-1.4-2.4 1.4-1.1a7 7 0 0 1 0-2l-1.4-1.1 1.4-2.4 1.7.6a8 8 0 0 1 1.8-1l.3-1.8h2.8l.3 1.8a8 8 0 0 1 1.8 1l1.7-.6 1.4 2.4-1.4 1.1a7 7 0 0 1 0 2Z" />
    </>,
    search: <>
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
    </>,
    plus: <path d="M12 5v14M5 12h14" />,
    arrow: <path d="M5 12h14m-7-7 7 7-7 7" />,
    activity: <path d="M3 12h4l3-8 4 16 3-8h4" />,
    cloud: <path d="M20 16.2A4.5 4.5 0 0 0 18 7.5 6.5 6.5 0 0 0 5.3 9 4 4 0 0 0 6 17h13" />,
    database: <>
        <ellipse cx="12" cy="5" rx="8" ry="3" />
        <path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3" />
    </>,
    lock: <>
        <rect x="4" y="10" width="16" height="11" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
    </>,
    menu: <path d="M4 6h16M4 12h16M4 18h16" />,
    close: <path d="m18 6-12 12M6 6l12 12" />,
    check: <path d="m5 12 4 4L19 6" />,
    "chevron-down": <path d="m6 9 6 6 6-6" />,
    "chevron-right": <path d="m9 18 6-6-6-6" />,
    "external-link": <>
        <path d="M15 3h6v6M10 14 21 3" />
        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
    </>,
    filter: <>
        <path d="M4 7h16M7 12h10M10 17h4" />
        <circle cx="8" cy="7" r="1.5" />
        <circle cx="15" cy="12" r="1.5" />
    </>,
    refresh: <>
        <path d="M20 7v5h-5M4 17v-5h5" />
        <path d="M5.5 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 12.5-3" />
    </>,
    copy: <>
        <rect x="8" y="8" width="13" height="13" rx="2" />
        <path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" />
    </>,
    clock: <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
    </>,
    info: <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 11v5M12 8h.01" />
    </>,
    "check-circle": <>
        <circle cx="12" cy="12" r="9" />
        <path d="m8 12 2.5 2.5L16 9" />
    </>,
    "x-circle": <>
        <circle cx="12" cy="12" r="9" />
        <path d="m9 9 6 6m0-6-6 6" />
    </>,
    loader: <>
        <path d="M12 3a9 9 0 1 0 9 9" />
    </>,
    file: <>
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" />
        <path d="M14 2v6h6" />
    </>,
    download: <>
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <path d="m7 10 5 5 5-5M12 15V3" />
    </>,
    command: <>
        <path d="M9 7V5a2 2 0 1 0-2 2h10a2 2 0 1 0-2-2v14a2 2 0 1 0 2-2H7a2 2 0 1 0 2 2V7Z" />
    </>,
    sparkles: <>
        <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />
        <path d="m19 14 1.2 2.8L23 18l-2.8 1.2L19 22l-1.2-2.8L15 18l2.8-1.2L19 14Z" />
    </>,
    "shield-alert": <>
        <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
        <path d="M12 8v4M12 16h.01" />
    </>,
};

type IconProps = Omit<SVGProps<SVGSVGElement>, "name"> & {
    name: IconName;
    size?: number;
};

export function Icon({
                         name,
                         size = 18,
                         strokeWidth = 1.7,
                         ...props
                     }: IconProps) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            focusable="false"
            {...props}
        >
            {paths[name]}
        </svg>
    );
}
