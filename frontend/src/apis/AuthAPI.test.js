import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const dependencies = vi.hoisted(() => ({
  api: { post: vi.fn() },
  bootstrapCsrf: vi.fn(),
  createApiInstance: vi.fn(),
}));

vi.mock("./HttpClientManager", () => ({
  default: {
    bootstrapCsrf: dependencies.bootstrapCsrf,
    createApiInstance: dependencies.createApiInstance,
  },
}));

const loadAuthApi = () => import("./AuthAPI");

beforeEach(() => {
  vi.resetModules();
  vi.clearAllMocks();
  dependencies.createApiInstance.mockReturnValue(dependencies.api);
  dependencies.bootstrapCsrf.mockResolvedValue({});
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("AuthAPI cookie-backed authentication", () => {
  it("bootstraps CSRF and sends login credentials without a JS-readable refresh token", async () => {
    const localStorage = { getItem: vi.fn(() => "stale-refresh-token") };
    vi.stubGlobal("localStorage", localStorage);
    const response = { data: { accessToken: "access-token" } };
    dependencies.api.post.mockResolvedValue(response);
    const { login } = await loadAuthApi();

    await expect(login({ email: "member@example.com", password: "password" })).resolves.toBe(response);

    expect(dependencies.bootstrapCsrf).toHaveBeenCalledOnce();
    expect(dependencies.api.post).toHaveBeenCalledWith("/api/auth/login", {
      email: "member@example.com",
      password: "password",
    });
    expect(localStorage.getItem).not.toHaveBeenCalled();
  });

  it("uses the cookie-backed logout endpoint after CSRF bootstrap", async () => {
    const response = { status: 204 };
    dependencies.api.post.mockResolvedValue(response);
    const { logout } = await loadAuthApi();

    await expect(logout()).resolves.toBe(response);

    expect(dependencies.bootstrapCsrf).toHaveBeenCalledOnce();
    expect(dependencies.api.post).toHaveBeenCalledWith("/api/auth/logout");
  });
});
