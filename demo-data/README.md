# Trove 演示数据 / Trove Demo Data

这套数据供 Localnet 和 Devnet 联调时手动填写表单、登记实物并走完整业务流程。JSON 保存演示输入和预期步骤。

These JSON files provide sample inputs and expected steps for manual Localnet and Devnet walkthroughs.

| 文件 / File | 用途 / Use |
| --- | --- |
| [cards.json](cards.json) | 两张卡牌。第一张走寄存、转售和兑换，第二张演示验真驳回及退回。 / Two cards for the end-to-end trade and redemption flow and the rejected consignment flow. |
| [art-toys.json](art-toys.json) | 一件普通寄存潮玩，以及三件预留给盲盒的潮玩。 / One consignment toy and three toys reserved for the blind-box pool. |
| [blind-box-pool.json](blind-box-pool.json) | 一个限量系列、测试 SOL 价格和奖品实物清单。 / One limited series, its test-SOL price, and the physical prizes. |
| [mock-logistics.json](mock-logistics.json) | 模拟寄入、验真驳回退回，以及兑换寄出与签收。 / Simulated inbound delivery, return after rejection, and outbound fulfillment. |

## 演示顺序 / Walkthrough

1. 钱包 A 使用 `DEMO-CARD-001` 提交寄存并填写对应运单。库管员登记收货、验真，给 A 铸造 NFT。用 `DEMO-TOY-001` 可以走同一条寄存流程。 / Wallet A submits `DEMO-CARD-001` with its matching parcel details. The operator receives the parcel, verifies it, and mints the NFT to A. `DEMO-TOY-001` uses the same workflow.
2. A 挂售卡牌，B 买入后转售给 C。C 兑换后，库管员等链上凭证最终确认，再按模拟出库单登记发货和签收。 / A lists the card, B buys it and sells it to C. After C redeems it and the on-chain receipt is finalized, the operator uses the outbound sample to record shipping and delivery.
3. 用 `DEMO-CARD-002` 演示收货后验真驳回和退回。驳回原因和退件运单在物流文件里。 / Use `DEMO-CARD-002` to demonstrate receipt, rejection, and return using the sample reason and tracking number.
4. 从 `DEMO-POOL-001` 购买并开一盒。开出的潮玩使用奖池里分配的实物编号铸造，之后进入同一个「我的收藏」。 / Buy and open a box from `DEMO-POOL-001`. Mint the toy using its assigned physical identity and display it in the same collection.

## 字段和边界 / Fields and boundaries

- `fixtureId` 是样例引用，物流的 `itemFixtureId` 和奖池的 `prizeFixtureIds` 用它关联藏品。业务系统执行时生成自己的寄存单、藏品 UUID、Mint 和交易签名。 / `fixtureId` links each sample to its logistics and prize pool entries. Runtime consignments, item UUIDs, mints, and signatures come from the application and Solana.
- 实物用 `identityKind`、`identityIssuer`、`identityNumber` 标识。`inventoryUse` 为 `BLIND_BOX_PRIZE` 的藏品划入奖池专用库存，开盒后才进入交易流程。奖池库存由奖品清单的数量得出。 / The identity kind, issuer, and number identify a physical item. `BLIND_BOX_PRIZE` stock enters the marketplace only after opening. The prize list defines the pool's stock.
- 盲盒 `priceLamports` 用整数保存，`50000000` lamports 等于 `0.05 SOL`。该文件用于盲盒联调，具体表结构和抽取机制按业务设计确定。 / `priceLamports` is an integer. The sample price of `50000000` lamports equals `0.05 SOL`. The blind-box schema and draw mechanism follow the business design.
- 钱包 A、B、C 是演示身份。运单、收件人、电话、地址和藏品编号都是虚构样例，物流状态以演示时执行的动作为准。 / A, B, and C are demo wallet aliases. Parcel details, recipients, phone numbers, addresses, and identifiers are fictional. Logistics status advances as each demo action is performed.
