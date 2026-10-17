
import type { CloudAsset } from "@/lib/api-types";

export type AssetFilters = {
    query: string;
    provider: string;
    exposure: string;
};

export function filterAssets(
    assets: CloudAsset[],
    filters: AssetFilters,
): CloudAsset[] {
    const query = filters.query.trim().toLocaleLowerCase();

    return assets.filter((asset) => {
        const matchesQuery =
            !query ||
            [
                asset.name,
                asset.external_id,
                asset.provider,
                asset.asset_type,
                asset.region ?? "",
                asset.exposure ?? "",
            ].some((value) => value.toLocaleLowerCase().includes(query));

        const matchesProvider =
            filters.provider === "all" ||
            asset.provider.toLocaleLowerCase() ===
            filters.provider.toLocaleLowerCase();

        const matchesExposure =
            filters.exposure === "all" ||
            (asset.exposure ?? "Unknown").toLocaleLowerCase() ===
            filters.exposure.toLocaleLowerCase();

        return matchesQuery && matchesProvider && matchesExposure;
    });
}

export function getAssetProviders(assets: CloudAsset[]): string[] {
    return [...new Set(assets.map((asset) => asset.provider).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b));
}

export function formatAssetDate(value?: string): string {
    if (!value) return "Not available";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "Not available";

    return new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date);
}

export function formatExposure(value: string | null): string {
    if (!value?.trim()) return "Unknown";

    return value
        .toLowerCase()
        .replaceAll("_", " ")
        .replace(/\b\w/g, (character) => character.toUpperCase());
}
