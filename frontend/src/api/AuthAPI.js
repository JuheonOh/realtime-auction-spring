import axios from "axios";

const TOKEN_TYPE = localStorage.getItem("tokenType");
let ACCESS_TOKEN = localStorage.getItem("accessToken");

export const AuthApi = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
    Authorization: `${TOKEN_TYPE} ${ACCESS_TOKEN}`,
  },
});

export const login = async ({ email, password }) => {
  const data = { email, password };
  const response = await AuthApi.post("/api/auth/login", data);
  return response.data;
};

export const signUp = async (formData) => {
  formData.agreeTerms = formData.agreeTerms === "on" ? true : false;

  const data = formData;
  const response = await AuthApi.post("/api/auth/signup", data);
  return response.data;
};
