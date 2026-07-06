import React, { useState } from "react";
import { Sidebar, Page } from "./components/Sidebar";
import { DashboardPage } from "./components/DashboardPage";
import { ManageUsersPage } from "./components/ManageUsersPage";
import { CooksPage } from "./components/CooksPage";
import { MealsPage } from "./components/MealsPage";
import { ManageOrdersPage } from "./components/ManageOrdersPage";
import { ReviewsPage } from "./components/ReviewsPage";
import { PaymentsPage } from "./components/PaymentsPage";
import { EarningsPage } from "./components/EarningsPage";
import { ReportsPage } from "./components/ReportsPage";
import { SettingsPage } from "./components/SettingsPage";
import { SupportPage } from "./components/SupportPage";

function LoginPage({ onLogin }: { onLogin: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (email === "admin@tiffincraft.com" && password === "admin123") {
      onLogin();
    } else {
      setError("Invalid email or password.");
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ background: "#f2f2f5", fontFamily: "Inter, sans-serif" }}
    >
      <div
        className="bg-white rounded-[16px] p-10 w-full max-w-[420px]"
        style={{ boxShadow: "0px 4px 24px rgba(0,0,0,0.08)" }}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8">
          <div
            className="w-8 h-8 rounded-[6px] bg-[#58c66c] flex items-center justify-center"
          >
            <span className="text-white text-[16px]">🍱</span>
          </div>
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
            className="w-full py-3 rounded-[8px] text-white text-[14px] mt-2 cursor-pointer transition-all duration-150 hover:brightness-95"
            style={{ background: "#57b869", fontFamily: "Inter", fontWeight: 600, border: "none" }}
          >
            Sign In
          </button>
        </form>

        <p className="text-center mt-6" style={{ fontFamily: "Inter", fontSize: 12, color: "#b2b8bf" }}>
          Use admin@tiffincraft.com / admin123
        </p>
      </div>
    </div>
  );
}

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [activePage, setActivePage] = useState<Page>("dashboard");

  if (!isLoggedIn) {
    return <LoginPage onLogin={() => setIsLoggedIn(true)} />;
  }

  function renderPage() {
    switch (activePage) {
      case "dashboard":
        return <DashboardPage onNavigate={(p) => setActivePage(p as Page)} />;
      case "users":
        return <ManageUsersPage />;
      case "cooks":
        return <CooksPage />;
      case "meals":
        return <MealsPage />;
      case "orders":
        return <ManageOrdersPage />;
      case "reviews":
        return <ReviewsPage />;
      case "payments":
        return <PaymentsPage />;
      case "earnings":
        return <EarningsPage />;
      case "reports":
        return <ReportsPage />;
      case "settings":
        return <SettingsPage />;
      case "support":
        return <SupportPage />;
      default:
        return <DashboardPage onNavigate={(p) => setActivePage(p as Page)} />;
    }
  }

  function handleLogout() {
    setIsLoggedIn(false);
    setActivePage("dashboard");
  }

  return (
    <div className="flex min-h-screen" style={{ background: "#f2f2f5", fontFamily: "Inter, sans-serif" }}>
      <Sidebar activePage={activePage} onNavigate={setActivePage} onLogout={handleLogout} />
      <main
        className="flex-1 min-h-screen overflow-y-auto"
        style={{ marginLeft: 260, padding: "32px" }}
      >
        {renderPage()}
      </main>
    </div>
  );
}
