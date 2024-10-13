import axios from "axios";

const BASE_URL = "http://localhost:8080";
const TOKEN_TYPE = "Bearer";

// API 인스턴스 생성
export const UserApi = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 토큰 관리 함수
const getAccessToken = () => localStorage.getItem("accessToken");
const getRefreshToken = () => localStorage.getItem("refreshToken");
const setAccessToken = (token) => localStorage.setItem("accessToken", token);

// 요청 인터셉터: 매 요청마다 최신 토큰 사용
UserApi.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers["Authorization"] = `${TOKEN_TYPE} ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 토큰 갱신
const refreshAccessToken = async () => {
  try {
    const response = await axios.get(`${BASE_URL}/api/auth/refresh`, {
      headers: {
        REFRESH_TOKEN: getRefreshToken(),
      },
    });

    const newAccessToken = response.data.accessToken;
    setAccessToken(newAccessToken);
    return newAccessToken;
  } catch (error) {
    if(error.response.status === 403) {
      localStorage.clear();
      window.location.replace("/auth/login");
    }
  }
};

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

// API 함수들
export const fetchUser = () => UserApi.get(`/api/user`);
export const updateUser = (data) => UserApi.put(`/api/user`, data);
export const deleteUser = () => UserApi.delete(`/api/user`);
