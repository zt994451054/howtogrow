import { STORAGE_KEYS } from "./config";
import { getStorage, setStorage } from "./storage";
import { apiRequest } from "./request";
import type { ChildCreateRequest, ChildCreateResponse, ChildView } from "./types";

export function getCachedChildren(): ChildView[] | null {
  return getStorage<ChildView[]>(STORAGE_KEYS.children);
}

export async function fetchChildren(): Promise<ChildView[]> {
  const response = await apiRequest<ChildView[]>("GET", "/miniprogram/children");
  setStorage(STORAGE_KEYS.children, response);
  return response;
}

export async function createChild(payload: ChildCreateRequest): Promise<number> {
  const response = await apiRequest<ChildCreateResponse>("POST", "/miniprogram/children", payload);
  return response.childId;
}

