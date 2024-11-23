import { API_BASE_URL } from "@utils/constant";
import axios from "axios";
import httpClientManager from "./HttpClientManager";

const AuctionApi = httpClientManager.createApiInstance();

//////////////////////////////////////
// 인증이 필요한 API
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
export const createBid = async (auctionId, bidAmount) => {
  try {
    const response = await AuctionApi.post(`/api/auctions/${auctionId}/bids`, { bidAmount });
    return response;
  } catch (error) {
    throw error;
  }
};

// 즉시 구매
export const buyNowAuction = async (auctionId) => {
  try {
    const response = await AuctionApi.post(`/api/auctions/${auctionId}/buy-now`);
    return response;
  } catch (error) {
    throw error;
  }
};

//////////////////////////////////////
// 인증이 필요하지 않은 API
// 경매 목록 조회
export const getAuctionList = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auctions`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 주목할 만한 경매 조회
export const getFeaturedAuctions = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auctions/featured`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 경매 상세 조회
export const getAuctionDetail = async (auctionId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auctions/${auctionId}`);
    return response;
  } catch (error) {
    throw error;
  }
};

// 입찰 스트림 조회
export const getAuctionBidStream = async (auctionId) => {
  try {
    const eventSource = new EventSource(`${API_BASE_URL}/api/auctions/${auctionId}/bids-stream`);
    return eventSource;
  } catch (error) {
    throw error;
  }
};
