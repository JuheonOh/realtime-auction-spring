import axios from "axios";

const TOKEN_TYPE = localStorage.getItem("tokenType");
let ACCESS_TOKEN = localStorage.getItem("accessToken");
let REFRESH_TOKEN = localStorage.getItem("refreshToken");

// CREATE CUSTOM AXIOS INSTANCE
export const UserApi = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
    Authorization: `${TOKEN_TYPE} ${ACCESS_TOKEN}`,
    REFRESH_TOKEN: REFRESH_TOKEN,
  },
});

// 토큰 갱신
const refreshAccessToken = async () => {
  try {
    const response = await UserApi.get("/api/auth/refresh");
    ACCESS_TOKEN = response.data;
    localStorage.setItem("accessToken", ACCESS_TOKEN);
    UserApi.defaults.headers.Authorization = `${TOKEN_TYPE} ${ACCESS_TOKEN}`;
  } catch (err) {
    console.error("Failed to refresh token:", err);
  }
};

// 토큰 유효성 검사
UserApi.interceptors.response.use(
  (res) => {
    return res;
  },
  async (err) => {
    const originalRequest = err.config;

    if (err.response.status === 500 && !originalRequest._retry) {
      originalRequest._retry = true;
      await refreshAccessToken();
      return UserApi(originalRequest);
    }

    return Promise.reject(err);
  }
);

// 회원조회 API
export const fetchUser = async () => {
  const response = await UserApi.get("/api/user");
  return response.data;
};

// 회원정보 수정 API
export const updateUser = async (formData) => {
  const response = await UserApi.put("/api/user", formData);
  return response.data;
};

// 회원탈퇴 API
export const deleteUser = async () => {
  const response = await UserApi.delete("/api/user");
  return response.data;
};
