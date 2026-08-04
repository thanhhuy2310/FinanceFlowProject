import axios from "axios";

import type { ApiResponse } from "../types/api";

const TOKEN_KEY = "financeflow_token";
const USER_KEY = "financeflow_user";
const LOGIN_PATH = "/login";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "",
});

// Attach the stored token to every outgoing request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// On an expired or invalid session, clear stored credentials and return to login.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);

      if (!window.location.pathname.startsWith(LOGIN_PATH)) {
        window.location.assign(LOGIN_PATH);
      }
    }

    return Promise.reject(error);
  },
);

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  return (await request).data.data;
}

export function errorMessage(error: unknown, fallback = "Something went wrong. Please try again.") {
  if (axios.isAxiosError(error) && typeof error.response?.data === "string") {
    return error.response.data;
  }

  if (axios.isAxiosError(error) && typeof error.response?.data?.message === "string") {
    return error.response.data.message;
  }

  return fallback;
}
