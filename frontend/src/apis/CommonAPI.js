import axios from "axios";
import { API_SERVER_URL } from "../utils/constant";

export const CommonApi = axios.create({
  baseURL: API_SERVER_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export const getCategoryList = async () => {
  try {
    const response = await CommonApi.get("/api/categories");
    return response;
  } catch (error) {
    throw error;
  }
};
