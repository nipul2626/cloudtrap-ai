"use client";

import { useEffect, useState, type ReactNode } from "react";
import { AppHeader } from "@/components/layout/app-header";
import { AppSidebar } from "@/components/layout/app-sidebar";

type AppShellProps = {
    children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    useEffect(() => {
        if (!sidebarOpen) return;

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setSidebarOpen(false);
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [sidebarOpen]);

    function closeSidebar() {
        setSidebarOpen(false);
    }

    return (
        <div className="app-shell app-shell--animated" id="overview">
            <div className="ambient ambient--one" aria-hidden="true" />
            <div className="ambient ambient--two" aria-hidden="true" />
            <div className="ambient ambient--three" aria-hidden="true" />

            {sidebarOpen && (
                <button
                    type="button"
                    className="sidebar-backdrop"
                    aria-label="Close navigation menu"
                    onClick={closeSidebar}
                />
            )}

            <div id="app-sidebar">
                <AppSidebar
                    open={sidebarOpen}
                    onNavigate={closeSidebar}
                />
            </div>

            <div className="main-column">
                <AppHeader
                    sidebarOpen={sidebarOpen}
                    onToggleSidebar={() =>
                        setSidebarOpen((current) => !current)
                    }
                />
                {children}
            </div>
        </div>
    );
}
