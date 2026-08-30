import store from "@data/redux/store";
import { LOGOUT, SET_ACCESS_TOKEN } from "@data/redux/store/User";
import { API_BASE_URL } from "@utils/constant";
import axios from "axios";

class HttpClientManager {
  constructor() {
    this.TOKEN_TYPE = "Bearer";
    this.REFRESH_URL = `${API_BASE_URL}/api/auth/refresh`;
    this.refreshPromise = null;
    this.csrfPromise = null;
  }

  createApiInstance() {
    const apiInstance = axios.create({
      baseURL: API_BASE_URL,
      withCredentials: true,
      withXSRFToken: true,
      xsrfCookieName: "XSRF-TOKEN",
      xsrfHeaderName: "X-XSRF-TOKEN",
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
        const accessToken = store.getState().user.accessToken;
        if (accessToken) {
          config.headers = config.headers ?? {};
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
        const isRefreshableRequest =
          originalRequest &&
          !originalRequest._retry &&
          !originalRequest.url?.includes("/api/auth/");

        if (error.response?.status === 401 && isRefreshableRequest) {
          originalRequest._retry = true;

          try {
            const newAccessToken = await this.refreshAccessToken();
            originalRequest.headers = originalRequest.headers ?? {};
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

  refreshAccessToken() {
    if (!this.refreshPromise) {
      this.refreshPromise = this.bootstrapCsrf()
        .then(() =>
          axios.post(this.REFRESH_URL, null, {
            withCredentials: true,
            withXSRFToken: true,
            xsrfCookieName: "XSRF-TOKEN",
            xsrfHeaderName: "X-XSRF-TOKEN",
          })
        )
        .then((response) => {
          const accessToken = response.data.accessToken;
          store.dispatch(SET_ACCESS_TOKEN(accessToken));
          return accessToken;
        })
        .catch((error) => {
          if (error.response?.status === 401 || error.response?.status === 403) {
            store.dispatch(LOGOUT());
            window.location.replace("/auth/login");
          }
          throw error;
        })
        .finally(() => {
          this.refreshPromise = null;
        });
    }

    return this.refreshPromise;
  }

  bootstrapCsrf() {
    if (!this.csrfPromise) {
      this.csrfPromise = axios
        .get(`${API_BASE_URL}/api/auth/csrf`, { withCredentials: true })
        .finally(() => {
          this.csrfPromise = null;
        });
    }

    return this.csrfPromise;
  }

  getAuthHeader(token) {
    return `${this.TOKEN_TYPE} ${token}`;
  }
}

const httpClientManager = new HttpClientManager();
export default httpClientManager;
