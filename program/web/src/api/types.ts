export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
  traceId: string;
};

export type PageResponse<T> = {
  page: number;
  pageSize: number;
  total: number;
  items: T[];
};

