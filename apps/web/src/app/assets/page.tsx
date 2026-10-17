
"use client";

import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Icon } from "@/components/ui/icon";
import { Button } from "@/components/ui/button";
import { EmptyState, Panel, PanelHeading } from "@/components/ui/panel";
import { Skeleton } from "@/components/ui/skeleton";
import { getAssets, getWorkspaces } from "@/lib/api-client";
import type {
    CloudAsset,
    Workspace,
    WorkspacePageResponse,
} from "@/lib/api-types";
import {
    filterAssets,
    formatAssetDate,
    formatExposure,
    getAssetProviders,
} from "@/lib/asset-utils";
import styles from "./assets.module.css";

type LoadState = "loading" | "ready" | "error";

export default function AssetsPage() {
    const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
    const [workspaceId, setWorkspaceId] = useState("");
    const [assets, setAssets] = useState<CloudAsset[]>([]);
    const [workspaceState, setWorkspaceState] =
        useState<LoadState>("loading");
    const [assetState, setAssetState] = useState<LoadState>("loading");
    const [error, setError] = useState("");
    const [query, setQuery] = useState("");
    const [provider, setProvider] = useState("all");
    const [exposure, setExposure] = useState("all");

    useEffect(() => {
        let cancelled = false;

        async function loadWorkspaces() {
            try {
                const result: WorkspacePageResponse = await getWorkspaces();

                if (cancelled) return;

                setWorkspaces(result.content);
                setWorkspaceId((current) =>
                    result.content.some((workspace) => workspace.id === current)
                        ? current
                        : result.content[0]?.id ?? "",
                );
                setWorkspaceState("ready");
            } catch (cause) {
                if (cancelled) return;

                setError(
                    cause instanceof Error
                        ? cause.message
                        : "Unable to load workspaces.",
                );
                setWorkspaceState("error");
            }
        }

        void loadWorkspaces();

        return () => {
            cancelled = true;
        };
    }, []);


    useEffect(() => {
        if (!workspaceId) {
            return;
        }

        let cancelled = false;

        async function loadAssets() {
            setAssetState("loading");
            setError("");

            try {
                const result = await getAssets(workspaceId, 0, 50);

                if (cancelled) return;

                setAssets(result.items);
                setAssetState("ready");
            } catch (cause) {
                if (cancelled) return;

                setAssets([]);
                setError(
                    cause instanceof Error
                        ? cause.message
                        : "Unable to load cloud assets.",
                );
                setAssetState("error");
            }
        }

        void loadAssets();

        return () => {
            cancelled = true;
        };
    }, [workspaceId]);

    const providers = useMemo(() => getAssetProviders(assets), [assets]);

    const filteredAssets = useMemo(
        () => filterAssets(assets, { query, provider, exposure }),
        [assets, query, provider, exposure],
    );

    const selectedWorkspace = workspaces.find(
        (workspace) => workspace.id === workspaceId,
    );

    function resetFilters() {
        setQuery("");
        setProvider("all");
        setExposure("all");
    }

    return (
        <AppShell>
            <main className={`page-content ${styles.page}`}>
                <header className={styles.heading}>
                    <div>
                        <p className={styles.eyebrow}>Cloud security / Inventory</p>
                        <h1 className={styles.title}>Cloud assets</h1>
                        <p className={styles.description}>
                            Explore the cloud resources returned by your connected
                            CloudTrap API. Search and filter the inventory by provider,
                            exposure, and resource details.
                        </p>
                    </div>

                    <Badge tone={assetState === "ready" ? "success" : "neutral"} dot>
                        {assetState === "loading"
                            ? "Loading inventory"
                            : assetState === "error"
                                ? "Inventory unavailable"
                                : "API inventory"}
                    </Badge>
                </header>

                <section className={styles.stats} aria-label="Inventory summary">
                    <Panel className={styles.statCard}>
                        <span className={styles.statLabel}>Loaded assets</span>
                        <strong className={styles.statValue}>
                            {assetState === "ready" ? assets.length : "—"}
                        </strong>
                        <span className={styles.statHint}>Current API page</span>
                    </Panel>

                    <Panel className={styles.statCard}>
                        <span className={styles.statLabel}>Providers</span>
                        <strong className={styles.statValue}>
                            {assetState === "ready" ? providers.length : "—"}
                        </strong>
                        <span className={styles.statHint}>Represented in this inventory</span>
                    </Panel>

                    <Panel className={styles.statCard}>
                        <span className={styles.statLabel}>Matching assets</span>
                        <strong className={styles.statValue}>
                            {assetState === "ready" ? filteredAssets.length : "—"}
                        </strong>
                        <span className={styles.statHint}>After applying filters</span>
                    </Panel>
                </section>

                <Panel className={styles.inventoryPanel}>
                    <PanelHeading
                        title="Inventory"
                        description="Select a workspace, then narrow down the returned resources."
                    />

                    <div className={styles.controls}>
                        <label className={styles.field}>
                            <span>Workspace</span>
                            <select
                                value={workspaceId}
                                onChange={(event) => setWorkspaceId(event.target.value)}
                                disabled={workspaceState !== "ready" || workspaces.length === 0}
                            >
                                <option value="">
                                    {workspaceState === "loading"
                                        ? "Loading workspaces…"
                                        : "Select a workspace"}
                                </option>
                                {workspaces.map((workspace) => (
                                    <option key={workspace.id} value={workspace.id}>
                                        {workspace.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className={styles.field}>
                            <span>Search assets</span>
                            <input
                                type="search"
                                value={query}
                                onChange={(event) => setQuery(event.target.value)}
                                placeholder="Name, ID, type, region…"
                                disabled={assetState !== "ready"}
                            />
                        </label>

                        <label className={styles.field}>
                            <span>Provider</span>
                            <select
                                value={provider}
                                onChange={(event) => setProvider(event.target.value)}
                                disabled={assetState !== "ready"}
                            >
                                <option value="all">All providers</option>
                                {providers.map((item) => (
                                    <option key={item} value={item}>
                                        {item}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className={styles.field}>
                            <span>Exposure</span>
                            <select
                                value={exposure}
                                onChange={(event) => setExposure(event.target.value)}
                                disabled={assetState !== "ready"}
                            >
                                <option value="all">All exposure states</option>
                                {[...new Set(
                                    assets.map((asset) => asset.exposure ?? "Unknown"),
                                )]
                                    .sort()
                                    .map((item) => (
                                        <option key={item} value={item}>
                                            {formatExposure(item)}
                                        </option>
                                    ))}
                            </select>
                        </label>
                    </div>

                    <div className={styles.inventoryMeta}>
                        <div>
                            <strong>{selectedWorkspace?.name ?? "Workspace inventory"}</strong>
                            <span>
                {assetState === "ready"
                    ? `${filteredAssets.length} matching of ${assets.length} loaded assets`
                    : "Asset data is shown only after a successful API response."}
              </span>
                        </div>

                        <Button
                            variant="secondary"
                            onClick={resetFilters}
                            disabled={!query && provider === "all" && exposure === "all"}
                        >
                            <Icon name="close" size={15} />
                            Clear filters
                        </Button>
                    </div>

                    {workspaceState === "loading" ? (
                        <div className={styles.skeletons} aria-label="Loading workspaces">
                            <Skeleton height={54} />
                            <Skeleton height={54} />
                            <Skeleton height={54} />
                        </div>
                    ) : workspaceState === "error" ? (
                        <EmptyState
                            icon="x-circle"
                            title="Workspaces unavailable"
                            description={error || "Check the API connection and your sign-in status."}
                        />
                    ) : workspaces.length === 0 ? (
                        <EmptyState
                            icon="boxes"
                            title="No workspaces found"
                            description="Create or join a workspace before viewing its cloud inventory."
                        />
                    ) : assetState === "loading" ? (
                        <div className={styles.skeletons} aria-label="Loading cloud assets">
                            <Skeleton height={64} />
                            <Skeleton height={64} />
                            <Skeleton height={64} />
                        </div>
                    ) : assetState === "error" ? (
                        <EmptyState
                            icon="x-circle"
                            title="Assets could not be loaded"
                            description={error || "The API did not return asset inventory."}
                        />
                    ) : filteredAssets.length === 0 ? (
                        <EmptyState
                            icon="search"
                            title={assets.length ? "No matching assets" : "No assets found"}
                            description={
                                assets.length
                                    ? "Try changing your search or filters."
                                    : "No asset records were returned for this workspace."
                            }
                        />
                    ) : (
                        <div className={styles.tableWrap}>
                            <table className={styles.table}>
                                <thead>
                                <tr>
                                    <th scope="col">Asset</th>
                                    <th scope="col">Provider / type</th>
                                    <th scope="col">Region</th>
                                    <th scope="col">Exposure</th>
                                    <th scope="col">Criticality</th>
                                    <th scope="col">Last seen</th>
                                </tr>
                                </thead>
                                <tbody>
                                {filteredAssets.map((asset) => (
                                    <tr key={asset.id}>
                                        <td>
                                            <div className={styles.assetName}>
                          <span className={styles.assetIcon}>
                            <Icon name="cloud" size={17} />
                          </span>
                                                <span>
                            <strong>{asset.name || asset.external_id}</strong>
                            <small>{asset.external_id}</small>
                          </span>
                                            </div>
                                        </td>
                                        <td>
                                            <span>{asset.provider}</span>
                                            <small className={styles.secondaryText}>
                                                {asset.asset_type}
                                            </small>
                                        </td>
                                        <td>{asset.region || "Not provided"}</td>
                                        <td>
                                            <Badge tone="neutral">
                                                {formatExposure(asset.exposure)}
                                            </Badge>
                                        </td>
                                        <td>
                                            {asset.criticality == null
                                                ? "Unrated"
                                                : asset.criticality}
                                        </td>
                                        <td>{formatAssetDate(asset.last_seen_at)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <p className={styles.disclaimer}>
                        Showing at most 50 assets from the current API page. Counts and
                        labels reflect returned data; no security posture or exposure
                        score is inferred by this screen.
                    </p>
                </Panel>
            </main>
        </AppShell>
    );
}
