import axios from "axios";
import { clearCookie, getCookie, setCookie } from "../data/storage/Cookie";

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

//////////////////////////////////////
// No Auth

// 경매 목록 조회
export const getAuctionList = async () => {
  try {
    const response = await axios.get(`${BASE_URL}/api/auctions`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 경매 상세 조회
export const getAuctionDetail = async (auctionId) => {
  try {
    const response = await axios.get(`${BASE_URL}/api/auctions/${auctionId}`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 입찰 내역 조회
export const getBidList = async (auctionId) => {
  try {
    const response = await axios.get(`${BASE_URL}/api/bids/${auctionId}`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 입찰 스트림 조회
export const getAuctionBidStream = async (auctionId) => {
  try {
    const eventSource = new EventSource(`${BASE_URL}/api/auctions/${auctionId}/bids-stream`);
    return eventSource;
  } catch (error) {
    throw error;
  }
};

//////////////////////////////////////
// Auth
// 경매 생성
export const createAuction = async (formData) => {
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

// 입찰 생성
export const createBid = async (auctionId, bidAmount, userId) => {
  try {
    const response = await AuctionApi.post(`/api/auctions/${auctionId}/bids`, {
      bidAmount,
      userId,
    });
    return response;
  } catch (error) {
    throw error;
  }
};

// 즉시 구매
export const buyNowAuction = async (auctionId, userId) => {
  try {
    const response = await AuctionApi.post(`/api/auctions/${auctionId}/buy-now`, { userId });
    return response;
  } catch (error) {
    throw error;
  }
};
