"use client";

import Link from "next/link";
import { Icon } from "@/components/ui/icon";
import type { IconName } from "@/components/ui/icon";

type NavigationItem = {
    label: string;
    icon: IconName;
    href?: string;
    status: "available" | "planned";
};

const navigation: NavigationItem[] = [
    { label: "Overview", icon: "dashboard", href: "#overview", status: "available" },
    { label: "Cloud assets", icon: "boxes", href: "#assets", status: "available" },
    { label: "Findings", icon: "alert", href: "#findings", status: "available" },
    { label: "Attack graph", icon: "network", status: "planned" },
    { label: "AI copilot", icon: "bot", status: "planned" },
    { label: "Remediation", icon: "sliders", status: "planned" },
    { label: "Scan history", icon: "history", href: "#scan-history", status: "available" },
    { label: "Reports", icon: "report", status: "planned" },
];

type AppSidebarProps = {
    open: boolean;
    onNavigate: () => void;
};

export function AppSidebar({ open, onNavigate }: AppSidebarProps) {
    return (
        <aside id="app-sidebar" className="sidebar" data-open={open}>
            <Link
                className="brand"
                href="#overview"
                aria-label="CloudTrap AI home"
                onClick={onNavigate}
            >
                <span className="brand-mark">
                    <Icon name="shield" size={23} />
                </span>
                <span className="brand-copy">
                    <span className="brand-name">CLOUDTRAP AI</span>
                    <span className="brand-caption">Cloud security intelligence</span>
                </span>
            </Link>

            <div className="nav-section-label">Security workspace</div>

            <nav className="nav-list" aria-label="Main navigation">
                {navigation.map((item) =>
                    item.status === "available" && item.href ? (
                        <a
                            key={item.label}
                            href={item.href}
                            className={`nav-link${item.label === "Overview" ? " nav-link--overview" : ""}`}
                            aria-current={item.label === "Overview" ? "page" : undefined}
                            onClick={onNavigate}
                        >
                            <span className="nav-icon">
                                <Icon name={item.icon} size={18} />
                            </span>
                            <span>{item.label}</span>
                            {item.label === "Cloud assets" && (
                                <span className="nav-link__arrow">
                                    <Icon name="chevron-right" size={14} />
                                </span>
                            )}
                        </a>
                    ) : (
                        <div
                            key={item.label}
                            className="nav-link nav-link--planned"
                            aria-disabled="true"
                            title={`${item.label} is planned for a later development phase`}
                        >
                            <span className="nav-icon">
                                <Icon name={item.icon} size={18} />
                            </span>
                            <span>{item.label}</span>
                            <span className="nav-planned">Soon</span>
                        </div>
                    ),
                )}
            </nav>

            <div className="sidebar-bottom">
                <div className="sidebar-workspace-card">
                    <div className="sidebar-workspace-card__icon">
                        <Icon name="cloud" size={17} />
                    </div>
                    <div className="sidebar-workspace-card__copy">
                        <span className="workspace-label">Workspace inventory</span>
                        <strong>Cloud environment</strong>
                        <span className="sidebar-workspace-card__hint">
                            Select a workspace below
                        </span>
                    </div>
                </div>

                <a className="nav-link nav-link--settings" href="#settings" onClick={onNavigate}>
                    <span className="nav-icon">
                        <Icon name="settings" size={18} />
                    </span>
                    <span>Settings</span>
                </a>

                <div className="sidebar-build-label">
                    <span className="sidebar-build-label__dot" />
                    Midnight SOC interface
                </div>
            </div>
        </aside>
    );
}

