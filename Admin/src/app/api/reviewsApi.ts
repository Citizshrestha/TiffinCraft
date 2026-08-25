import { apiGet, apiDelete } from "./client";

export interface AdminReview {
  id: number;
  customer: string;
  cook: string;
  rating: number;
  comment: string;
  reply: string | null;
  date: string;
}

export interface ReviewStats {
  avg_rating: number;
  total_reviews: number;
  five_star: number;
  pending_replies: number;
}

export interface ReviewPagination {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export interface AdminReviewsResponse {
  success: boolean;
  stats: ReviewStats;
  reviews: AdminReview[];
  pagination: ReviewPagination;
}

export interface FetchReviewsParams {
  page?: number;
  limit?: number;
  search?: string;
  rating?: string;
}

export async function fetchAdminReviews(
  params: FetchReviewsParams = {}
): Promise<AdminReviewsResponse> {
  const qs = new URLSearchParams();
  if (params.page)   qs.set("page",   String(params.page));
  if (params.limit)  qs.set("limit",  String(params.limit));
  if (params.search) qs.set("search", params.search);
  if (params.rating && params.rating !== "all") qs.set("rating", params.rating);
  const query = qs.toString() ? `?${qs}` : "";
  return apiGet<AdminReviewsResponse>(`/admin/reviews${query}`);
}

export async function deleteAdminReview(reviewId: number): Promise<void> {
  await apiDelete(`/admin/reviews/${reviewId}`);
}
