Build a production-grade Admin Dashboard web app for "TiffinCraft" — 
a home-cooked meal subscription platform connecting home cooks with 
customers. This is the admin/internal panel only (not the customer 
or cook-facing app).

STACK & CONSTRAINTS
- React with TypeScript (.tsx) throughout, no .jsx files
- Functional components + hooks only, no class components
- Use a component library/primitives consistent with shadcn/ui patterns 
  (Card, Table, Badge, Button, Input, Tabs) if available in this 
  environment — otherwise build clean custom components with the same 
  visual quality
- Fully responsive: sidebar collapses to icons or drawer below 1024px
- I am attaching reference screenshots — match them pixel-for-pixel 
  on layout, spacing, type scale, and color where a screen is shown. 
  Do not "reimagine" or simplify what's in the screenshots.

═══════════════════════════════════════════
GLOBAL LAYOUT (applies to every page)
═══════════════════════════════════════════
- Fixed left sidebar, dark theme (near-black, ~#1A1A1A–#1F1F23), 
  full height, ~240px wide
- Sidebar top: green square logo mark + "TiffinCraft" wordmark (white, 
  bold) + "Admin Panel" subtitle (gray, small, below wordmark)
- Sidebar nav items in this exact order, each with a left-aligned icon 
  + label:
  Dashboard, Users, Cooks, Meals, Orders, Reviews, Payments, Earnings, 
  Reports, Settings, Support
- Active nav item: solid green pill/rounded background (~#22C55E or 
  similar brand green), white text, rounded corners (~8px)
- Inactive nav items: gray-400 text on transparent, hover state = 
  subtle lighter background (gray-800/10% white overlay) with smooth 
  150ms transition — no hard color jumps
- Sidebar bottom: fixed user card — green circular avatar with initials 
  "AU", "Admin User" (white, semibold) + "Super Admin" (gray, small) 
  stacked, pinned to bottom of sidebar with a top border separator
- Main content area: light gray background (~#F5F6F8), generous padding 
  (~32px), max content width with comfortable margins on large screens

TYPOGRAPHY
- Page titles: large bold (~28-32px), near-black
- Page subtitle (under title): gray-500, regular weight, smaller (~14px)
- Card/section headers: semibold, ~16-18px
- Body/table text: regular, ~14px, gray-700/900 depending on emphasis
- Numbers in stat cards: extra bold, large (~32-36px), tabular-nums

CARDS (used throughout — stat cards, table containers, chart containers)
- White background, rounded corners (~12-16px), subtle border 
  (1px, near-transparent gray) or soft shadow (not both heavy) 
- Consistent internal padding (~20-24px)
- Hover effect on interactive cards (e.g. clickable rows, cook cards): 
  subtle lift — slight shadow increase + 1-2px translateY on hover, 
  150-200ms ease transition, no jarring scale jumps

═══════════════════════════════════════════
PAGE 1 — DASHBOARD (/)
═══════════════════════════════════════════
- Header: "Welcome back, Admin! 👋" (bold, large) + subtitle 
  "Here's what's happening with TiffinCraft today." (gray)
- Top-right: date range picker pill showing something like 
  "📅 May 12 - May 18, 2025" — styled as a bordered rounded button, 
  hover state darkens border slightly

- Row of 4 stat cards (equal width, responsive grid → 2 cols on tablet, 
  1 on mobile):
  1. Total Users — big number, green "+12.8%" badge top-right of label
  2. Total Cooks — big number, green "+8.2%" badge
  3. Total Orders — big number, green "+18.3%" badge
  4. Total Revenue (₹ formatted, e.g. ₹1,45,230 — Indian number 
     formatting with commas) — green "+24.4%" badge
  Each card: small gray label top, large bold number below, 
  percentage change badge (green pill, small) positioned near the label

- Two-column row below stats (50/50 on desktop, stacked on mobile):
  LEFT: "Orders Overview" card — header with total orders number + 
    green % change inline, below it a smooth line/area chart 
    (purple/indigo line, ~#6366F1) over 7 days (May 12–May 18 labels 
    on x-axis), small dot markers on data points, subtle gradient 
    fill under the line, no heavy gridlines
  RIGHT: "Revenue Overview" card — same structure but green line 
    (~#22C55E), revenue total + % change header, smooth area chart

- Bottom two-column row (roughly 65/35 split):
  LEFT — "Recent Orders" card:
    - Header with "Recent Orders" title + "View All Orders →" link 
      (green text, right-aligned, hover underline)
    - Table columns: Order ID (blue/link-colored, e.g. #ORD-1234), 
      Customer, Cook, Amount (₹), Status
    - Status as colored pill badges: Delivered = green bg/green text, 
      Processing = orange/amber bg+text, Out for Delivery = blue 
      bg+text, Accepted = light green bg+text, Pending = yellow, 
      Cancelled = red — soft pastel backgrounds with matching darker 
      text color, rounded-full pills
    - Row hover: subtle background tint on the whole row, cursor pointer
  RIGHT — "Top Performing Cooks" card:
    - List of cooks: circular avatar placeholder, cook/kitchen name 
      (e.g. "Anita's Kitchen"), order count subtitle (e.g. "123+ orders"), 
      star rating right-aligned (⭐ 4.8 format)
    - Hover: subtle row highlight

═══════════════════════════════════════════
PAGE 2 — MANAGE USERS (/users)
═══════════════════════════════════════════
- Header: "Manage Users" (bold) + "View and manage all user data." 
  (gray subtitle) — with a green "+ Add User" button top-right 
  (rounded, white text, hover = slightly darker green + subtle scale)
- Filter tab pills below header: "All Users (2,345)" [active, solid 
  green], "Customers (1,938)", "Cooks (456)", "Admins (5)" — inactive 
  pills are white/bordered, hover state shows light gray background, 
  smooth transition on active state change
- Search input below tabs: full-width, icon-prefixed (search icon), 
  placeholder "Search users...", rounded border, focus state = 
  green border ring
- Table inside a card:
  Columns: checkbox (select-all in header), Name (with avatar circle 
  + name bold + email gray small below, stacked), Role (Customer/Cook 
  as plain text or subtle badge), Phone, Status (Active = green pill, 
  Inactive = red/pink pill), Joined On (date), Actions (vertical 
  three-dot kebab menu icon, opens a dropdown on click — not just 
  decorative, wire up basic open/close state)
  - Row hover: light background tint
  - Checkbox row select: row gets a subtle highlighted background when 
    checked
- Pagination footer: "Showing 1 to 10 of 2,345 results" left-aligned, 
  page number buttons right-aligned (← 1 2 3 ... 235 →), active page 
  = solid green square, inactive = white/bordered with hover state

═══════════════════════════════════════════
PAGE 3 — MANAGE ORDERS (/orders)
═══════════════════════════════════════════
- Header: "Manage Orders" + "View and track all order information." 
  subtitle, with "Export CSV" (outlined button) and "Filter" 
  (outlined button with filter icon) top-right, both with hover states
- Status filter tabs with live counts: "All Orders" [active], 
  "Pending (23)", "Processing (45)", "Completed (189)", 
  "Cancelled (12)" — same pill style as Users page tabs
- Search bar: "Search orders..." same style as Users page
- Table columns: Order ID (link-blue), Customer, Cook, Product 
  (meal name), Amount (₹), Status (same pill badge system as Dashboard 
  recent orders — Delivered/Processing/Pending/Cancelled), Date, 
  Actions (kebab menu)
- Pagination: same pattern as Users page, footer shows 
  "Showing 1 to 10 of 6,709 results"

═══════════════════════════════════════════
PAGE 4 — REVIEWS & RATINGS (/reviews)
═══════════════════════════════════════════
- Header: "Reviews & Ratings" + "Customer feedback and ratings 
  overview." subtitle
- Row of 4 stat cards:
  1. Average Rating — "4.6" with star icon, "out of 5.0" caption
  2. Total Reviews — "1,234" with "+18% this month" green caption
  3. 5 Star Reviews — "789" with "64% of total" caption
  4. Pending Reviews — "23" with "Need response" caption (consider 
     amber/warning tone on the number or icon here since it implies 
     action needed)
- Table inside card: Customer, Cook, Rating (star icons rendered 
  inline, filled stars in amber/gold up to the rating value, e.g. 
  ★★★★★ 5), Comment (italicized quoted text, truncate with ellipsis 
  if long), Date, Actions
  - Row hover: subtle tint
- Pagination footer: "Showing 1 to 10 of 1,234 results" + page buttons

═══════════════════════════════════════════
PAGE 5 — REPORTS & ANALYTICS (/reports)
═══════════════════════════════════════════
- Header: "Reports & Analytics" + "View detailed analytics and 
  performance reports." subtitle, "📊 Export Report" green button 
  top-right
- Row of 4 stat cards, each with % change badge top-right of card 
  (not just near label — top corner placement here):
  1. Total Orders — 6,709, +12.3%
  2. Total Revenue — ₹1.45L (lakh-formatted), +18.5%
  3. Active Users — 334, +8.2%
  4. Avg Order Value — ₹456, +5.1%
- Two-column row:
  LEFT — "Orders Breakdown" card: donut/pie chart (concentric ring 
  style as shown — blue, green, orange, yellow segments) with a 
  legend below showing colored dot + label + value + percentage for 
  each segment: Delivered 4,287 (64%), Processing 1,342 (20%), 
  Pending 804 (12%), Cancelled 276 (4%)
  RIGHT — "Revenue by Day" card: vertical bar chart, purple/indigo 
  bars (~#6366F1), Mon–Sun on x-axis, rounded bar tops, hover on bar 
  = slightly darker shade + tooltip showing exact value

═══════════════════════════════════════════
PAGES NOT SCREENSHOTTED (Cooks, Meals, Payments, Earnings, 
Settings, Support) — INFERRED, follow this guidance
═══════════════════════════════════════════
These weren't in my reference images, so build them consistent with 
the established design system rather than guessing new patterns:
- Cooks: same table/card pattern as Users, but columns relevant to 
  cooks — Kitchen Name, Owner Name, Rating, Total Orders, Verification 
  Status (pill badge), Joined Date, Actions
- Meals: grid or table of meals with image thumbnail, name, cook name, 
  price, category, availability toggle, actions
- Payments: table of transactions — Order ID, Customer, Cook, Amount, 
  Payment Method, Status (Paid/Pending/Refunded as pills), Date
- Earnings: stat cards (Total Platform Earnings, This Month, Cook 
  Payouts Pending) + a revenue trend chart, consistent with Dashboard 
  chart styling
- Settings: simple form-based sections (Profile, Notifications, 
  Security) in cards with labeled inputs/toggles
- Support: ticket list table similar to Orders pattern — Ticket ID, 
  User, Subject, Status, Date, Actions
Keep these structurally consistent but do not over-invest detail here 
— prioritize pixel accuracy on the 5 screenshotted pages first.

═══════════════════════════════════════════
INTERACTION DETAIL REQUIREMENTS
═══════════════════════════════════════════
- Every clickable element (nav items, buttons, table rows, tabs, 
  pagination, kebab menus) must have a real hover state — not just 
  cursor:pointer. Use 150-200ms ease transitions consistently, never 
  instant/jarring state changes
- Active/selected states must be visually distinct from hover states 
  (don't conflate them)
- Status badges/pills must use consistent color logic across ALL 
  pages — Delivered/Completed/Active/Paid = green tones, 
  Processing/Pending = amber/orange tones, Cancelled/Inactive/Failed 
  = red tones, Out for Delivery = blue tones. Reuse one badge 
  component everywhere rather than redefining colors per page
- Charts should feel "alive" — smooth curves on line charts (not 
  jagged straight segments), rounded bar tops, soft gradient fills 
  under area charts where applicable

DATA
- Use realistic mock/seed data matching exactly what's shown in the 
  reference screenshots where a screen is provided (same names, same 
  numbers) so the visual comparison is exact
- For inferred pages, generate plausible mock data consistent with 
  the TiffinCraft domain (Indian names, ₹ currency, meal names like 
  Paneer Tikka/Chole Bhature/Butter Chicken/Dal Makhani)

Build this as a working, navigable multi-page React app with routing 
between all 11 sidebar sections, not static mockup images.