import axios from "axios";

export const AuthApi = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 인터셉터 추가
AuthApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    const tokenType = localStorage.getItem("tokenType");

    if (token) {
      config.headers["Authorization"] = `${tokenType} ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export const login = async ({ email, password }) => {
  try {
    const response = await AuthApi.post("/api/auth/login", { email, password });

    // 로그인 성공 시 토큰 저장
    localStorage.setItem("tokenType", response.data.tokenType);
    localStorage.setItem("accessToken", response.data.accessToken);
    localStorage.setItem("refreshToken", response.data.refreshToken);

    return response.data;
  } catch (error) {
    console.error("Login failed:", error);
    throw error;
  }
};

export const signUp = async (formData) => {
  try {
    formData.agreeTerms = formData.agreeTerms === "on";
    const response = await AuthApi.post("/api/auth/signup", formData);

    return response.data;
  } catch (error) {
    console.error("Signup failed:", error);
    throw error;
  }
};

// 로그아웃 함수 추가
export const logout = () => {
  localStorage.removeItem("tokenType");
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
};
