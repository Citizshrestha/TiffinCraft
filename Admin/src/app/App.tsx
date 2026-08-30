import React, { useEffect, useState } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router";
import { Sidebar } from "./components/Sidebar";
import { DashboardPage } from "./components/DashboardPage";
import { ManageUsersPage } from "./components/ManageUsersPage";
import { CooksPage } from "./components/CooksPage";
import { MealsPage } from "./components/MealsPage";
import { ManageOrdersPage } from "./components/ManageOrdersPage";
import { ReviewsPage } from "./components/ReviewsPage";
import { PaymentsPage } from "./components/PaymentsPage";
import { RefundsPage } from "./components/RefundsPage";
import { EarningsPage } from "./components/EarningsPage";
import { CommissionSettlementsPage } from "./components/CommissionSettlementsPage";
import { NotificationBell } from "./components/NotificationBell";
import { ReportsPage } from "./components/ReportsPage";
import { SettingsPage } from "./components/SettingsPage";
import { SupportPage } from "./components/SupportPage";
import { AdminUser, getStoredAdmin, getToken, loginAdmin, logoutAdmin } from "./api/authApi";
import { capitalizeRole } from "./utils/format";

function LoginPage({ onLogin }: { onLogin: (admin: AdminUser) => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError("");
    setSubmitting(true);
    try {
      const admin = await loginAdmin(email.trim(), password);
      onLogin(admin);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invalid email or password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center p-4"
      style={{ background: "#f2f2f5", fontFamily: "Inter, sans-serif" }}
    >
      <div
        className="bg-white rounded-[16px] p-6 sm:p-10 w-full max-w-[420px]"
        style={{ boxShadow: "0px 4px 24px rgba(0,0,0,0.08)" }}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8">
          <img src="/tiffin-logo.png" alt="TiffinCraft logo" className="w-10 h-10 object-contain" />
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>
              TiffinCraft
            </p>
            <p style={{ fontFamily: "Inter", fontWeight: 400, fontSize: 12, color: "#9499a6" }}>
              Admin Panel
            </p>
          </div>
        </div>

        <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 24, color: "#1c1f29", marginBottom: 4 }}>
          Welcome back 👋
        </p>
        <p style={{ fontFamily: "Inter", fontWeight: 400, fontSize: 14, color: "#9499a6", marginBottom: 28 }}>
          Sign in to your admin account
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <p className="mb-1" style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29" }}>
              Email Address
            </p>
            <input
              type="email"
              className="w-full px-4 py-3 rounded-[8px] text-[14px] outline-none transition-all duration-150"
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29" }}
              placeholder="admin@tiffincraft.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setError(""); }}
              onFocus={(e) => (e.target.style.borderColor = "#57b869")}
              onBlur={(e) => (e.target.style.borderColor = "#e5e8ed")}
            />
          </div>
          <div>
            <p className="mb-1" style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29" }}>
              Password
            </p>
            <input
              type="password"
              className="w-full px-4 py-3 rounded-[8px] text-[14px] outline-none transition-all duration-150"
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29" }}
              placeholder="••••••••"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setError(""); }}
              onFocus={(e) => (e.target.style.borderColor = "#57b869")}
              onBlur={(e) => (e.target.style.borderColor = "#e5e8ed")}
            />
          </div>

          {error && (
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#f25959" }}>{error}</p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 rounded-[8px] text-white text-[14px] mt-2 cursor-pointer transition-all duration-150 hover:brightness-95"
            style={{
              background: "#57b869",
              fontFamily: "Inter",
              fontWeight: 600,
              border: "none",
              opacity: submitting ? 0.7 : 1,
            }}
          >
            {submitting ? "Signing in..." : "Sign In"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default function App() {
  const [admin, setAdmin] = useState<AdminUser | null>(null);
  const [checkedSession, setCheckedSession] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();

  // Restore session on refresh
  useEffect(() => {
    const stored = getStoredAdmin();
    if (stored && getToken()) {
      setAdmin(stored);
    }
    setCheckedSession(true);
  }, []);

  if (!checkedSession) {
    return null;
  }

  // Rendered over whatever URL the admin arrived at, so signing in drops them
  // straight onto the page they deep-linked to instead of the dashboard.
  if (!admin) {
    return <LoginPage onLogin={setAdmin} />;
  }

  // Child pages take an onNavigate(page) callback. Keep that contract and turn
  // it into a real URL change, so none of them need to know about the router.
  const goToPage = (page: string) => navigate(`/${page}`);

  function handleLogout() {
    logoutAdmin();
    setAdmin(null);
    navigate("/dashboard");
  }

  const today = new Date();
  const weekAgo = new Date(today);
  weekAgo.setDate(today.getDate() - 6);
  const dateRangeLabel = `${weekAgo.toLocaleDateString("en-US",{month:"short",day:"numeric",year:"numeric"})} - ${today.toLocaleDateString("en-US",{month:"short",day:"numeric",year:"numeric"})}`;

  return (
    <div className="flex h-screen overflow-hidden" style={{ background: "#f2f2f5", fontFamily: "Inter, sans-serif" }}>
      <Sidebar
        onLogout={handleLogout}
        adminName={admin.full_name}
        adminRoleLabel={capitalizeRole(admin.role) === "Admin" ? "Super Admin" : capitalizeRole(admin.role)}
        mobileOpen={sidebarOpen}
        onCloseMobile={() => setSidebarOpen(false)}
      />
      <div className="flex-1 min-w-0 flex flex-col h-screen lg:ml-[260px]">
        {/* Mobile top bar — hamburger opens the sidebar drawer; hidden on lg+ */}
        <header
          className="lg:hidden sticky top-0 z-30 flex items-center gap-3 px-4 py-3"
          style={{ background: "#1e222d" }}
        >
          <button
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
            className="w-10 h-10 rounded-[8px] flex items-center justify-center cursor-pointer"
            style={{ background: "rgba(255,255,255,0.08)", border: "none" }}
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M3 5h14M3 10h14M3 15h14" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
          <img src="/tiffin-logo.png" alt="TiffinCraft logo" className="w-7 h-7 object-contain" />
          <p className="text-white text-[16px] flex-1" style={{ fontFamily: "Inter", fontWeight: 700 }}>
            TiffinCraft
          </p>
          <NotificationBell dark onNavigate={goToPage} />
        </header>

        {/* Desktop top bar — greeting + date range on the left, bell on the right.
            The page content scrolls inside <main> below, so this bar never moves. */}
        <header
          className="hidden lg:flex sticky top-0 z-30 items-center justify-between px-8 py-3 shrink-0"
          style={{ background: "#f2f2f5" }}
        >
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 24, color: "#1c1f29" }}>
              Welcome back, Admin! 👋
            </p>
            <p style={{ fontFamily: "Inter", fontWeight: 400, fontSize: 14, color: "#9499a6", marginTop: 4 }}>
              Here's what's happening with TiffinCraft today.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center px-3 py-2 rounded-[6px]"
              style={{ border: "1px solid #e5e8ed", background: "white", fontFamily: "Inter", fontWeight: 400, fontSize: 13, color: "#9499a6" }}>
              📅 {dateRangeLabel}
            </div>
            <NotificationBell onNavigate={goToPage} />
          </div>
        </header>

        <main className="flex-1 min-h-0 overflow-y-auto p-4 sm:p-6 lg:p-8">
          {/* Route slugs match the Sidebar's Page ids, so /settlements etc. are
              bookmarkable and survive a refresh. Unknown paths fall back to the
              dashboard, same as the old switch statement's default case. */}
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage onNavigate={goToPage} />} />
            <Route path="/users" element={<ManageUsersPage />} />
            <Route path="/cooks" element={<CooksPage />} />
            <Route path="/meals" element={<MealsPage />} />
            <Route path="/orders" element={<ManageOrdersPage />} />
            <Route path="/reviews" element={<ReviewsPage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/refunds" element={<RefundsPage />} />
            <Route path="/earnings" element={<EarningsPage />} />
            <Route path="/settlements" element={<CommissionSettlementsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/settings" element={<SettingsPage onProfileUpdated={setAdmin} />} />
            <Route path="/support" element={<SupportPage />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}
