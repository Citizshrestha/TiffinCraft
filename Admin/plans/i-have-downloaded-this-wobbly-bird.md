# How to Run This Project Locally

## Context
This is a Figma Make project built with React 18 + TypeScript + Vite 6 + Tailwind CSS 4. The package manager is **pnpm**. There is no `dev` script in `package.json` — the dev server is launched directly via the `vite` CLI.

---

## Steps to Run

### 1. Install Node.js
Make sure you have **Node.js 18+** installed.  
Download from: https://nodejs.org

### 2. Install pnpm
```bash
npm install -g pnpm
```

### 3. Install dependencies
```bash
cd <your-project-folder>
pnpm install
```

### 4. Start the dev server
```bash
pnpm exec vite
```
or equivalently:
```bash
npx vite
```

This starts the Vite dev server. Open the URL shown in the terminal (usually **http://localhost:5173**) in your browser.

---

## Build for Production
```bash
pnpm build
# or
pnpm exec vite build
```
Output goes to `dist/`.

---

## Notes
- No `.env` file is required — there are no external API keys needed.
- The project uses **pnpm workspaces** (`pnpm-workspace.yaml`), so always use `pnpm` (not `npm` or `yarn`) to install packages.
- There is no `start` or `dev` script defined in `package.json`; use `pnpm exec vite` directly.
