import httpClientManager from "./HttpClientManager";

// API 인스턴스 생성
const UserApi = httpClientManager.createApiInstance();

export const getUser = async () => {
  try {
    const response = await UserApi.get(`/api/users`);
    return response;
  } catch (error) {
    throw error;
  }
};
