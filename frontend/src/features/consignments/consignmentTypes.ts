/** 首版支持的藏品类型。 / Collectible categories supported in the first phase. */
export type ConsignmentCategory = "CARD" | "TOY";

/** 仓库核验实物用的编号类型。 / Identifier types used to verify physical items. */
export type ConsignmentIdentityKind = "GRADING_CERTIFICATE" | "ITEM_SERIAL";

/** 由后端控制的寄存状态。 / Consignment states controlled by the backend. */
export type ConsignmentStatus =
  | "PENDING_ARRIVAL"
  | "RECEIVED"
  | "APPROVED_PENDING_MINT"
  | "MINTED"
  | "REJECTED_PENDING_RETURN"
  | "RETURNED";

/** 寄存页面与仓库工作台共享的单据详情。 / Consignment detail shared by the depositor and warehouse screens. */
export interface Consignment {
  /** 后端生成的寄存单 UUID。 / Backend-generated consignment UUID. */
  consignmentId: string;
  /** 原寄存人的 Solana Base58 地址。 / Original depositor's Solana Base58 wallet address. */
  depositorWallet: string;
  /** 申报的藏品类型。 / Category declared by the depositor. */
  category: ConsignmentCategory;
  /** 申报名称。 / Title declared by the depositor. */
  submittedTitle: string;
  /** 申报品相。 / Condition declared by the depositor. */
  submittedCondition: string;
  /** 实物编号类型。 / Type of the physical item identifier. */
  identityKind: ConsignmentIdentityKind;
  /** 发放实物编号的评级机构或厂商。 / Grading service or manufacturer that issued the identifier. */
  identityIssuer: string;
  /** 实物证书编号或序列号。 / Grading certificate number or physical serial. */
  identityNumber: string;
  /** 寄入承运商，未填写时为空。 / Inbound carrier, null when omitted. */
  inboundCarrier: string | null;
  /** 寄入运单号，未填写时为空。 / Inbound tracking number, null when omitted. */
  inboundTrackingNumber: string | null;
  /** 当前状态代码。 / Current workflow state. */
  status: ConsignmentStatus;
  /** 收货的 ISO 8601 时间，收货前为空。 / ISO 8601 receiving time, null before receipt. */
  receivedAt: string | null;
  /** 验收的 ISO 8601 时间，决定前为空。 / ISO 8601 inspection time, null before the decision. */
  reviewedAt: string | null;
  /** 拒收原因，拒收前为空。 / Rejection reason, null before rejection. */
  rejectionReason: string | null;
  /** 实物退回的 ISO 8601 时间，退回前为空。 / ISO 8601 return time, null before the return. */
  returnedAt: string | null;
  /** 退件运单号，未填写时为空。 / Return tracking number, null when omitted. */
  returnTrackingNumber: string | null;
  /** 创建申请的 ISO 8601 时间。 / ISO 8601 time of submission. */
  createdAt: string;
  /** 最后更新的 ISO 8601 时间。 / ISO 8601 time of the last update. */
  updatedAt: string;
  /** 验收生成的实物 UUID，入库前为空。 / Custody item UUID, null before intake. */
  itemId: string | null;
}

/** 寄存列表的可选过滤和分页条件。 / Optional filter and pagination parameters for a consignment list. */
export interface ConsignmentListParams {
  /** 后端状态代码，省略时查询全部状态。 / Backend status code, omitted to include all states. */
  status?: ConsignmentStatus;
  /** 从 1 开始的页码，后端默认第 1 页。 / One-based page number, defaulting to 1 on the backend. */
  page?: number;
  /** 每页 1 至 100 条，后端默认 20 条。 / Entries per page from 1 to 100, defaulting to 20 on the backend. */
  size?: number;
}

/** 匹配当前条件的分页结果。 / A page of records matching the current query. */
export interface ConsignmentPage {
  /** 当前页返回的单据。 / Records returned on the current page. */
  items: Consignment[];
  /** 从 1 开始的当前页码。 / One-based number of the current page. */
  page: number;
  /** 每页条数。 / Requested number of entries per page. */
  size: number;
  /** 匹配当前条件的总条数。 / Total number of records matching the current filters. */
  total: number;
}

/** 页面显示的寄存状态名称。 / Consignment state labels shown in the UI. */
export const consignmentStatusLabels: Record<ConsignmentStatus, string> = {
  PENDING_ARRIVAL: "待寄入",
  RECEIVED: "已收货",
  APPROVED_PENDING_MINT: "已验收待铸造",
  MINTED: "已铸造",
  REJECTED_PENDING_RETURN: "拒收待退回",
  RETURNED: "已退回",
};

/** 页面显示的藏品分类名称。 / Collectible category labels shown in the UI. */
export const consignmentCategoryLabels: Record<ConsignmentCategory, string> = {
  CARD: "卡牌",
  TOY: "潮玩",
};

/** 页面显示的实物编号类型名称。 / Physical identifier type labels shown in the UI. */
export const consignmentIdentityKindLabels: Record<ConsignmentIdentityKind, string> = {
  GRADING_CERTIFICATE: "鉴定证书",
  ITEM_SERIAL: "实物序列号",
};
