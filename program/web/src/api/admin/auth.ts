import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export type AdminLoginRequest = {
  username: string;
  password: string;
};

export type AdminLoginResponse = {
  token: string;
  expiresIn: number;
};

export type AdminMeResponse = {
  adminUserId: number;
  username: string;
  permissionCodes: string[];
};

export async function adminLogin(request: AdminLoginRequest): Promise<AdminLoginResponse> {
  const res = await http.post<ApiResponse<AdminLoginResponse>>("/api/v1/admin/auth/login", request);
  return res.data.data;
}

export async function adminMe(): Promise<AdminMeResponse> {
  const res = await http.get<ApiResponse<AdminMeResponse>>("/api/v1/admin/auth/me");
  return res.data.data;
}

