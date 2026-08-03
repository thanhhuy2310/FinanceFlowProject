import axios from "axios";
import type { ApiResponse } from "../types/api";

const tokenKey = "financeflow_token";
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? "" });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(tokenKey);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(tokenKey);
      localStorage.removeItem("financeflow_user");
      if (!window.location.pathname.startsWith("/login")) window.location.assign("/login");
    }
    return Promise.reject(error);
  },
);

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>) {
  return (await request).data.data;
}

export function errorMessage(error: unknown, fallback = "Something went wrong. Please try again.") {
  if (axios.isAxiosError(error) && typeof error.response?.data === "string") return error.response.data;
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === "string") return error.response.data.message;
  return fallback;
}
