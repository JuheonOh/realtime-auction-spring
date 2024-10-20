import axios from "axios";

const BASE_URL = "http://localhost:8080";

export const CommonApi = axios.create({
  baseURL: BASE_URL,
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
