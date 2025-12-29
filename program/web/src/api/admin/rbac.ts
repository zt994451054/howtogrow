import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export type PermissionView = {
  permissionId: number;
  code: string;
  name: string;
};

export async function listPermissions(): Promise<PermissionView[]> {
  const res = await http.get<ApiResponse<PermissionView[]>>("/api/v1/admin/rbac/permissions");
  return res.data.data;
}

export type RoleView = {
  roleId: number;
  code: string;
  name: string;
  permissionCodes: string[];
};

export type RoleCreateRequest = {
  code: string;
  name: string;
};

export type RolePermissionUpdateRequest = {
  permissionCodes: string[];
};

export async function listRoles(): Promise<RoleView[]> {
  const res = await http.get<ApiResponse<RoleView[]>>("/api/v1/admin/rbac/roles");
  return res.data.data;
}

export async function createRole(request: RoleCreateRequest): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/rbac/roles", request);
}

export async function updateRolePermissions(roleId: number, request: RolePermissionUpdateRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/rbac/roles/${roleId}/permissions`, request);
}

export type AdminUserView = {
  adminUserId: number;
  username: string;
  status: number;
  createdAt: string;
  roleCodes: string[];
};

export type AdminUserCreateRequest = {
  username: string;
  password: string;
  roleCodes: string[];
};

export type AdminUserRoleUpdateRequest = {
  roleCodes: string[];
};

export async function listAdminUsers(): Promise<AdminUserView[]> {
  const res = await http.get<ApiResponse<AdminUserView[]>>("/api/v1/admin/rbac/admin-users");
  return res.data.data;
}

export async function createAdminUser(request: AdminUserCreateRequest): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/rbac/admin-users", request);
}

export async function updateAdminUserRoles(adminUserId: number, request: AdminUserRoleUpdateRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/rbac/admin-users/${adminUserId}/roles`, request);
}
