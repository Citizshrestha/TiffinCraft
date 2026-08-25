import React, { useState, useEffect, useCallback, useRef } from "react";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow } from "./Modal";
import {
  fetchAdminReviews,
  deleteAdminReview,
  AdminReview,
  ReviewStats,
} from "../api/reviewsApi";

const PER_PAGE = 10;

const ini = (n: string) =>
  n
    .split(" ")
    .map((x) => x[0])
    .join("")
    .toUpperCase();

function Stars({ n }: { n: number }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <svg
          key={i}
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill={i <= n ? "#f59e0b" : "#e5e7eb"}
          xmlns="http://www.w3.org/2000/svg"
        >
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
      ))}
      <span
        style={{
          fontFamily: "Inter",
          fontWeight: 600,
          fontSize: 13,
          color: "#1c1f29",
          marginLeft: 2,
        }}
      >
        {n}
      </span>
    </div>
  );
}

function StatCard({
  icon,
  value,
  label,
  sub,
  accent,
}: {
  icon: React.ReactNode;
  value: string;
  label: string;
  sub: string;
  accent?: string;
}) {
  return (
    <div
      className="bg-white flex flex-col gap-3 p-5 rounded-[14px] flex-1"
      style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
    >
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: 10,
          background: accent || "#fff7ed",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 18,
        }}
      >
        {icon}
      </div>
      <p
        style={{
          fontFamily: "Inter",
          fontWeight: 700,
          fontSize: 30,
          color: "#1c1f29",
          lineHeight: 1,
        }}
      >
        {value}
      </p>
      <div>
        <p
          style={{
            fontFamily: "Inter",
            fontWeight: 600,
            fontSize: 14,
            color: "#1c1f29",
          }}
        >
          {label}
        </p>
        <p
          style={{ fontFamily: "Inter", fontSize: 12, color: "#9499a6" }}
        >
          {sub}
        </p>
      </div>
    </div>
  );
}

function SkeletonRow() {
  return (
    <div className="flex gap-4 items-center py-4">
      {[40, 150, 140, 120, 240, 70, 110, 110].map((w, i) => (
        <div
          key={i}
          style={{
            width: w,
            flexShrink: 0,
            height: 12,
            borderRadius: 6,
            background: "#f0f2f5",
          }}
        />
      ))}
    </div>
  );
}

export function ReviewsPage() {
  const [stats, setStats] = useState<ReviewStats | null>(null);
  const [rows, setRows] = useState<AdminReview[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [ratingFilter, setRatingFilter] = useState("all");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [viewing, setViewing] = useState<AdminReview | null>(null);
  const [del, setDel] = useState<AdminReview | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Debounce search
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [debouncedSearch, setDebouncedSearch] = useState("");

  const handleSearchChange = (val: string) => {
    setSearch(val);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(val);
      setPage(1);
    }, 400);
  };

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const res = await fetchAdminReviews({
        page,
        limit: PER_PAGE,
        search: debouncedSearch || undefined,
        rating: ratingFilter !== "all" ? ratingFilter : undefined,
      });
      setStats(res.stats);
      setRows(res.reviews);
      setTotalPages(res.pagination.totalPages);
      setTotalItems(res.pagination.total);
    } catch (err) {
      setLoadError(
        err instanceof Error ? err.message : "Failed to load reviews."
      );
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, ratingFilter]);

  useEffect(() => {
    load();
  }, [load]);

  // Reset page on filter change
  useEffect(() => {
    setPage(1);
  }, [ratingFilter]);

  const doDelete = async () => {
    if (!del) return;
    setDeleting(true);
    try {
      await deleteAdminReview(del.id);
      setDel(null);
      load();
    } catch {
      setDel(null);
    } finally {
      setDeleting(false);
    }
  };

  const fmtDate = (iso: string) => {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div>
        <p
          style={{
            fontFamily: "Inter",
            fontWeight: 700,
            fontSize: 28,
            color: "#1c1f29",
          }}
        >
          Reviews &amp; Ratings
        </p>
        <p
          style={{
            fontFamily: "Inter",
            fontWeight: 400,
            fontSize: 14,
            color: "#9499a6",
            marginTop: 4,
          }}
        >
          Customer feedback and ratings overview.
        </p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          icon="⭐"
          value={stats ? stats.avg_rating.toFixed(1) : "—"}
          label="Average Rating"
          sub="out of 5.0"
          accent="#fff7ed"
        />
        <StatCard
          icon="📝"
          value={stats ? String(stats.total_reviews) : "—"}
          label="Total Reviews"
          sub="in database"
          accent="#eff6ff"
        />
        <StatCard
          icon="🏆"
          value={stats ? String(stats.five_star) : "—"}
          label="5 Star Reviews"
          sub={
            stats && stats.total_reviews > 0
              ? `${Math.round((stats.five_star / stats.total_reviews) * 100)}% of total`
              : "—"
          }
          accent="#f0fdf4"
        />
        <StatCard
          icon="⏳"
          value={stats ? String(stats.pending_replies) : "—"}
          label="Pending Replies"
          sub="Need response"
          accent="#fef3c7"
        />
      </div>

      {/* Filters */}
      <div className="flex gap-3 items-center flex-wrap">
        <div
          className="flex items-center gap-2 px-4 py-3 rounded-[10px] bg-white flex-1"
          style={{ border: "1px solid #e5e8ed", minWidth: 200 }}
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#9499a6"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="M21 21l-4.35-4.35" />
          </svg>
          <input
            className="flex-1 outline-none bg-transparent"
            style={{
              border: "none",
              fontFamily: "Inter",
              fontSize: 14,
              color: "#1c1f29",
            }}
            placeholder="Search customer or cook..."
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
          />
        </div>
        <div className="flex gap-2 flex-wrap">
          {["all", "5", "4", "3", "2", "1"].map((r) => (
            <button
              key={r}
              onClick={() => setRatingFilter(r)}
              style={{
                padding: "10px 14px",
                borderRadius: 8,
                fontSize: 13,
                cursor: "pointer",
                border: "none",
                fontFamily: "Inter",
                fontWeight: 500,
                background:
                  ratingFilter === r
                    ? "#3b82f6"
                    : ratingFilter === r
                    ? "#eff6ff"
                    : "#f2f5f7",
                color: ratingFilter === r ? "#fff" : "#9499a6",
                transition: "all 0.15s",
              }}
            >
              {r === "all"
                ? "All Stars"
                : `${"★".repeat(parseInt(r))} ${r}`}
            </button>
          ))}
        </div>
      </div>

      {/* Error banner */}
      {loadError && (
        <div
          className="flex items-center gap-3 px-4 py-3 rounded-[10px]"
          style={{ background: "#fef2f2", border: "1px solid #fecaca" }}
        >
          <span style={{ fontSize: 16 }}>⚠️</span>
          <p
            style={{
              fontFamily: "Inter",
              fontSize: 14,
              color: "#dc2626",
              flex: 1,
            }}
          >
            {loadError}
          </p>
          <button
            onClick={load}
            style={{
              background: "#dc2626",
              color: "white",
              border: "none",
              borderRadius: 6,
              padding: "6px 14px",
              fontFamily: "Inter",
              fontWeight: 600,
              fontSize: 13,
              cursor: "pointer",
            }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Table */}
      <div
        className="bg-white rounded-[14px]"
        style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
      >
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1100px]">
            {/* Header row */}
            <div
              className="flex gap-4 pb-4"
              style={{ borderBottom: "1px solid #f0f2f5" }}
            >
              <p
                style={{
                  width: 40,
                  flexShrink: 0,
                  fontFamily: "Inter",
                  fontWeight: 600,
                  fontSize: 12,
                  color: "#b2b8bf",
                  textTransform: "uppercase",
                  letterSpacing: "0.05em",
                }}
              >
                #
              </p>
              {[
                "Customer",
                "Cook / Kitchen",
                "Rating",
                "Comment",
                "Replied",
                "Date",
                "Actions",
              ].map((h, i) => (
                <p
                  key={h}
                  style={{
                    width: [150, 140, 120, 240, 70, 110, 110][i],
                    flexShrink: 0,
                    fontFamily: "Inter",
                    fontWeight: 600,
                    fontSize: 12,
                    color: "#b2b8bf",
                    textTransform: "uppercase",
                    letterSpacing: "0.05em",
                  }}
                >
                  {h}
                </p>
              ))}
            </div>

            {/* Loading skeletons */}
            {loading &&
              Array.from({ length: 5 }).map((_, i) => (
                <div key={i}>
                  <SkeletonRow />
                  {i < 4 && (
                    <div style={{ height: 1, background: "#f5f7fa" }} />
                  )}
                </div>
              ))}

            {/* Empty state */}
            {!loading && rows.length === 0 && (
              <div
                style={{
                  padding: "48px 0",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: 8,
                }}
              >
                <span style={{ fontSize: 40 }}>🔍</span>
                <p
                  style={{
                    fontFamily: "Inter",
                    fontSize: 14,
                    color: "#9499a6",
                  }}
                >
                  {debouncedSearch || ratingFilter !== "all"
                    ? "No reviews match your filters."
                    : "No reviews yet."}
                </p>
              </div>
            )}

            {/* Data rows */}
            {!loading &&
              rows.map((r, idx) => (
                <div key={r.id}>
                  <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                    <p
                      style={{
                        width: 40,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontWeight: 500,
                        fontSize: 13,
                        color: "#9499a6",
                      }}
                    >
                      {(page - 1) * PER_PAGE + idx + 1}
                    </p>

                    {/* Customer with avatar */}
                    <div
                      style={{
                        width: 150,
                        flexShrink: 0,
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                      }}
                    >
                      <div
                        style={{
                          width: 28,
                          height: 28,
                          borderRadius: "50%",
                          background: "#e0e7ff",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                        }}
                      >
                        <span
                          style={{
                            fontFamily: "Inter",
                            fontWeight: 700,
                            fontSize: 10,
                            color: "#4f46e5",
                          }}
                        >
                          {ini(r.customer)}
                        </span>
                      </div>
                      <p
                        style={{
                          fontFamily: "Inter",
                          fontWeight: 500,
                          fontSize: 13,
                          color: "#1c1f29",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {r.customer}
                      </p>
                    </div>

                    <p
                      style={{
                        width: 140,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#1c1f29",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {r.cook}
                    </p>

                    <div style={{ width: 120, flexShrink: 0 }}>
                      <Stars n={r.rating} />
                    </div>

                    <p
                      style={{
                        width: 240,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#6b7280",
                        fontStyle: "italic",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {r.comment || "—"}
                    </p>

                    <div style={{ width: 70, flexShrink: 0 }}>
                      <span
                        style={{
                          padding: "3px 10px",
                          borderRadius: 20,
                          fontSize: 11,
                          fontFamily: "Inter",
                          fontWeight: 600,
                          background: r.reply
                            ? "rgba(16,185,129,0.1)"
                            : "rgba(245,158,11,0.1)",
                          color: r.reply ? "#059669" : "#d97706",
                        }}
                      >
                        {r.reply ? "Yes" : "No"}
                      </span>
                    </div>

                    <p
                      style={{
                        width: 110,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#9499a6",
                      }}
                    >
                      {fmtDate(r.date)}
                    </p>

                    <div style={{ width: 110, flexShrink: 0 }}>
                      <ActionButtons
                        onView={() => setViewing(r)}
                        onDelete={() => setDel(r)}
                      />
                    </div>
                  </div>
                  {idx < rows.length - 1 && (
                    <div style={{ height: 1, background: "#f5f7fa" }} />
                  )}
                </div>
              ))}
          </div>
        </div>
      </div>

      <Pagination
        current={page}
        total={totalPages}
        totalItems={totalItems}
        onPageChange={setPage}
      />

      {/* View Modal */}
      {viewing && (
        <Modal title="Review Details" onClose={() => setViewing(null)}>
          <div className="flex items-center gap-3 mb-5">
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: "50%",
                background: "#e0e7ff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <span
                style={{
                  fontFamily: "Inter",
                  fontWeight: 700,
                  fontSize: 14,
                  color: "#4f46e5",
                }}
              >
                {ini(viewing.customer)}
              </span>
            </div>
            <div>
              <p
                style={{
                  fontFamily: "Inter",
                  fontWeight: 700,
                  fontSize: 16,
                  color: "#1c1f29",
                }}
              >
                {viewing.customer}
              </p>
              <Stars n={viewing.rating} />
            </div>
          </div>
          <DetailRow label="Cook / Kitchen" value={viewing.cook} />
          <DetailRow
            label="Comment"
            value={
              <span style={{ fontStyle: "italic" }}>
                {viewing.comment || "No comment provided."}
              </span>
            }
          />
          <DetailRow label="Date" value={fmtDate(viewing.date)} />
          <DetailRow
            label="Replied"
            value={
              viewing.reply ? (
                <span style={{ color: "#059669" }}>{viewing.reply}</span>
              ) : (
                <span style={{ color: "#9499a6" }}>No reply yet</span>
              )
            }
          />
        </Modal>
      )}

      {/* Delete Confirm */}
      {del && (
        <ConfirmDelete
          name={`review by ${del.customer}`}
          onConfirm={doDelete}
          onCancel={() => setDel(null)}
        />
      )}
    </div>
  );
}
