import React, { useState } from "react";

type Page =
  | "dashboard"
  | "users"
  | "cooks"
  | "meals"
  | "orders"
  | "reviews"
  | "payments"
  | "earnings"
  | "reports"
  | "settings"
  | "support";

interface SidebarProps {
  activePage: Page;
  onNavigate: (page: Page) => void;
  onLogout: () => void;
}

const navItems: { id: Page; icon: string; label: string }[] = [
  { id: "dashboard", icon: "🏠", label: "Dashboard" },
  { id: "users", icon: "👥", label: "Users" },
  { id: "cooks", icon: "👨‍🍳", label: "Cooks" },
  { id: "meals", icon: "🍱", label: "Meals" },
  { id: "orders", icon: "📦", label: "Orders" },
  { id: "reviews", icon: "⭐", label: "Reviews" },
  { id: "payments", icon: "💳", label: "Payments" },
  { id: "earnings", icon: "💰", label: "Earnings" },
  { id: "reports", icon: "📊", label: "Reports" },
  { id: "settings", icon: "⚙️", label: "Settings" },
  { id: "support", icon: "❓", label: "Support" },
];

export function Sidebar({ activePage, onNavigate, onLogout }: SidebarProps) {
  const [showLogoutMenu, setShowLogoutMenu] = useState(false);

  return (
    <>
      <div
        className="fixed top-0 left-0 h-full w-[260px] flex flex-col overflow-hidden z-50"
        style={{
          background: "#1e222d",
          border: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        {/* Logo Section */}
        <div className="px-6 pt-7 pb-4">
          <div className="w-5 h-5 rounded bg-[#58c66c] mb-[10px]" />
          <p
            className="text-white text-2xl tracking-[-0.4px] mb-1"
            style={{ fontFamily: "Inter", fontWeight: 700 }}
          >
            TiffinCraft
          </p>
          <p
            className="text-[12px]"
            style={{
              fontFamily: "Inter",
              fontWeight: 500,
              color: "rgba(255,255,255,0.55)",
            }}
          >
            Admin Panel
          </p>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-4 pt-4 flex flex-col gap-2 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = activePage === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className="flex items-center gap-3 h-11 px-[14px] rounded-[10px] w-full text-left transition-all duration-150 cursor-pointer"
                style={
                  isActive
                    ? {
                        background: "#58c66c",
                        boxShadow: "0px 8px 24px 0px rgba(88,198,108,0.25)",
                      }
                    : {}
                }
                onMouseEnter={(e) => {
                  if (!isActive)
                    (e.currentTarget as HTMLElement).style.background =
                      "rgba(255,255,255,0.06)";
                }}
                onMouseLeave={(e) => {
                  if (!isActive)
                    (e.currentTarget as HTMLElement).style.background =
                      "transparent";
                }}
              >
                <span className="text-[18px] leading-none">{item.icon}</span>
                <span
                  className="text-[14px]"
                  style={{
                    fontFamily: "Inter",
                    fontWeight: 500,
                    color: isActive ? "#ffffff" : "#d1d5db",
                  }}
                >
                  {item.label}
                </span>
              </button>
            );
          })}
        </nav>

        {/* Profile Section — click to open logout menu */}
        <div
          className="px-4 py-4 relative"
          style={{ borderTop: "1px solid rgba(255,255,255,0.08)" }}
        >
          {/* Logout popup — rendered above profile card */}
          {showLogoutMenu && (
            <div
              className="absolute bottom-[84px] left-4 right-4 rounded-[10px] overflow-hidden"
              style={{
                background: "#252a36",
                border: "1px solid rgba(255,255,255,0.1)",
                boxShadow: "0px 8px 24px rgba(0,0,0,0.4)",
              }}
            >
              <button
                onClick={onLogout}
                className="w-full flex items-center gap-3 px-4 py-3 text-left transition-colors duration-150"
                style={{
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  fontFamily: "Inter",
                  fontWeight: 500,
                  fontSize: 14,
                  color: "#f25959",
                }}
                onMouseEnter={(e) =>
                  ((e.currentTarget as HTMLElement).style.background =
                    "rgba(242,89,89,0.08)")
                }
                onMouseLeave={(e) =>
                  ((e.currentTarget as HTMLElement).style.background = "none")
                }
              >
                <span className="text-[16px]">🚪</span>
                Logout
              </button>
            </div>
          )}

          <button
            onClick={() => setShowLogoutMenu((v) => !v)}
            className="w-full flex items-center gap-3 p-3 rounded-[12px] cursor-pointer transition-all duration-150 text-left"
            style={{
              background: showLogoutMenu
                ? "rgba(255,255,255,0.07)"
                : "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.05)",
            }}
            onMouseEnter={(e) =>
              ((e.currentTarget as HTMLElement).style.background =
                "rgba(255,255,255,0.07)")
            }
            onMouseLeave={(e) => {
              if (!showLogoutMenu)
                (e.currentTarget as HTMLElement).style.background =
                  "rgba(255,255,255,0.03)";
            }}
          >
            {/* Avatar */}
            <div className="relative shrink-0 w-9 h-9">
              <svg className="w-full h-full" viewBox="0 0 36 36" fill="none">
                <circle cx="18" cy="18" r="18" fill="#58C66C" />
              </svg>
              <span
                className="absolute inset-0 flex items-center justify-center text-white text-[13px]"
                style={{ fontFamily: "Inter", fontWeight: 600 }}
              >
                AU
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <p
                className="text-white text-[13px]"
                style={{ fontFamily: "Inter", fontWeight: 600 }}
              >
                Admin User
              </p>
              <p
                className="text-[11px]"
                style={{
                  fontFamily: "Inter",
                  fontWeight: 500,
                  color: "rgba(255,255,255,0.55)",
                }}
              >
                Super Admin
              </p>
            </div>
            <span
              style={{
                color: "rgba(255,255,255,0.4)",
                fontSize: 10,
                transform: showLogoutMenu ? "rotate(0deg)" : "rotate(180deg)",
                display: "inline-block",
                transition: "transform 150ms",
              }}
            >
              ▲
            </span>
          </button>
        </div>
      </div>

      {/* Backdrop to close menu on outside click */}
      {showLogoutMenu && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => setShowLogoutMenu(false)}
        />
      )}
    </>
  );
}

export type { Page };
