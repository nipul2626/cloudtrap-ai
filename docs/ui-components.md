# CloudTrap AI — UI Component Guide

## Purpose

This document describes the shared frontend UI primitives. These components
provide consistent styling and behavior; they do not implement backend
capabilities or business workflows by themselves.

## Available components

| Component | File | Intended use |
|---|---|---|
| Button | components/ui/button.tsx | Primary, secondary, ghost and danger actions |
| Badge | components/ui/badge.tsx | Neutral, informational and status labels |
| SeverityBadge | components/ui/badge.tsx | Finding severity labels |
| StatusBadge | components/ui/badge.tsx | Scan and finding status labels |
| Panel | components/ui/panel.tsx | Semantic content sections |
| PanelHeading | components/ui/panel.tsx | Panel title, description and action |
| EmptyState | components/ui/panel.tsx | Empty or unavailable content |
| Skeleton | components/ui/skeleton.tsx | Loading placeholders |
| MetricSkeleton | components/ui/skeleton.tsx | Metric-card loading placeholder |
| TextField | components/ui/input.tsx | Labelled text input |
| SearchField | components/ui/input.tsx | Search input |
| SelectField | components/ui/select.tsx | Labelled native select |
| DataTable | components/ui/data-table.tsx | Typed tabular data |
| Dialog | components/ui/dialog.tsx | Modal workflows |
| Drawer | components/ui/drawer.tsx | Side-panel workflows |
| Tooltip | components/ui/tooltip.tsx | Short contextual descriptions |

## Usage principles

- Prefer shared components over duplicated one-off controls.
- Preserve the established dark theme and design tokens in globals.css.
- Use SeverityBadge for security finding severity and StatusBadge for workflow
  state; do not communicate meaning through color alone.
- Use EmptyState when a real data collection is empty or unavailable.
- Use Skeleton only while content is loading, not as permanent placeholder data.
- Use Dialog for focused decisions and Drawer for contextual details or filters.
- Keep destructive actions explicit and require confirmation where appropriate.
- Tooltips should supplement, not replace, visible labels or essential instructions.
- Respect reduced-motion preferences and visible keyboard focus.
- Give every icon-only button an accessible name.
- Keep interactive controls operable with a keyboard.

## Accessibility review checklist

Before using a primitive in a production workflow, verify:

- [ ] Keyboard focus is visible.
- [ ] Inputs have accessible labels and associated error messages.
- [ ] Dialogs and drawers have meaningful accessible names.
- [ ] Escape and backdrop behavior are predictable.
- [ ] Focus stays within an open modal and returns to the invoking control.
- [ ] Nested overlays do not close or interfere with one another unexpectedly.
- [ ] Tooltips work with keyboard focus and do not contain essential information
      that is unavailable elsewhere.
- [ ] Reduced-motion settings disable nonessential transitions.
- [ ] Loading and empty states are announced appropriately.

## API integration boundary

UI primitives must not invent backend data or imply that unsupported features
are functional. Screen-level components should use the existing API client and
types. Features without a confirmed backend contract must be clearly identified
as demonstrations or unavailable functionality.
