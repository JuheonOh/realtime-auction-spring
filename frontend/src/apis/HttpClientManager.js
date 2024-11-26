import { clearCookie, getCookie, setCookie } from "@data/storage/Cookie";
import { API_BASE_URL } from "@utils/constant";
import axios from "axios";

class HttpClientManager {
  constructor() {
    this.TOKEN_TYPE = "Bearer";
    this.REFRESH_URL = `${API_BASE_URL}/api/auth/refresh`;
  }

  createApiInstance() {
    const apiInstance = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        "Content-Type": "application/json",
      },
    });

    this.setupInterceptors(apiInstance);

    return apiInstance;
  }

  setupInterceptors(axiosInstance) {
    this.setupRequestInterceptor(axiosInstance);
    this.setupResponseInterceptor(axiosInstance);
  }

  setupRequestInterceptor(axiosInstance) {
    axiosInstance.interceptors.request.use(
      (config) => {
        const accessToken = getCookie("accessToken");
        if (accessToken) {
          config.headers["Authorization"] = this.getAuthHeader(accessToken);
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
  }

  setupResponseInterceptor(axiosInstance) {
    axiosInstance.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const newAccessToken = await this.refreshAccessToken();
            originalRequest.headers["Authorization"] = `${this.TOKEN_TYPE} ${newAccessToken}`;
            return axiosInstance(originalRequest);
          } catch (refreshError) {
            return Promise.reject(refreshError);
          }
        }
        return Promise.reject(error);
      }
    );
  }

  async refreshAccessToken() {
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
      throw error;
    }
  }

  getAuthHeader(token) {
    return `${this.TOKEN_TYPE} ${token}`;
  }
}

const httpClientManager = new HttpClientManager();
export default httpClientManager;
