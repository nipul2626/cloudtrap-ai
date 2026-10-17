# CloudTrap AI — Midnight SOC visual upgrade

This source bundle contains a visual and interaction pass over the existing Next.js frontend.

## What changed

- Added layered ambient lighting, staggered page entrances, reduced-motion handling, and a reusable motion vocabulary in `src/app/globals.css`.
- Enhanced the dashboard hero with a decorative animated topology preview, orbit rings, particles, refined gradient typography, and a subtle scan shimmer. The topology is explicitly illustrative and is not presented as live telemetry.
- Refined metric and activity panels with entrance timing, hover depth, animated accents, and improved focus/interaction states without inventing metric values.
- Improved the sidebar, navigation, buttons, workspace asset rows, and small-screen behavior.
- Added client-side asset filtering by name, external ID, provider, type, and region in `src/components/dashboard/workspace-overview.tsx`.
- Preserved the existing API client, session/CSRF behavior, API types, workspace loading flow, and existing route structure.

## Apply to your local project

Extract this ZIP into `D:\Projects\cloudtrap-ai\apps\web` and allow it to overwrite the matching `src` files. The ZIP contains the frontend `src` tree plus this note; it does not include `.env.local`, `node_modules`, or build output.

Then run from PowerShell:

```powershell
Set-Location "D:\Projects\cloudtrap-ai"
npm --prefix apps/web run lint
if ($LASTEXITCODE -ne 0) { throw "Lint failed" }
npm --prefix apps/web run build
if ($LASTEXITCODE -ne 0) { throw "Build failed" }
git diff --check
if ($LASTEXITCODE -ne 0) { throw "Diff check failed" }
```

Review the resulting diff before staging. Keep unrelated backend files unstaged and do not use `git add .`.

## Validation note

The uploaded bundle did not include installed dependencies or `node_modules`, and this environment cannot run the project's full Next.js build without them. Both edited TSX files were checked with the TypeScript transpiler for syntax. Run the lint and production build commands above in the actual repository before committing.
