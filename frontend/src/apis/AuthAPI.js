import HttpClientManager from "./HttpClientManager";

export const AuthApi = HttpClientManager.createApiInstance();

export const login = async ({ email, password }) => {
  try {
    await HttpClientManager.bootstrapCsrf();
    const response = await AuthApi.post("/api/auth/login", { email, password });
    return response;
  } catch (error) {
    throw error;
  }
};

export const signUp = async (formData) => {
  try {
    await HttpClientManager.bootstrapCsrf();
    const response = await AuthApi.post("/api/auth/signup", formData);
    return response.data;
  } catch (error) {
    console.error("Signup failed:", error);
    throw error;
  }
};

export const logout = async () => {
  try {
    await HttpClientManager.bootstrapCsrf();
    const response = await AuthApi.post("/api/auth/logout");
    return response;
  } catch (error) {
    throw error;
  }
};
