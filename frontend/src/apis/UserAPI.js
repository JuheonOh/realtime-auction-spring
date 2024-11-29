import { API_BASE_URL } from "@utils/constant";
import { EventSourcePolyfill, NativeEventSource } from "event-source-polyfill";
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

export const getNotificationStream = (token, userId) => {
  try {
    const EventSource = EventSourcePolyfill || NativeEventSource;
    const eventSource = new EventSource(`${API_BASE_URL}/api/users/${userId}/notifications/stream`, {
      headers: {
        Authorization: httpClientManager.getAuthHeader(token),
      }
    });
    return eventSource;
  } catch (error) {
    throw error;
  }
};

export const patchNotificationAll = async () => {
  try {
    const response = await UserApi.patch(`/api/users/notifications/all`);
    return response;
  } catch (error) {
    throw error;
  }
};

export const patchNotification = async (notificationId) => {
  try {
    const response = await UserApi.patch(`/api/users/notifications`, {
      notificationId,
    });
    return response;
  } catch (error) {
    throw error;
  }
};

export const deleteNotificationAll = async () => {
  try {
    const response = await UserApi.delete(`/api/users/notifications/all`);
    return response;
  } catch (error) {
    throw error;
  }
};

export const deleteNotification = async (notificationId) => {
  try {
    const response = await UserApi.delete(`/api/users/notifications`, {
      data: {
        notificationId,
      },
    });
    return response;
  } catch (error) {
    throw error;
  }
};
