import { Cookies } from "react-cookie";

const cookies = new Cookies();

export const setCookie = (name, value, options) => {
  return cookies.set(name, value, { path: "/", ...options });
};

export const getCookie = (name) => {
  return cookies.get(name);
};

export const removeCookie = (name, options) => {
  return cookies.remove(name, { path: "/", ...options });
};

export const clearCookie = () => {
  cookies.remove("tokenType");
  cookies.remove("accessToken");
  cookies.remove("refreshToken");
};
