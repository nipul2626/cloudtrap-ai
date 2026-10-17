
"use client";

import { Icon } from "@/components/ui/icon";
import type { IconName } from "@/components/ui/icon";
import { AppShell } from "@/components/layout/app-shell";
import { WorkspaceOverview } from "@/components/dashboard/workspace-overview";

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
                <path
                    className="network-connection network-connection--secondary"
                    d="M76 55 267 58 274 190 70 193Z"
                />
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

export default function DashboardPage() {
    return (
        <AppShell>
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

                <section
                    className="metric-grid metric-grid--staggered"
                    aria-label="Security metrics"
                >
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
        </AppShell>
    );
}
