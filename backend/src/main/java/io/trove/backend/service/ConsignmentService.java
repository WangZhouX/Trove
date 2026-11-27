package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.api.ConsignmentApi;
import io.trove.backend.entity.ConsignmentDO;

import java.util.UUID;

/**
 * 对已认证的钱包提供寄存和仓库验收业务。
 * Provides consignment and warehouse inspection operations for authenticated wallets.
 */
public interface ConsignmentService extends IService<ConsignmentDO> {

    /**
     * 受理寄存申请，原样重试时返回原单。 / Accept a consignment application and replay an identical request.
     * @param walletAddress 从会话获得的寄存钱包 / depositor wallet from the session
     * @param request 有客户端请求编号的寄存申报 / application with a client request identifier
     * @return 寄存详情及本次是否新建 / consignment details and whether this call created it
     */
    ConsignmentApi.Submission submit(String walletAddress, ConsignmentApi.CreateRequest request);

    /**
     * 按时间倒序查询本钱包的单据。 / Lists the current wallet's consignments, newest first.
     * @param walletAddress 从会话获得的寄存钱包 / depositor wallet from the session
     * @param status 可选状态，空时查询全部 / optional status, null for all
     * @param page 从 1 起的页码 / one-based page number
     * @param size 每页 1 至 100 条 / 1 to 100 entries per page
     * @return 本钱包的分页结果 / page of the wallet's consignments
     */
    ConsignmentApi.PageResult listMine(String walletAddress, String status, int page, int size);

    /**
     * 查询本钱包的一笔寄存。 / Reads one consignment belonging to the current wallet.
     * @param walletAddress 从会话获得的寄存钱包 / depositor wallet from the session
     * @param id 寄存单 UUID / consignment UUID
     * @return 本钱包单据的详情 / details for the wallet's consignment
     */
    ConsignmentApi.Detail getMine(String walletAddress, UUID id);

    /**
     * 供库管员按时间正序查询队列。 / Lists the consignment queue, oldest first, for warehouse staff.
     * @param status 可选状态，空时查询全部 / optional status, null for all
     * @param page 从 1 起的页码 / one-based page number
     * @param size 每页 1 至 100 条 / 1 to 100 entries per page
     * @return 仓库队列分页结果 / page of warehouse queue entries
     */
    ConsignmentApi.PageResult listForWarehouse(String status, int page, int size);

    /**
     * 供库管员查看任意寄存单。 / Retrieves a consignment for authorized warehouse staff.
     * @param id 寄存单 UUID / consignment UUID
     * @return 仓库可查询的单据详情 / consignment details visible to warehouse staff
     */
    ConsignmentApi.Detail getForWarehouse(UUID id);

    /**
     * 登记待到仓实物的签收。 / Records receipt of a consignment awaiting arrival.
     * @param actor 已授权的库管钱包 / authorized warehouse wallet
     * @param id 寄存单 UUID / consignment UUID
     * @return 收货后的单据 / consignment after receipt
     */
    ConsignmentApi.Detail receive(String actor, UUID id);

    /**
     * 验收通过并在同一事务内登记在库实物。 / Approves inspection and registers the custody item in the same transaction.
     * @param actor 已授权的库管钱包 / authorized warehouse wallet
     * @param id 寄存单 UUID / consignment UUID
     * @param request 核验后的名称、品相和仓位 / verified title, condition, and location
     * @return 待铸造单据和稳定实物编号 / mint-pending consignment and stable item identifier
     */
    ConsignmentApi.Detail approve(String actor, UUID id, ConsignmentApi.ApproveRequest request);

    /**
     * 登记验收拒收和原因。 / Records a failed inspection and its reason.
     * @param actor 已授权的库管钱包 / authorized warehouse wallet
     * @param id 寄存单 UUID / consignment UUID
     * @param request 拒收原因 / reason for rejection
     * @return 拒收待退回单据 / rejected consignment awaiting return
     */
    ConsignmentApi.Detail reject(String actor, UUID id, ConsignmentApi.RejectRequest request);

    /**
     * 登记拒收实物完成退件。 / Records the completed return of a rejected consignment.
     * @param actor 已授权的库管钱包 / authorized warehouse wallet
     * @param id 寄存单 UUID / consignment UUID
     * @param request 可选的退件运单信息 / optional return tracking information
     * @return 已退件单据 / completed returned consignment
     */
    ConsignmentApi.Detail recordReturn(String actor, UUID id, ConsignmentApi.ReturnRequest request);
}
