import axios from "axios";
import { getCookie } from "../data/storage/Cookie";

const BASE_URL = "http://localhost:8080";

export const AuthApi = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 인터셉터 추가
AuthApi.interceptors.request.use(
  (config) => {
    const tokenType = getCookie("tokenType");
    const accessToken = getCookie("accessToken");

    if (accessToken) {
      config.headers["Authorization"] = `${tokenType} ${accessToken}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export const login = async ({ email, password }) => {
  try {
    const response = await AuthApi.post("/api/auth/login", { email, password });
    return response;
  } catch (error) {
    throw error;
  }
};

export const signUp = async (formData) => {
  try {
    const response = await AuthApi.post("/api/auth/signup", formData);
    return response.data;
  } catch (error) {
    console.error("Signup failed:", error);
    throw error;
  }
};

export const logout = async () => {
  try {
    const response = await AuthApi.get("/api/auth/logout", {
      headers: {
        REFRESH_TOKEN: getCookie("refreshToken"),
      },
    });

    return response;
  } catch (error) {
    throw error;
  }
};
