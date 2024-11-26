import { getCookie } from "@data/storage/Cookie";
import HttpClientManager from "./HttpClientManager";

export const AuthApi = HttpClientManager.createApiInstance();

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
