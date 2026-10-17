"use client";

import { useEffect, useState } from "react";
import { Icon } from "@/components/ui/icon";

type AppHeaderProps = {
    sidebarOpen: boolean;
    onToggleSidebar: () => void;
};

function LiveClock() {
    const [now, setNow] = useState<Date | null>(null);

    useEffect(() => {
        const updateClock = () => setNow(new Date());

        updateClock();
        const intervalId = window.setInterval(updateClock, 30_000);

        return () => window.clearInterval(intervalId);
    }, []);

    if (!now) {
        return <span className="topbar-clock">Local time</span>;
    }

    return (
        <time className="topbar-clock" dateTime={now.toISOString()}>
            <Icon name="clock" size={14} />
            <span>
                {now.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                })}
            </span>
        </time>
    );
}

export function AppHeader({
    sidebarOpen,
    onToggleSidebar,
}: AppHeaderProps) {
    return (
        <header className="topbar">
            <div className="topbar-identity">
                <button
                    type="button"
                    className="icon-button mobile-menu-button"
                    aria-label={sidebarOpen ? "Close navigation" : "Open navigation"}
                    aria-expanded={sidebarOpen}
                    aria-controls="app-sidebar"
                    onClick={onToggleSidebar}
                >
                    <Icon name={sidebarOpen ? "close" : "menu"} size={20} />
                </button>

                <div className="topbar-heading">
                    <div className="breadcrumb">
                        CloudTrap <span>/</span> Security workspace
                    </div>
                    <div className="topbar-title">Security command center</div>
                    <div className="topbar-context">
                        <span className="topbar-context__pulse" />
                        Cloud posture overview
                    </div>
                </div>
            </div>

            <div className="topbar-actions">
                <LiveClock />
                <span className="topbar-divider" aria-hidden="true" />
                <span className="system-state system-state--neutral">
                    <span className="system-state__dot" />
                    Dashboard loaded
                </span>
            </div>
        </header>
    );
}
