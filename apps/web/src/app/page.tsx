
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { WorkspaceOverview } from "@/components/dashboard/workspace-overview";

type IconName =
    | "shield"
    | "dashboard"
    | "boxes"
    | "alert"
    | "network"
    | "bot"
    | "sliders"
    | "history"
    | "report"
    | "settings"
    | "search"
    | "plus"
    | "arrow"
    | "activity"
    | "cloud"
    | "database"
    | "lock"
    | "menu"
    | "close"
    | "clock"
    | "sparkles"
    | "chevron";

function Icon({
                  name,
                  size = 18,
              }: {
    name: IconName;
    size?: number;
}) {
    const common = {
        width: size,
        height: size,
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        strokeWidth: 1.7,
        strokeLinecap: "round" as const,
        strokeLinejoin: "round" as const,
        "aria-hidden": true as const,
    };

    const paths: Record<IconName, React.ReactNode> = {
        shield: (
            <>
                <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
                <path d="m9 12 2 2 4-4" />
            </>
        ),
        dashboard: (
            <>
                <rect x="3" y="3" width="8" height="8" rx="2" />
                <rect x="13" y="3" width="8" height="5" rx="2" />
                <rect x="13" y="10" width="8" height="11" rx="2" />
                <rect x="3" y="13" width="8" height="8" rx="2" />
            </>
        ),
        boxes: (
            <>
                <path d="m12 3 9 5-9 5-9-5 9-5Z" />
                <path d="m3 12 9 5 9-5" />
                <path d="m3 16 9 5 9-5" />
            </>
        ),
        alert: (
            <>
                <path d="m10.3 3.9-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0Z" />
                <path d="M12 9v4" />
                <path d="M12 17h.01" />
            </>
        ),
        network: (
            <>
                <rect x="9" y="2" width="6" height="6" rx="2" />
                <rect x="2" y="16" width="6" height="6" rx="2" />
                <rect x="16" y="16" width="6" height="6" rx="2" />
                <path d="M12 8v4M5 16v-4h14v4" />
            </>
        ),
        bot: (
            <>
                <rect x="3" y="7" width="18" height="14" rx="4" />
                <path d="M12 3v4M8 13h.01M16 13h.01M9 17h6" />
            </>
        ),
        sliders: (
            <>
                <path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3" />
                <path d="M2 14h4M10 8h4M18 16h4" />
            </>
        ),
        history: (
            <>
                <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
                <path d="M3 3v5h5M12 7v5l3 2" />
            </>
        ),
        report: (
            <>
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" />
                <path d="M14 2v6h6M8 13h8M8 17h8M8 9h2" />
            </>
        ),
        settings: (
            <>
                <circle cx="12" cy="12" r="3" />
                <path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.6a8 8 0 0 1-1.8 1l-.3 1.8h-2.8l-.3-1.8a8 8 0 0 1-1.8-1l-1.7.6-1.4-2.4 1.4-1.1a7 7 0 0 1 0-2l-1.4-1.1 1.4-2.4 1.7.6a8 8 0 0 1 1.8-1l.3-1.8h2.8l.3 1.8a8 8 0 0 1 1.8 1l1.7-.6 1.4 2.4-1.4 1.1a7 7 0 0 1 0 2Z" />
            </>
        ),
        search: (
            <>
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
            </>
        ),
        plus: <path d="M12 5v14M5 12h14" />,
        arrow: <path d="M5 12h14m-7-7 7 7-7 7" />,
        activity: <path d="M3 12h4l3-8 4 16 3-8h4" />,
        cloud: (
            <path d="M20 16.2A4.5 4.5 0 0 0 18 7.5 6.5 6.5 0 0 0 5.3 9 4 4 0 0 0 6 17h13" />
        ),
        database: (
            <>
                <ellipse cx="12" cy="5" rx="8" ry="3" />
                <path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3" />
            </>
        ),
        lock: (
            <>
                <rect x="4" y="10" width="16" height="11" rx="2" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
            </>
        ),
        menu: <path d="M4 6h16M4 12h16M4 18h16" />,
        close: <path d="m18 6-12 12M6 6l12 12" />,
        clock: (
            <>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
            </>
        ),
        sparkles: (
            <>
                <path d="m12 3 1.9 5.8L20 11l-6.1 2.2L12 19l-1.9-5.8L4 11l6.1-2.2L12 3Z" />
                <path d="m19 14 1.2 2.8L23 18l-2.8 1.2L19 22l-1.2-2.8L15 18l2.8-1.2L19 14Z" />
            </>
        ),
        chevron: <path d="m9 18 6-6-6-6" />,
    };

    return <svg {...common}>{paths[name]}</svg>;
}

const navigation: {
    label: string;
    icon: IconName;
    href?: string;
    status: "available" | "planned";
}[] = [
    { label: "Overview", icon: "dashboard", href: "#overview", status: "available" },
    { label: "Cloud assets", icon: "boxes", href: "#assets", status: "available" },
    { label: "Findings", icon: "alert", href: "#findings", status: "available" },
    { label: "Attack graph", icon: "network", status: "planned" },
    { label: "AI copilot", icon: "bot", status: "planned" },
    { label: "Remediation", icon: "sliders", status: "planned" },
    { label: "Scan history", icon: "history", href: "#scan-history", status: "available" },
    { label: "Reports", icon: "report", status: "planned" },
];

function Sidebar({
                     open,
                     onNavigate,
                 }: {
    open: boolean;
    onNavigate: () => void;
}) {
    return (
        <aside className="sidebar" data-open={open}>
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
                        item.status === "available" ? (
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
                  <Icon name="chevron" size={14} />
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

                <a className="nav-link nav-link--settings" href="#settings">
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

function MetricCard({
                        label,
                        value,
                        footnote,
                        icon,
                        variant,
                    }: {
    label: string;
    value: string;
    footnote: string;
    icon: IconName;
    variant: "violet" | "rose" | "blue" | "cyan";
}) {
    return (
        <article className={`panel metric-card metric-card--${variant}`}>
            <span className="metric-card__glow" aria-hidden="true" />
            <div className="metric-topline">
                <span className="metric-label">{label}</span>
                <span className="metric-icon">
          <Icon name={icon} size={18} />
        </span>
            </div>
            <div className="metric-value">{value}</div>
            <div className="metric-footnote">
                <span className="metric-footnote__marker" />
                {footnote}
            </div>
            <div className="metric-bottom-line" aria-hidden="true">
                <span />
            </div>
        </article>
    );
}

function EmptyActivity({
                           icon,
                           title,
                           description,
                       }: {
    icon: IconName;
    title: string;
    description: string;
}) {
    return (
        <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} size={20} />
      </span>
            <p className="empty-title">{title}</p>
            <p className="empty-description">{description}</p>
        </div>
    );
}

function NetworkPreview() {
    return (
        <div
            className="hero-network"
            role="img"
            aria-label="Decorative preview of connected cloud resources, not live topology data"
        >
            <div className="network-grid" aria-hidden="true" />
            <div className="network-orbit network-orbit--outer" />
            <div className="network-orbit network-orbit--inner" />
            <div className="network-ring network-ring--one" />
            <div className="network-ring network-ring--two" />

            <svg
                className="network-connections"
                viewBox="0 0 340 250"
                fill="none"
                aria-hidden="true"
            >
                <path className="network-connection" d="M170 125 76 55" />
                <path className="network-connection" d="M170 125 267 58" />
                <path className="network-connection" d="M170 125 274 190" />
                <path className="network-connection" d="M170 125 70 193" />
                <path className="network-connection network-connection--secondary" d="M76 55 267 58 274 190 70 193Z" />
                <circle className="network-connection-point" cx="76" cy="55" r="3" />
                <circle className="network-connection-point" cx="267" cy="58" r="3" />
                <circle className="network-connection-point" cx="274" cy="190" r="3" />
                <circle className="network-connection-point" cx="70" cy="193" r="3" />
            </svg>

            <span className="network-particle network-particle--one" />
            <span className="network-particle network-particle--two" />
            <span className="network-particle network-particle--three" />

            <span className="network-node node-one">
        <Icon name="cloud" size={17} />
      </span>
            <span className="network-node node-two">
        <Icon name="database" size={17} />
      </span>
            <span className="network-node node-three">
        <Icon name="lock" size={17} />
      </span>
            <span className="network-node node-four">
        <Icon name="activity" size={17} />
      </span>

            <span className="network-core">
        <span className="network-core__halo" />
        <Icon name="shield" size={29} />
      </span>

            <span className="network-signal">
        <span className="network-signal__dot" />
        VISUAL PREVIEW
      </span>
        </div>
    );
}

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

export default function DashboardPage() {
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

            <Sidebar open={sidebarOpen} onNavigate={closeSidebar} />

            <div className="main-column">
                <header className="topbar">
                    <div className="topbar-identity">
                        <button
                            type="button"
                            className="icon-button mobile-menu-button"
                            aria-label={sidebarOpen ? "Close navigation" : "Open navigation"}
                            aria-expanded={sidebarOpen}
                            onClick={() => setSidebarOpen((current) => !current)}
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

                <main className="page-content">
                    <section className="page-heading reveal reveal--1">
                        <div className="page-heading__copy">
                            <div className="eyebrow">
                                <span className="eyebrow__line" />
                                CLOUD SECURITY / OVERVIEW
                            </div>
                            <h1 className="page-title" id="page-title">
                                Clarity in a complex
                                <span className="page-title__accent"> cloud.</span>
                            </h1>
                            <p className="page-description">
                                Your workspace for cloud assets and security intelligence.
                                Explore your inventory, then investigate findings as verified
                                data becomes available.
                            </p>
                        </div>

                        <a className="secondary-button page-heading__action" href="#assets">
                            <Icon name="boxes" size={16} />
                            <span>Explore inventory</span>
                            <Icon name="arrow" size={15} />
                        </a>
                    </section>

                    <section
                        className="panel hero-panel reveal reveal--2"
                        aria-labelledby="hero-title"
                    >
                        <div className="hero-grid-overlay" aria-hidden="true" />
                        <div className="hero-topline">
              <span className="hero-topline__mark">
                <Icon name="sparkles" size={14} />
                MIDNIGHT INTELLIGENCE
              </span>
                            <span className="hero-topline__tag">PLATFORM / 01</span>
                        </div>

                        <div className="hero-copy">
                            <div className="hero-kicker">
                                <span className="hero-kicker__dot" />
                                A clearer view of your cloud
                            </div>
                            <h2 className="hero-title" id="hero-title">
                                See the connections.
                                <br />
                                <span className="gradient-text">Understand the risk.</span>
                            </h2>
                            <p className="hero-description">
                                Start with your cloud inventory. Follow verified evidence,
                                understand exposure, and build a clearer picture of your
                                security posture.
                            </p>

                            <div className="hero-actions">
                                <a className="primary-button" href="#assets">
                                    Explore cloud assets
                                    <Icon name="arrow" size={16} />
                                </a>
                                <span className="hero-action-note">
                  <Icon name="shield" size={14} />
                  Evidence-first security
                </span>
                            </div>

                            <div className="hero-capabilities">
                <span>
                  <Icon name="cloud" size={14} />
                  Asset visibility
                </span>
                                <span>
                  <Icon name="activity" size={14} />
                  Risk investigation
                </span>
                                <span>
                  <Icon name="network" size={14} />
                  Relationship analysis
                </span>
                            </div>
                        </div>

                        <NetworkPreview />

                        <div className="hero-bottom-line" aria-hidden="true">
                            <span />
                        </div>
                    </section>

                    <div className="section-intro reveal reveal--3">
                        <div>
                            <div className="section-overline">YOUR ENVIRONMENT</div>
                            <h2 className="section-title">Security at a glance</h2>
                            <p className="section-subtitle">
                                Values remain unavailable until the corresponding verified
                                backend data is connected.
                            </p>
                        </div>
                        <span className="data-integrity-note">
              <Icon name="shield" size={14} />
              No estimated metrics
            </span>
                    </div>

                    <section className="metric-grid metric-grid--staggered" aria-label="Security metrics">
                        <MetricCard
                            label="Security risk score"
                            value="—"
                            footnote="Risk aggregation not connected"
                            icon="shield"
                            variant="violet"
                        />
                        <MetricCard
                            label="Critical findings"
                            value="—"
                            footnote="Findings summary not connected"
                            icon="alert"
                            variant="rose"
                        />
                        <MetricCard
                            label="Exposed assets"
                            value="—"
                            footnote="Exposure aggregation not connected"
                            icon="cloud"
                            variant="blue"
                        />
                        <MetricCard
                            label="Latest scan"
                            value="—"
                            footnote="Scan summary not connected"
                            icon="activity"
                            variant="cyan"
                        />
                    </section>

                    <div className="section-intro section-intro--activity reveal reveal--4">
                        <div>
                            <div className="section-overline">INVESTIGATION</div>
                            <h2 className="section-title">Your security activity</h2>
                            <p className="section-subtitle">
                                A focused view of findings and scan outcomes.
                            </p>
                        </div>
                    </div>

                    <section className="activity-grid activity-grid--staggered">
                        <article
                            className="panel activity-panel activity-panel--findings"
                            id="findings"
                        >
                            <div className="panel-heading">
                                <div>
                                    <div className="panel-overline">INVESTIGATE</div>
                                    <h3 className="panel-title">Recent findings</h3>
                                    <p className="panel-description">
                                        Prioritize issues using verified severity and evidence.
                                    </p>
                                </div>
                                <span className="metric-icon metric-icon--rose">
                  <Icon name="alert" size={18} />
                </span>
                            </div>

                            <EmptyActivity
                                icon="shield"
                                title="Findings summary not connected"
                                description="The dashboard will show findings here when the findings list is connected to this view."
                            />

                            <a className="panel-footer-link" href="#assets">
                                Start with cloud inventory
                                <Icon name="arrow" size={15} />
                            </a>
                        </article>

                        <article
                            className="panel activity-panel activity-panel--scans"
                            id="scan-history"
                        >
                            <div className="panel-heading">
                                <div>
                                    <div className="panel-overline">OBSERVABILITY</div>
                                    <h3 className="panel-title">Scan history</h3>
                                    <p className="panel-description">
                                        Review actual scan runs and their outcomes.
                                    </p>
                                </div>
                                <span className="metric-icon metric-icon--blue">
                  <Icon name="history" size={18} />
                </span>
                            </div>

                            <EmptyActivity
                                icon="activity"
                                title="Scan history not connected"
                                description="No scan records are being presented in this panel yet. No scan status is inferred."
                            />

                            <div className="activity-panel__footnote">
                                <Icon name="lock" size={13} />
                                Only verified scan results should appear here.
                            </div>
                        </article>
                    </section>
                    <WorkspaceOverview />
                    <footer className="dashboard-footer">
            <span className="footer-brand">
              <Icon name="shield" size={14} />
              CLOUDTRAP AI
            </span>
                        <span>Midnight SOC · Cloud security intelligence</span>
                        <a href="#overview">
                            Back to top <Icon name="arrow" size={13} />
                        </a>
                    </footer>
                </main>
            </div>
        </div>
    );
}
