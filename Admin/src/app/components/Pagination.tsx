import React from "react";

interface PaginationProps {
  current: number;
  total: number;
  perPage?: number;
  totalItems: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  current,
  total,
  perPage = 10,
  totalItems,
  onPageChange,
}: PaginationProps) {
  const start = (current - 1) * perPage + 1;
  const end = Math.min(current * perPage, totalItems);

  const pages: (number | "...")[] = [];
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    pages.push(1, 2, 3);
    if (current > 4) pages.push("...");
    if (current > 3 && current < total - 1) pages.push(current);
    if (total > 3) pages.push("...", total);
  }

  const btnBase =
    "w-9 h-9 rounded-[6px] flex items-center justify-center text-[13px] cursor-pointer transition-all duration-150";

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between w-full">
      <p
        className="text-[13px]"
        style={{
          fontFamily: "Inter",
          fontWeight: 400,
          color: "#9499a6",
        }}
      >
        Showing {start} to {end} of {totalItems.toLocaleString()} results
      </p>
      <div className="flex items-center gap-2">
        <button
          onClick={() => onPageChange(Math.max(1, current - 1))}
          className={btnBase}
          style={{
            background: "#f2f5f7",
            color: "#b2b8bf",
            fontFamily: "Inter",
          }}
        >
          ←
        </button>
        {[1, 2, 3].map((p) => (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            className={btnBase}
            style={
              current === p
                ? {
                    background: "#57b869",
                    color: "#ffffff",
                    fontFamily: "Inter",
                    fontWeight: 500,
                  }
                : {
                    background: "#f2f5f7",
                    color: "#9499a6",
                    fontFamily: "Inter",
                    fontWeight: 500,
                  }
            }
          >
            {p}
          </button>
        ))}
        {total > 4 && (
          <>
            <button
              className={btnBase}
              style={{
                background: "#f2f5f7",
                color: "#9499a6",
                fontFamily: "Inter",
                fontWeight: 500,
              }}
            >
              ...
            </button>
            <button
              onClick={() => onPageChange(total)}
              className={btnBase}
              style={
                current === total
                  ? {
                      background: "#57b869",
                      color: "#ffffff",
                      fontFamily: "Inter",
                      fontWeight: 500,
                    }
                  : {
                      background: "#f2f5f7",
                      color: "#9499a6",
                      fontFamily: "Inter",
                      fontWeight: 500,
                    }
              }
            >
              {total}
            </button>
          </>
        )}
        <button
          onClick={() => onPageChange(Math.min(total, current + 1))}
          className={btnBase}
          style={{
            background: "#f2f5f7",
            color: "#9499a6",
            fontFamily: "Inter",
          }}
        >
          →
        </button>
      </div>
    </div>
  );
}
