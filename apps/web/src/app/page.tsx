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
    | "close";

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
    activity: (
        <path d="M3 12h4l3-8 4 16 3-8h4" />
    ),
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
  };

  return <svg {...common}>{paths[name]}</svg>;
}

const navigation = [
  { label: "Dashboard", icon: "dashboard", href: "/", active: true },
  { label: "Assets", icon: "boxes", href: "#assets" },
  { label: "Findings", icon: "alert", href: "#findings" },
  { label: "Attack Graph", icon: "network", href: "#attack-graph" },
  { label: "AI Copilot", icon: "bot", href: "#ai-copilot" },
  {
    label: "Remediation Simulator",
    icon: "sliders",
    href: "#simulator",
  },
  { label: "Scan History", icon: "history", href: "#scan-history" },
  { label: "Reports", icon: "report", href: "#reports" },
] as const;

const metrics = [
  {
    label: "Security risk score",
    value: "—",
    footnote: "Awaiting connected risk data",
    icon: "shield",
  },
  {
    label: "Critical findings",
    value: "—",
    footnote: "Awaiting findings data",
    icon: "alert",
  },
  {
    label: "Exposed assets",
    value: "—",
    footnote: "Awaiting inventory data",
    icon: "cloud",
  },
  {
    label: "Last scan",
    value: "—",
    footnote: "No scan status loaded",
    icon: "activity",
  },
] as const;

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
              href="/"
              aria-label="CloudTrap AI home"
              onClick={onNavigate}
          >
        <span className="brand-mark">
          <Icon name="shield" size={23} />
        </span>
          <span>
          <span className="brand-name">CLOUDTRAP AI</span>
          <span className="brand-caption" style={{ display: "block" }}>
            Cloud security intelligence
          </span>
        </span>
        </Link>

        <div className="nav-section-label">Workspace</div>
        <nav className="nav-list" aria-label="Main navigation">
          {navigation.map((item) => (
              <a
                  key={item.label}
                  href={item.href}
                  className="nav-link"
                  aria-current={"active" in item && item.active ? "page" : undefined}
                  onClick={onNavigate}
              >
            <span className="nav-icon">
              <Icon name={item.icon} size={18} />
            </span>
                <span>{item.label}</span>
              </a>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="workspace-card">
            <div className="workspace-label">Current workspace</div>
            <div className="workspace-name">CloudTrap Demo Workspace</div>
            <div className="workspace-status">
              <span className="status-dot" aria-hidden="true" />
              <span>Workspace selected</span>
            </div>
          </div>
          <a
              className="nav-link"
              href="#settings"
              style={{ marginTop: 10 }}
              onClick={onNavigate}
          >
          <span className="nav-icon">
            <Icon name="settings" />
          </span>
            <span>Settings</span>
          </a>
        </div>
      </aside>
  );
}

function MetricCard({
                      label,
                      value,
                      footnote,
                      icon,
                    }: {
  label: string;
  value: string;
  footnote: string;
  icon: IconName;
}) {
  return (
      <article className="panel metric-card">
        <div className="metric-topline">
          <span className="metric-label">{label}</span>
          <span className="metric-icon">
          <Icon name={icon} size={18} />
        </span>
        </div>
        <div className="metric-value" aria-label={`${label}: unavailable`}>
          {value}
        </div>
        <div className="metric-footnote">{footnote}</div>
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

export default function DashboardPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    if (!sidebarOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setSidebarOpen(false);
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [sidebarOpen]);

  function closeSidebar() {
    setSidebarOpen(false);
  }

  return (
      <div className="app-shell">
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
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <button
                  type="button"
                  className="icon-button mobile-menu-button"
                  aria-label={sidebarOpen ? "Close navigation" : "Open navigation"}
                  aria-expanded={sidebarOpen}
                  onClick={() => setSidebarOpen((current) => !current)}
              >
                <Icon name={sidebarOpen ? "close" : "menu"} size={20} />
              </button>
              <div>
                <div className="breadcrumb">Workspace / Overview</div>
                <div className="topbar-title">Security Command Center</div>
              </div>
            </div>

            <div className="topbar-actions">
              <button
                  type="button"
                  className="secondary-button"
                  aria-label="Search (not available yet)"
                  title="Global search will be connected in a later phase"
                  disabled
              >
                <Icon name="search" size={16} />
                <span>Search</span>
              </button>
              <button
                  type="button"
                  className="icon-button"
                  aria-label="Security data connection status"
                  title="Backend connection status will be integrated in a later phase"
                  disabled
              >
                <Icon name="activity" size={17} />
              </button>
            </div>
          </header>

          <main className="page-content">
            <section className="page-heading" aria-labelledby="page-title">
              <div>
                <div className="eyebrow">Cloud security overview</div>
                <h1 className="page-title" id="page-title">
                  Your security command center.
                </h1>
                <p className="page-description">
                  Explore your cloud posture, investigate findings, and
                  understand risk across your environment—all from one place.
                </p>
              </div>
              <button
                  type="button"
                  className="primary-button"
                  disabled
                  title="Scan creation will be connected in the scan integration phase"
              >
                <Icon name="plus" size={17} />
                <span>Start a scan</span>
              </button>
            </section>

            <section className="panel hero-panel" aria-labelledby="hero-title">
              <div className="hero-copy">
                <div className="eyebrow">Cloud security intelligence</div>
                <h2 className="hero-title" id="hero-title">
                  See the connections.
                  <br />
                  <span className="gradient-text">Understand the risk.</span>
                </h2>
                <p className="hero-description">
                  Connect cloud assets, security findings, and attack-path
                  analysis in one focused investigation experience.
                </p>
                <a className="secondary-button" href="#assets">
                  Explore the workspace
                  <Icon name="arrow" size={16} />
                </a>
              </div>

              <div
                  className="hero-network"
                  aria-label="Decorative illustration of connected cloud resources"
                  role="img"
              >
                <div className="network-orbit" />
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
                <Icon name="shield" size={30} />
              </span>
              </div>
            </section>
              <WorkspaceOverview />
            <section aria-labelledby="metrics-title">
              <div className="section-heading">
                <div>
                  <h2 className="section-title" id="metrics-title">
                    Security at a glance
                  </h2>
                  <p className="section-subtitle">
                    Metrics will populate from verified backend data.
                  </p>
                </div>
              </div>

              <div className="metric-grid">
                {metrics.map((metric) => (
                    <MetricCard
                        key={metric.label}
                        label={metric.label}
                        value={metric.value}
                        footnote={metric.footnote}
                        icon={metric.icon}
                    />
                ))}
              </div>
            </section>

            <section aria-labelledby="activity-title">
              <div className="section-heading">
                <div>
                  <h2 className="section-title" id="activity-title">
                    Recent activity
                  </h2>
                  <p className="section-subtitle">
                    Findings and scan history from your workspace.
                  </p>
                </div>
              </div>

              <div className="activity-grid">
                <article className="panel activity-panel" id="findings">
                  <div className="panel-heading">
                    <div>
                      <h3 className="panel-title">Recent findings</h3>
                      <p className="panel-description">
                        Prioritize what needs attention.
                      </p>
                    </div>
                    <span className="metric-icon">
                    <Icon name="alert" size={17} />
                  </span>
                  </div>
                  <EmptyActivity
                      icon="shield"
                      title="Findings not loaded yet"
                      description="Verified findings will appear here after API integration."
                  />
                </article>

                <article className="panel activity-panel" id="scan-history">
                  <div className="panel-heading">
                    <div>
                      <h3 className="panel-title">Scan history</h3>
                      <p className="panel-description">
                        Track actual scan runs and outcomes.
                      </p>
                    </div>
                    <span className="metric-icon">
                    <Icon name="history" size={17} />
                  </span>
                  </div>
                  <EmptyActivity
                      icon="activity"
                      title="No scan status loaded"
                      description="This panel will show real scan activity once connected."
                  />
                </article>
              </div>
            </section>

            <footer
                style={{
                  padding: "26px 0 8px",
                  color: "var(--muted)",
                  fontSize: 11,
                  textAlign: "center",
                }}
            >
              CloudTrap AI · Security intelligence workspace
            </footer>
          </main>
        </div>
      </div>
  );
}