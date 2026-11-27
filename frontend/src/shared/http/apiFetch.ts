import { appConfig } from "../config/appConfig";

/** 后端 HTTP 错误，用于区分未登录、权限不足和其他请求失败。 / Backend HTTP failure used to distinguish sign-in, permissions, and other request errors. */
export class ApiError extends Error {
  /** 后端返回的 HTTP 状态码。 / HTTP status returned by the backend. */
  readonly status: number;

  /** 后端业务错误码，响应未提供时为空。 / Backend error code, absent when omitted by the response. */
  readonly code: string | undefined;

  /**
   * 保存后端响应中的错误信息。 / Preserve the error details from the backend response.
   * @param status HTTP 状态码。 / HTTP status code.
   * @param message 页面可显示的错误说明。 / Error message suitable for display.
   * @param code 可选的业务错误码。 / Optional application error code.
   */
  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

/** 发往 Trove 后端的 JSON 请求参数。 / Options for a JSON request to the Trove backend. */
interface ApiRequestOptions {
  /** HTTP 方法，省略时使用 GET。 / HTTP method, defaulting to GET. */
  method?: string;
  /** 可序列化的请求体。 / Serializable request body. */
  body?: unknown;
  /** 当前页面内存中的钱包会话令牌。 / Wallet session token held in the current page's memory. */
  token?: string;
  /** 切换钱包或离开页面时取消该请求。 / Cancels the request on wallet switch or page exit. */
  signal?: AbortSignal;
}

/**
 * 请求 Trove 的 JSON 接口，204 响应对应 void。 / Calls a Trove JSON endpoint, with 204 responses corresponding to void.
 * @param path 后端接口路径，以斜杠开头。 / Backend endpoint path beginning with a slash.
 * @param options 方法、请求体、令牌及取消信号。 / Method, body, token, and cancellation signal.
 * @returns 对应接口的响应数据。 / Response data for the endpoint.
 * @throws HTTP 失败抛出 ApiError，网络故障和取消信号交给调用方处理。 / Throws ApiError for HTTP failures and passes network failures and cancellation to the caller.
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = "GET", body, token, signal } = options;
  const headers = new Headers({ Accept: "application/json" });
  if (body !== undefined) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  // 钱包会话随当前请求发送，关闭缓存、Cookie 和重定向。 / Scope the wallet session to this request and disable caching, cookies, and redirects.
  const response = await fetch(`${appConfig.apiBaseUrl.replace(/\/+$/, "")}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
    cache: "no-store",
    credentials: "omit",
    redirect: "error",
  });

  if (!response.ok) {
    // 代理或服务器可能返回非 JSON 错误，保留 HTTP 状态供页面判断。 / Retain the HTTP status even when a proxy or server returns a non-JSON error.
    const error = await response.json().catch(() => null);
    const message = typeof error?.message === "string" ? error.message : `请求失败（${response.status}）`;
    const code = typeof error?.code === "string" ? error.code : undefined;
    throw new ApiError(response.status, message, code);
  }

  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}
