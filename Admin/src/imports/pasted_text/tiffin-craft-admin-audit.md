Audit the entire TiffinCraft Admin Dashboard React app for production-level 
frontend functionality. This is a DIAGNOSTIC + FIX pass, not a rebuild — 
do not regenerate pages that already work correctly. Preserve all existing 
visual design, layout, and styling exactly as-is. Only fix behavior/logic.

CONTEXT
This is a frontend-only build for now — no backend/API exists yet. 
"Functional" here means real, working React logic using local state, 
mock data arrays, and proper event handling — NOT actual network 
requests. When backend is added later, these functions should be easy 
to swap from local state to API calls without restructuring the 
component logic.

═══════════════════════════════════════════
STEP 1 — AUDIT (do this first, report before fixing)
═══════════════════════════════════════════
Go through every page (Dashboard, Users, Cooks, Meals, Orders, Reviews, 
Payments, Earnings, Reports, Settings, Support) and every shared 
component (Sidebar, TopBar, Badge, Table, Pagination, Modals, Tabs, 
Buttons). For each interactive element, classify it as one of:

- ✅ WORKING — genuinely functional (state changes, real filtering, 
  real validation, etc.)
- ⚠️ FAKE — looks interactive (hover state, cursor pointer) but does 
  nothing on click/change, or is hardcoded to always show the same 
  result regardless of input
- ❌ MISSING — no handler at all, purely decorative

Produce this audit as a checklist/table BEFORE writing any fix code, 
so I can see the full picture first. Group by page.

═══════════════════════════════════════════
STEP 2 — SPECIFIC FUNCTIONALITY TO VERIFY/FIX PER AREA
═══════════════════════════════════════════

GLOBAL / SHARED
- Sidebar nav: clicking each item must actually route to that page 
  (real routing, not just visual active-state change) and the correct 
  page component must render
- Active nav state must be derived from actual current route, not a 
  manually toggled boolean that can desync from the URL
- Kebab/three-dot menus (used in Users, Orders, Reviews tables): must 
  open a real dropdown on click, close on outside click or Escape, 
  and each menu item (e.g. "Edit", "Delete", "View Details") must have 
  a real onClick — at minimum updating local state or opening a modal, 
  even if it doesn't hit an API yet
- All modals (Add User, filters, confirmations): must actually open/
  close via real state, trap focus reasonably, close on backdrop click 
  and Escape key, and not be permanently mounted-but-hidden with dead 
  buttons inside

TABLES (Users, Orders, Reviews) 
- Search inputs must actually filter the visible rows in real time 
  against the mock dataset (not just accept typing with no effect)
- Filter tab pills ("Pending (23)", "Processing (45)", etc.): clicking 
  must actually filter the table to matching rows, and the count in 
  the pill label should reflect the real filtered count from the 
  dataset, not a hardcoded number
- Column sort (if headers look clickable, or add basic sort if 
  reasonable): clicking a sortable header should sort ascending/
  descending and toggle direction on repeat click
- Pagination: Previous/Next and page number buttons must actually 
  change which slice of data is displayed, disable Previous on page 1 
  and Next on last page, and the "Showing X to Y of Z results" text 
  must be computed from real state, not hardcoded
- Row checkboxes: individual checkbox state must persist per row, 
  header checkbox must "select all" and correctly reflect indeterminate 
  state when some (not all) rows are selected
- Status badges must derive their color/label from the actual data 
  field on each row, not be visually hardcoded per row

FORMS (Add User, Settings, any input forms)
- Must have real controlled inputs (value + onChange), not uncontrolled 
  decorative inputs
- Must have basic validation before "submit" is allowed to succeed 
  (required fields, email format, etc.) with visible error states — 
  not just a console.log placeholder
- Submit should update local mock data state so the new/edited item 
  actually appears in the relevant table afterward (e.g. Add User 
  really adds a row to the Users table state)

CHARTS (Dashboard line charts, Reports donut + bar chart)
- Confirm chart data is coming from a defined data structure (array/
  object) rather than hardcoded SVG paths, so it's realistic that 
  swapping in API data later just means replacing that data source
- Hover tooltips on chart data points/bars should actually show the 
  value on hover, not be purely visual

DATE RANGE PICKER (Dashboard top-right)
- Clicking it should open a real picker/dropdown, and selecting a 
  range should update the displayed label — even if it doesn't yet 
  refetch different mock data, the UI interaction itself must be real

BUTTONS THAT IMPLY AN ACTION
- "Export CSV" / "Export Report": at minimum should trigger a real 
  client-side CSV/file generation from the current mock dataset 
  (not just be a dead button) — this is achievable frontend-only
- "+ Add User" / any "Add X" button: must open the relevant form/modal
- "View All Orders →" and similar links: must route to the correct 
  full page, not be a dead link

═══════════════════════════════════════════
STEP 3 — MOCK DATA LAYER
═══════════════════════════════════════════
Consolidate mock data into a clean, centralized structure (e.g. a 
/data or /mocks folder with typed arrays matching your TypeScript 
interfaces) rather than inline hardcoded arrays scattered across 
components. This makes the future backend swap straightforward — 
components should read from a data-fetching hook/function 
(e.g. useUsers(), useOrders()) that currently returns local mock 
state, so later it can be swapped for a real fetch without touching 
component logic.

═══════════════════════════════════════════
RULES
═══════════════════════════════════════════
- Do NOT change visual design, spacing, colors, or layout of anything 
  that is already visually correct — this is a logic/functionality 
  pass only
- Do NOT add backend calls, fetch(), or API integration — everything 
  stays local-state/mock-data driven for now
- When you fix a ⚠️ FAKE or ❌ MISSING item, briefly note what you 
  changed and why, so I can track what was touched
- If something is ambiguous about intended behavior (e.g. what should 
  a specific kebab menu action actually do with no backend), make a 
  reasonable frontend-only decision and flag it clearly as 
  "[ASSUMED BEHAVIOR — confirm]" rather than silently guessing
- Prioritize fixes in this order: 1) broken navigation/routing, 
  2) non-functional tables (search/filter/pagination), 3) dead 
  buttons/modals, 4) forms without validation, 5) chart data structure 
  cleanup

Give me the Step 1 audit table first. Wait for my confirmation before 
proceeding to Step 2 fixes, so I can flag if anything you classified 
differs from what I intended.