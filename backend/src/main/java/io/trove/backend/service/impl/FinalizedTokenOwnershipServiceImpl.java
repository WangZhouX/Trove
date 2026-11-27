package io.trove.backend.service.impl;

import io.trove.backend.service.FinalizedTokenOwnershipService;
import io.trove.backend.utils.SolanaWalletUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按会话网络访问配置的 Solana 节点，读取钱包直接持有的标准 SPL Token。
 * Reads standard SPL Token accounts directly held by a wallet from the node configured for the session network.
 */
@Service
class FinalizedTokenOwnershipServiceImpl implements FinalizedTokenOwnershipService {

    // Trove 普通 NFT 使用标准 SPL Token 程序。请求在六秒内完成或返回可重试错误。
    // Trove standard NFTs use the classic SPL Token program. A request completes or fails retryably within six seconds.
    private static final String TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);

    // 复用 HTTP 连接和应用 JSON 映射，按配置中的固定地址访问各自网络。
    // Reuse HTTP connections and the application JSON mapper, with a fixed configured endpoint per network.
    private final HttpClient client;
    private final JsonMapper json;
    private final Map<String, URI> endpoints;

    /**
     * 为当前后端实例建立只读的 RPC 客户端。 / Initializes a read-only RPC client for the application.
     *
     * @param json 应用 JSON 映射器 / application JSON mapper
     * @param devnetEndpoint 运维配置的 Devnet HTTP RPC 地址 / operator-configured Devnet HTTP RPC endpoint
     * @param localnetEndpoint 运维配置的 Localnet HTTP RPC 地址 / operator-configured Localnet HTTP RPC endpoint
     */
    FinalizedTokenOwnershipServiceImpl(JsonMapper json,
                                      @Value("${trove.solana.devnet-rpc-url}") URI devnetEndpoint,
                                      @Value("${trove.solana.localnet-rpc-url}") URI localnetEndpoint) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.json = json;
        this.endpoints = Map.of("devnet", devnetEndpoint, "localnet", localnetEndpoint);
    }

    /** {@inheritDoc} */
    @Override
    public Ownership ownedMints(String network, String walletAddress) {
        // RPC 地址只由服务端按会话网络选择，HTTPS 或本地 HTTP 均使用固定配置。
        // Only server configuration selects the RPC URL for the session network, including local HTTP.
        URI endpoint = endpoints.get(network);
        if (endpoint == null || !SolanaWalletUtil.isAddress(walletAddress)) {
            throw new Unavailable();
        }

        try {
            // 使用原始原子余额和精度，避免将浮点显示值当作持有凭证。
            // Request raw amount and decimals so a rounded display value cannot be treated as ownership.
            String body = json.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "getTokenAccountsByOwner",
                    "params", List.of(walletAddress, Map.of("programId", TOKEN_PROGRAM),
                            Map.of("commitment", "finalized", "encoding", "jsonParsed"))));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new Unavailable();
            }
            return readOwnership(json.readTree(response.body()), walletAddress);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Unavailable();
        } catch (IOException | JacksonException | IllegalArgumentException exception) {
            // 避免把供应商错误、带凭据的 URL 或原始正文传给前端。
            // Keep provider errors, credential-bearing URLs and raw response bodies out of the client response.
            throw new Unavailable();
        }
    }

    /**
     * 解析同一最终确认快照内的区块槽和账户余额。
     * Reads the slot and token balances from the same finalized snapshot.
     *
     * @param envelope Solana JSON-RPC 结果 / Solana JSON-RPC response envelope
     * @param walletAddress 本次查询的所有者地址 / token owner address requested by this call
     * @return 与钱包匹配的 Mint 集合与最终确认区块槽 / matching Mint addresses and finalized context slot
     * @throws Unavailable RPC 错误或无法可靠解析账户 / on RPC error or unreadable account data
     */
    private static Ownership readOwnership(JsonNode envelope, String walletAddress) {
        // 缺少区块槽或账户列表表明整次查询不可用，不能按无持仓返回。
        // Missing slot or account list makes the whole query unavailable instead of reporting zero holdings.
        if (envelope == null || envelope.hasNonNull("error")) {
            throw new Unavailable();
        }
        JsonNode result = envelope.path("result");
        JsonNode slot = result.path("context").path("slot");
        JsonNode accounts = result.path("value");
        if (!slot.isIntegralNumber() || !slot.canConvertToLong() || slot.longValue() < 0 || !accounts.isArray()) {
            throw new Unavailable();
        }

        // 钱包可能持有多个同 Mint 的 Token 账户，响应按 Mint 去重。
        // A wallet can have several token accounts for one Mint, so deduplicate by Mint address.
        Set<String> mints = new HashSet<>();
        for (JsonNode row : accounts) {
            JsonNode account = row.path("account");
            JsonNode parsed = account.path("data").path("parsed");
            JsonNode info = parsed.path("info");
            JsonNode amount = info.path("tokenAmount");
            JsonNode rawAmount = amount.path("amount");
            JsonNode decimals = amount.path("decimals");
            String mint = info.path("mint").asString("");
            // 即使返回了账户，也需校验程序、结构和用户归属，避免使用错误的数据。
            // Verify program, parsed type and wallet ownership before relying on any returned account.
            if (!TOKEN_PROGRAM.equals(account.path("owner").asString(""))
                    || !"account".equals(parsed.path("type").asString(""))
                    || !walletAddress.equals(info.path("owner").asString(""))
                    || !rawAmount.isString() || !decimals.isIntegralNumber()
                    || !decimals.canConvertToInt() || decimals.intValue() < 0 || decimals.intValue() > 255
                    || !SolanaWalletUtil.isAddress(mint)) {
                throw new Unavailable();
            }
            if ("1".equals(rawAmount.asString()) && decimals.intValue() == 0) {
                mints.add(mint);
            }
        }
        return new Ownership(slot.longValue(), Set.copyOf(mints));
    }
}
