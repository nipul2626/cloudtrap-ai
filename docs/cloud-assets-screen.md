
# Cloud Assets Screen

## Route

`/assets`

## Data source

The screen uses the existing frontend API client:

- `getWorkspaces()` — loads available workspaces.
- `getAssets(workspaceId, 0, 50)` — loads the first page of cloud assets.

It uses the existing `Workspace` and `CloudAsset` API types.

## Features

- Workspace selection.
- Asset search by name, external ID, provider, type, region, and exposure.
- Provider and exposure filters.
- Loading, error, empty, and no-match states.
- Responsive inventory table.
- Asset count, provider count, and filtered count.
- Clear filters action.

## Data integrity

- The screen displays API-returned asset records.
- Summary counts cover only the currently loaded page.
- A maximum of 50 assets is requested.
- No risk score, exposure classification, or security finding is fabricated.
- Missing region, exposure, date, and criticality values are shown as unavailable or unknown.

## Limitations

- Pagination controls are not implemented in this batch.
- Asset details and remediation actions are not implemented.
- The screen depends on the existing API session and workspace permissions.
