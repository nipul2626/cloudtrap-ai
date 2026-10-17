@'
# CloudTrap AI — Frontend Design System

Status: Stage 2, Batch 1
Scope: Design tokens and reusable component standards
Dependencies: Existing Next.js, React, and Tailwind CSS setup

## 1. Design principles

- Dark-first security operations interface.
- High information density without sacrificing readability.
- Real API data must be distinguishable from demo or placeholder content.
- Shared tokens and components take precedence over screen-specific styling.
- Keyboard access, visible focus, and reduced motion are required.
- Existing application behavior must be preserved during visual changes.

## 2. Color tokens

| Token | Purpose |
| --- | --- |
| `--background` | Application canvas |
| `--surface` | Default surface |
| `--surface-raised` | Elevated surface |
| `--surface-hover` | Interactive hover surface |
| `--foreground` | Primary text |
| `--muted-strong` | Secondary text |
| `--muted` | Tertiary text |
| `--violet` | Primary brand accent |
| `--violet-bright` | Highlight and active navigation |
| `--blue` | Informational accent and keyboard focus |
| `--critical` | Critical severity and failure |
| `--warning` | Warning severity |
| `--success` | Successful or healthy state |
| `--info` | Informational state |

Do not communicate severity through color alone. Include a readable label or icon.

## 3. Spacing

Use the shared spacing scale:

- `--space-1`: 4px
- `--space-2`: 8px
- `--space-3`: 12px
- `--space-4`: 16px
- `--space-5`: 20px
- `--space-6`: 24px
- `--space-8`: 32px
- `--space-10`: 40px
- `--space-12`: 48px

Use consistent spacing within cards, between related controls, and between page sections.

## 4. Shape and elevation

Use `--radius-sm`, `--radius-md`, `--radius-lg`, and `--radius-xl`
for progressively larger surfaces.

Use `--shadow-panel` for restrained panel elevation and
`--shadow-overlay` for overlays. Avoid excessive glow effects.

## 5. Typography

- Primary text uses `--text-primary`.
- Secondary text uses `--text-secondary`.
- Tertiary labels use `--text-tertiary`.
- Technical identifiers and tabular values may use `--font-mono`.
- Body text must remain readable at normal browser zoom.
- Status labels must not depend on color alone.

The current system font stack is retained. No external font dependency
is introduced in this batch.

## 6. Component inventory

Existing shared components:

- `Button` and `IconButton`
- `Badge`, `SeverityBadge`, and `StatusBadge`
- `Panel`, `PanelHeading`, and `EmptyState`
- `Icon`
- `Skeleton`

Before creating a new component, check whether an existing primitive can
support the required behavior without breaking its current consumers.

Planned shared components:

- Text input and search field
- Select and filter controls
- Data table
- Tooltip
- Dialog and confirmation dialog
- Drawer and inspector panel
- Toast and notification region
- Pagination
- Loading, error, and empty-state patterns

Add these incrementally when a screen needs them. Do not introduce
unused abstractions solely to complete an inventory.

## 7. Severity and resource status

Severity mapping:

| Severity | Treatment |
| --- | --- |
| Critical | Critical badge |
| High | Critical badge with explicit High label |
| Medium | Warning badge |
| Low | Success badge |
| Informational | Info badge |

Operational status mapping:

| Status | Treatment |
| --- | --- |
| Queued | Neutral |
| Running | Info |
| Completed | Success |
| Failed | Critical |
| Open | Warning |
| Resolved | Success |

Resource health and scan execution status are different concepts and
must not be conflated.

## 8. Interaction and motion

- Fast transitions: approximately 120ms.
- Standard transitions: approximately 180ms.
- Slow transitions: approximately 260ms.
- Use `--ease-standard` for shared transitions.
- Avoid animating layout-heavy properties when transforms or opacity suffice.
- Respect `prefers-reduced-motion`.
- Decorative animation must never be necessary to understand application state.
- Focus must remain visible and keyboard navigation must remain usable.

Motion presets and a global decorative-effects preference will be
implemented in a later stage, after the screen structure is stable.

## 9. Icons and dependencies

The project already contains a local SVG icon component.
Reuse it before considering an additional icon package.

Do not add React Flow, an animation library, or a state-management
package until a specific approved feature requires it and compatibility
has been checked against the installed dependency versions.

## 10. Data integrity

- Never fabricate API counts, findings, assets, or scan history.
- Clearly label demo-only content.
- Represent loading, empty, error, and partial-data states separately.
- Do not claim real-time behavior unless a supported event or polling
  mechanism has been verified.
- Keep backend capability gaps documented in the API-to-screen matrix.

## 11. Acceptance checklist

- [ ] Existing dashboard layout remains intact.
- [ ] Existing components still compile.
- [ ] Focus indicators are visible during keyboard navigation.
- [ ] Reduced-motion preferences are respected.
- [ ] Severity and status labels remain understandable without color.
- [ ] No new runtime dependency was introduced.
- [ ] Lint, TypeScript, and production build pass.
  '@ | Set-Content -Path "docs/frontend-design-system.md" -Encoding utf8