// 서버 주소
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
export const WS_BASE_URL =
  import.meta.env.VITE_WS_BASE_URL ??
  API_BASE_URL.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
export const IMAGE_URL = `${API_BASE_URL}/auction/images`;
