
"use client";

import { useEffect, useState } from "react";
import { getAssets, getWorkspaces } from "@/lib/api-client";
import type {
    CloudAsset,
    Workspace,
    WorkspacePageResponse,
} from "@/lib/api-types";
import { Badge } from "@/components/ui/badge";

import { Icon } from "@/components/ui/icon";
import { EmptyState, Panel, PanelHeading } from "@/components/ui/panel";
import { Skeleton } from "@/components/ui/skeleton";

type LoadState = "loading" | "ready" | "error";

export function WorkspaceOverview() {
    const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
    const [selectedWorkspaceId, setSelectedWorkspaceId] = useState("");
    const [assets, setAssets] = useState<CloudAsset[]>([]);
    const [workspaceState, setWorkspaceState] = useState<LoadState>("loading");
    const [assetState, setAssetState] = useState<LoadState>("loading");
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadWorkspaces() {
            setWorkspaceState("loading");

            try {
                const result: WorkspacePageResponse = await getWorkspaces();

                if (cancelled) return;

                setWorkspaces(result.content);
                setSelectedWorkspaceId((current) => {
                    if (current && result.content.some((item) => item.id === current)) {
                        return current;
                    }

                    return result.content[0]?.id ?? "";
                });
                setWorkspaceState("ready");
            } catch (error) {
                if (cancelled) return;

                setErrorMessage(
                    error instanceof Error
                        ? error.message
                        : "Unable to load your workspaces.",
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
        if (!selectedWorkspaceId) {
            return;
        }

        let cancelled = false;

        async function loadAssets() {
            setAssetState("loading");
            setErrorMessage("");

            try {
                const result = await getAssets(selectedWorkspaceId, 0, 50);

                if (cancelled) return;

                setAssets(result.items);
                setAssetState("ready");
            } catch (error) {
                if (cancelled) return;

                setAssets([]);
                setErrorMessage(
                    error instanceof Error
                        ? error.message
                        : "Unable to load assets for this workspace.",
                );
                setAssetState("error");
            }
        }

        void loadAssets();

        return () => {
            cancelled = true;
        };
    }, [selectedWorkspaceId]);

    const selectedWorkspace = workspaces.find(
        (workspace) => workspace.id === selectedWorkspaceId,
    );

    return (
        <section className="workspace-overview" id="assets">
            <Panel className="workspace-overview__panel">
                <PanelHeading
                    title="Workspace inventory"
                    description="Cloud assets returned by your connected CloudTrap API."
                    action={
                        <Badge tone={assetState === "ready" ? "success" : "neutral"} dot>
                            {assetState === "loading"
                                ? "Loading"
                                : assetState === "error"
                                    ? "Unavailable"
                                    : "API data"}
                        </Badge>
                    }
                />

                {workspaceState === "loading" ? (
                    <div className="workspace-overview__loading" aria-label="Loading workspaces">
                        <Skeleton height={38} />
                        <Skeleton height={62} />
                        <Skeleton height={62} />
                    </div>
                ) : workspaceState === "error" ? (
                    <EmptyState
                        icon="x-circle"
                        title="Workspace data unavailable"
                        description={errorMessage || "Check that the API is running and that you are signed in."}
                    />
                ) : workspaces.length === 0 ? (
                    <EmptyState
                        icon="boxes"
                        title="No workspaces found"
                        description="Create a workspace before viewing cloud inventory."
                    />
                ) : (
                    <>
                        <label className="workspace-overview__label" htmlFor="workspace-select">
                            Active workspace
                        </label>

                        <select
                            id="workspace-select"
                            className="workspace-overview__select"
                            value={selectedWorkspaceId}
                            onChange={(event) => setSelectedWorkspaceId(event.target.value)}
                        >
                            {workspaces.map((workspace) => (
                                <option key={workspace.id} value={workspace.id}>
                                    {workspace.name}
                                </option>
                            ))}
                        </select>

                        {selectedWorkspace ? (
                            <p className="workspace-overview__meta">
                                Workspace slug: <code>{selectedWorkspace.slug}</code>
                            </p>
                        ) : null}

                        <div className="workspace-overview__inventory-heading">
                            <div>
                                <h3 className="panel-title">Cloud assets</h3>
                                <p className="panel-description">
                                    Showing up to 50 assets from this workspace.
                                </p>
                            </div>
                            <span className="workspace-overview__count">
                {assetState === "ready" ? assets.length : "—"}
              </span>
                        </div>

                        {assetState === "loading" ? (
                            <div className="workspace-overview__loading" aria-label="Loading assets">
                                <Skeleton height={58} />
                                <Skeleton height={58} />
                                <Skeleton height={58} />
                            </div>
                        ) : assetState === "error" ? (
                            <EmptyState
                                icon="x-circle"
                                title="Assets could not be loaded"
                                description={errorMessage || "The API did not return asset inventory."}
                            />
                        ) : assets.length === 0 ? (
                            <EmptyState
                                icon="cloud"
                                title="No assets in this workspace"
                                description="No asset records were returned for this workspace."
                            />
                        ) : (
                            <div className="workspace-overview__asset-list">
                                {assets.map((asset) => (
                                    <article className="workspace-overview__asset" key={asset.id}>
                    <span className="workspace-overview__asset-icon">
                      <Icon name="cloud" size={18} />
                    </span>

                                        <div className="workspace-overview__asset-main">
                                            <strong>{asset.name || asset.external_id}</strong>
                                            <span>
                        {asset.provider} · {asset.asset_type}
                      </span>
                                            <span>
                        {asset.region || "Region not provided"}
                      </span>
                                        </div>

                                        <Badge tone="neutral">
                                            {asset.criticality == null
                                                ? "Unrated"
                                                : `Criticality ${asset.criticality}`}
                                        </Badge>
                                    </article>
                                ))}
                            </div>
                        )}
                    </>
                )}
            </Panel>
        </section>
    );
}
