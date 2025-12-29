import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type OrderView = {
  orderId: number;
  orderNo: string;
  userId: number;
  planId: number;
  planName: string;
  amountCent: number;
  status: string;
  payTradeNo: string | null;
  prepayId: string | null;
  createdAt: string;
  paidAt: string | null;
};

export async function listOrders(params: { page: number; pageSize: number }): Promise<PageResponse<OrderView>> {
  const res = await http.get<ApiResponse<PageResponse<OrderView>>>("/api/v1/admin/orders", { params });
  return res.data.data;
}

