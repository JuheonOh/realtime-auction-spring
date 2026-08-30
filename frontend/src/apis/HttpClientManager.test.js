import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const dependencies = vi.hoisted(() => ({
  axios: {
    create: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
  clients: [],
  store: {
    dispatch: vi.fn(),
    getState: vi.fn(),
  },
  logout: vi.fn(() => ({ type: "user/LOGOUT" })),
  setAccessToken: vi.fn((token) => ({ payload: token, type: "user/SET_ACCESS_TOKEN" })),
}));

vi.mock("axios", () => ({ default: dependencies.axios }));
vi.mock("@data/redux/store", () => ({ default: dependencies.store }));
vi.mock("@data/redux/store/User", () => ({
  LOGOUT: dependencies.logout,
  SET_ACCESS_TOKEN: dependencies.setAccessToken,
}));

const makeClient = () => {
  const client = vi.fn();
  client.interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  };
  dependencies.clients.push(client);
  return client;
};

const loadManager = async () => (await import("./HttpClientManager")).default;

beforeEach(() => {
  vi.resetModules();
  vi.clearAllMocks();
  dependencies.clients.length = 0;
  dependencies.axios.create.mockImplementation(makeClient);
  dependencies.axios.get.mockResolvedValue({});
  dependencies.axios.post.mockResolvedValue({ data: { accessToken: "fresh-access-token" } });
  dependencies.store.getState.mockReturnValue({ user: { accessToken: null } });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("HttpClientManager", () => {
  it("uses only the in-memory Redux access token for request authorization", async () => {
    const localStorage = { getItem: vi.fn(() => "stale-refresh-token") };
    vi.stubGlobal("localStorage", localStorage);
    const manager = await loadManager();
    const client = manager.createApiInstance();
    const requestInterceptor = client.interceptors.request.use.mock.calls[0][0];

    const unauthenticatedRequest = requestInterceptor({ headers: {} });
    expect(unauthenticatedRequest.headers.Authorization).toBeUndefined();
    expect(localStorage.getItem).not.toHaveBeenCalled();

    dependencies.store.getState.mockReturnValue({ user: { accessToken: "memory-access-token" } });
    const authenticatedRequest = requestInterceptor({ headers: {} });
    expect(authenticatedRequest.headers.Authorization).toBe("Bearer memory-access-token");
  });

  it("deduplicates concurrent CSRF bootstraps", async () => {
    let resolveCsrf;
    dependencies.axios.get.mockReturnValue(
      new Promise((resolve) => {
        resolveCsrf = resolve;
      })
    );
    const manager = await loadManager();

    const first = manager.bootstrapCsrf();
    const second = manager.bootstrapCsrf();
    expect(dependencies.axios.get).toHaveBeenCalledTimes(1);
    expect(dependencies.axios.get).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/csrf",
      { withCredentials: true }
    );

    resolveCsrf({});
    await expect(Promise.all([first, second])).resolves.toEqual([{}, {}]);
  });

  it("shares one cookie-backed refresh request across concurrent 401 responses and retries each request once", async () => {
    let resolveCsrf;
    dependencies.axios.get.mockReturnValue(
      new Promise((resolve) => {
        resolveCsrf = resolve;
      })
    );
    const manager = await loadManager();
    const client = manager.createApiInstance();
    client.mockImplementation((request) => Promise.resolve({ retried: request }));
    const responseInterceptor = client.interceptors.response.use.mock.calls[0][1];
    const firstRequest = { headers: {}, url: "/api/auctions/1" };
    const secondRequest = { headers: {}, url: "/api/auctions/2" };

    const firstRetry = responseInterceptor({ config: firstRequest, response: { status: 401 } });
    const secondRetry = responseInterceptor({ config: secondRequest, response: { status: 401 } });
    expect(dependencies.axios.get).toHaveBeenCalledTimes(1);

    resolveCsrf({});
    await expect(Promise.all([firstRetry, secondRetry])).resolves.toEqual([
      { retried: firstRequest },
      { retried: secondRequest },
    ]);

    expect(dependencies.axios.post).toHaveBeenCalledTimes(1);
    expect(dependencies.axios.post).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/refresh",
      null,
      expect.objectContaining({
        withCredentials: true,
        withXSRFToken: true,
        xsrfCookieName: "XSRF-TOKEN",
        xsrfHeaderName: "X-XSRF-TOKEN",
      })
    );
    expect(dependencies.store.dispatch).toHaveBeenCalledWith({
      payload: "fresh-access-token",
      type: "user/SET_ACCESS_TOKEN",
    });
    expect(firstRequest).toMatchObject({
      _retry: true,
      headers: { Authorization: "Bearer fresh-access-token" },
    });
    expect(secondRequest).toMatchObject({
      _retry: true,
      headers: { Authorization: "Bearer fresh-access-token" },
    });

    const repeated401 = { config: firstRequest, response: { status: 401 } };
    await expect(responseInterceptor(repeated401)).rejects.toBe(repeated401);
    expect(dependencies.axios.post).toHaveBeenCalledTimes(1);
  });

  it("preserves response-less errors without causing a secondary TypeError", async () => {
    const manager = await loadManager();
    const client = manager.createApiInstance();
    const responseInterceptor = client.interceptors.response.use.mock.calls[0][1];
    const networkError = new Error("Network Error");

    await expect(responseInterceptor(networkError)).rejects.toBe(networkError);
    expect(dependencies.axios.post).not.toHaveBeenCalled();
  });

  it("logs out and redirects once when the shared refresh definitively fails", async () => {
    const replace = vi.fn();
    vi.stubGlobal("window", { location: { replace } });
    const refreshError = { response: { status: 401 } };
    dependencies.axios.post.mockRejectedValue(refreshError);
    const manager = await loadManager();
    const client = manager.createApiInstance();
    const responseInterceptor = client.interceptors.response.use.mock.calls[0][1];

    const first = responseInterceptor({ config: { url: "/api/auctions/1" }, response: { status: 401 } });
    const second = responseInterceptor({ config: { url: "/api/auctions/2" }, response: { status: 401 } });
    await expect(Promise.allSettled([first, second])).resolves.toEqual([
      { status: "rejected", reason: refreshError },
      { status: "rejected", reason: refreshError },
    ]);

    expect(dependencies.axios.post).toHaveBeenCalledTimes(1);
    expect(dependencies.logout).toHaveBeenCalledTimes(1);
    expect(dependencies.store.dispatch).toHaveBeenCalledWith({ type: "user/LOGOUT" });
    expect(replace).toHaveBeenCalledOnce();
    expect(replace).toHaveBeenCalledWith("/auth/login");
  });
});
