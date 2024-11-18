import { clearCookie, getCookie, setCookie } from "@data/storage/Cookie";
import { API_BASE_URL } from "@utils/constant";
import axios from "axios";

const TOKEN_TYPE = "Bearer";

// API 인스턴스 생성
export const UserApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 인터셉터: 매 요청마다 최신 토큰 사용
UserApi.interceptors.request.use(
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
UserApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const newAccessToken = await refreshAccessToken();
        originalRequest.headers["Authorization"] = `${TOKEN_TYPE} ${newAccessToken}`;
        return UserApi(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

// 토큰 갱신
export const refreshAccessToken = async () => {
  try {
    const refreshToken = getCookie("refreshToken");
    const response = await axios.get(`${API_BASE_URL}/api/auth/refresh`, {
      headers: {
        REFRESH_TOKEN: refreshToken,
      },
    });

    const newAccessToken = response.data.accessToken;
    setCookie("accessToken", newAccessToken);

    return newAccessToken;
  } catch (error) {
    if (error.response.status === 401) {
      clearCookie();
      window.location.replace("/auth/login");
    }
  }
};

export const getUser = async () => {
  try {
    const response = await UserApi.get(`/api/users`);
    return response;
  } catch (error) {
    throw error;
  }
};
