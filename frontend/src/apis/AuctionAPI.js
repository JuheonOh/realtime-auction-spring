import axios from "axios";
import { clearCookie, getCookie, setCookie } from "../storage/Cookie";

const BASE_URL = "http://localhost:8080";
const TOKEN_TYPE = "Bearer";

export const AuctionApi = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 인터셉터: 매 요청마다 최신 토큰 사용
AuctionApi.interceptors.request.use(
  (config) => {
    const accessToken = getCookie("accessToken");
    if (accessToken) {
      config.headers["Authorization"] = `${TOKEN_TYPE} ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 토큰 만료 시 갱신 및 재요청
AuctionApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const newAccessToken = await refreshAccessToken();
        originalRequest.headers["Authorization"] = `${TOKEN_TYPE} ${newAccessToken}`;
        return AuctionApi(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

// 토큰 갱신
const refreshAccessToken = async () => {
  try {
    const refreshToken = getCookie("refreshToken");
    const response = await axios.get(`${BASE_URL}/api/auth/refresh`, {
      headers: {
        REFRESH_TOKEN: refreshToken,
      },
    });

    const newAccessToken = response.data.accessToken;
    setCookie("accessToken", newAccessToken);

    return newAccessToken;
  } catch (error) {
    if (error.response.status === 403) {
      clearCookie();
      window.location.replace("/auth/login");
    }
  }
};

export const addAuction = async (formData) => {
  try {
    const response = await AuctionApi.post("/api/auctions", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response;
  } catch (error) {
    throw error;
  }
};
