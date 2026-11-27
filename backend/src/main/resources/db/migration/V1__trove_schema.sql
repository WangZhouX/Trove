-- Trove 第一阶段 PostgreSQL 建表草案，供空数据库评审和验证。
-- Trove Phase 1 PostgreSQL schema draft for review and verification in an empty database.

-- 钱包身份与仓储寄存。 / Wallet identity and warehouse consignments.
CREATE TABLE wallet_account (
    wallet_address varchar(44) COLLATE "C" PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_verified_at timestamptz,
    last_verified_at timestamptz,
    CONSTRAINT ck_wallet_account_address
        CHECK (wallet_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    CONSTRAINT ck_wallet_account_verification_times CHECK (
        (first_verified_at IS NULL AND last_verified_at IS NULL)
        OR (first_verified_at IS NOT NULL AND last_verified_at IS NOT NULL
            AND first_verified_at >= created_at AND last_verified_at >= first_verified_at)
    )
);
COMMENT ON TABLE wallet_account IS '曾发起钱包登录的身份记录。签名验证后才能用于受保护的业务。 / Identity records for wallets that initiated sign-in. Protected operations require verified signatures.';
COMMENT ON COLUMN wallet_account.wallet_address IS 'Solana 钱包公钥的 Base58 文本，大小写敏感。 / Case-sensitive Base58 text of a Solana wallet public key.';
COMMENT ON COLUMN wallet_account.created_at IS '身份记录创建时间，API 以 UTC 返回。 / Identity creation time, exposed in UTC by the API.';
COMMENT ON COLUMN wallet_account.first_verified_at IS '首次成功验证钱包签名的时间，空值表示尚未验证。 / First successful wallet-signature verification time, null until verified.';
COMMENT ON COLUMN wallet_account.last_verified_at IS '最近一次成功验证钱包签名的时间，空值表示尚未验证。 / Most recent successful verification time, null until verified.';

CREATE TABLE wallet_login_challenge (
    challenge_id uuid PRIMARY KEY,
    wallet_address varchar(44) COLLATE "C" NOT NULL REFERENCES wallet_account (wallet_address),
    network varchar(16) NOT NULL,
    domain varchar(255) NOT NULL,
    nonce_hash bytea NOT NULL UNIQUE,
    issued_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    CONSTRAINT ck_wallet_login_challenge_network CHECK (network IN ('localnet', 'devnet')),
    CONSTRAINT ck_wallet_login_challenge_domain
        CHECK (domain <> '' AND domain = btrim(domain)),
    CONSTRAINT ck_wallet_login_challenge_hash CHECK (octet_length(nonce_hash) = 32),
    CONSTRAINT ck_wallet_login_challenge_times CHECK (
        expires_at > issued_at
        AND (consumed_at IS NULL OR (consumed_at >= issued_at AND consumed_at < expires_at))
    )
);
-- 按钱包查询未消费挑战，批量清理时按到期时间筛选。
-- Look up unused challenges by wallet and clean them up by expiration time.
CREATE INDEX ix_wallet_login_challenge_wallet ON wallet_login_challenge (wallet_address, issued_at DESC);
CREATE INDEX ix_wallet_login_challenge_expiry ON wallet_login_challenge (expires_at);
COMMENT ON TABLE wallet_login_challenge IS '钱包签名登录的一次性挑战，按钱包、站点和网络限定使用范围。 / One-time wallet login challenge scoped to a wallet, site and network.';
COMMENT ON COLUMN wallet_login_challenge.challenge_id IS '后端生成的 UUID 登录挑战编号。 / Server-generated UUID login challenge identifier.';
COMMENT ON COLUMN wallet_login_challenge.wallet_address IS '预期签名的钱包公钥。 / Public key of the expected wallet signer.';
COMMENT ON COLUMN wallet_login_challenge.network IS '签名消息绑定的网络，localnet 或 devnet。 / Network bound to the signed message, localnet or devnet.';
COMMENT ON COLUMN wallet_login_challenge.domain IS '服务端允许的签名站点域名，可带本地开发端口。 / Server-allowed signing domain, optionally including a local development port.';
COMMENT ON COLUMN wallet_login_challenge.nonce_hash IS '随机挑战值的 SHA-256 摘要，固定 32 字节。 / Fixed 32-byte SHA-256 digest of the random nonce.';
COMMENT ON COLUMN wallet_login_challenge.issued_at IS '挑战签发时间，API 以 UTC 返回。 / Challenge issue time, exposed in UTC by the API.';
COMMENT ON COLUMN wallet_login_challenge.expires_at IS '挑战到期时间，等于当前时间时已不可用。 / Challenge expiration time, exclusive.';
COMMENT ON COLUMN wallet_login_challenge.consumed_at IS '挑战被成功消费的时间，空值表示未消费。 / Successful consumption time, null while unused.';

CREATE TABLE wallet_account_role (
    wallet_address varchar(44) COLLATE "C" NOT NULL REFERENCES wallet_account (wallet_address),
    role_code varchar(32) NOT NULL,
    granted_by_wallet varchar(44) COLLATE "C" REFERENCES wallet_account (wallet_address),
    granted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (wallet_address, role_code),
    CONSTRAINT ck_wallet_account_role_code CHECK (role_code IN ('WAREHOUSE_OPERATOR'))
);
COMMENT ON TABLE wallet_account_role IS '钱包身份与后端特殊权限的关联，普通用户权限由登录身份获得。 / Wallet-to-backend-role grants. Regular user capabilities come from the authenticated identity.';
COMMENT ON COLUMN wallet_account_role.wallet_address IS '获得权限的钱包公钥。 / Public key of the wallet receiving the role.';
COMMENT ON COLUMN wallet_account_role.role_code IS '首版特殊权限代码，WAREHOUSE_OPERATOR 为仓管员。 / Phase-one privileged role code, WAREHOUSE_OPERATOR for warehouse staff.';
COMMENT ON COLUMN wallet_account_role.granted_by_wallet IS '执行授权的人，空值表示由部署初始化。 / Wallet of the grantor, null for bootstrap grants.';
COMMENT ON COLUMN wallet_account_role.granted_at IS '权限授予时间，API 以 UTC 返回。 / Time the current grant was added, exposed in UTC by the API.';

CREATE TABLE consignment (
    consignment_id uuid PRIMARY KEY,
    depositor_wallet varchar(44) COLLATE "C" NOT NULL REFERENCES wallet_account (wallet_address),
    client_request_id uuid NOT NULL,
    category varchar(16) NOT NULL,
    submitted_title varchar(120) NOT NULL,
    submitted_condition varchar(120) NOT NULL,
    identity_kind varchar(24) NOT NULL,
    identity_issuer varchar(100) NOT NULL,
    identity_number varchar(120) NOT NULL,
    physical_identity varchar(280) COLLATE "C" NOT NULL,
    inbound_carrier varchar(80),
    inbound_tracking_number varchar(120),
    status varchar(32) NOT NULL DEFAULT 'PENDING_ARRIVAL',
    received_at timestamptz,
    received_by_wallet varchar(44) COLLATE "C" REFERENCES wallet_account (wallet_address),
    reviewed_at timestamptz,
    reviewed_by_wallet varchar(44) COLLATE "C" REFERENCES wallet_account (wallet_address),
    rejection_reason varchar(500),
    returned_at timestamptz,
    return_tracking_number varchar(120),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (depositor_wallet, client_request_id),
    UNIQUE (consignment_id, physical_identity),
    CONSTRAINT ck_consignment_category CHECK (category IN ('CARD', 'TOY')),
    CONSTRAINT ck_consignment_identity_kind
        CHECK (identity_kind IN ('GRADING_CERTIFICATE', 'ITEM_SERIAL')),
    CONSTRAINT ck_consignment_required_text CHECK (
        length(btrim(submitted_title)) > 0 AND length(btrim(submitted_condition)) > 0
        AND length(btrim(identity_issuer)) > 0 AND length(btrim(identity_number)) > 0
        AND physical_identity <> '' AND physical_identity = btrim(physical_identity)
    ),
    CONSTRAINT ck_consignment_status CHECK (status IN (
        'PENDING_ARRIVAL', 'RECEIVED', 'APPROVED_PENDING_MINT', 'MINTED',
        'REJECTED_PENDING_RETURN', 'RETURNED'
    )),
    CONSTRAINT ck_consignment_received CHECK (
        (received_at IS NULL) = (received_by_wallet IS NULL)
        AND (received_at IS NULL) = (status = 'PENDING_ARRIVAL')
        AND (received_at IS NULL OR received_at >= created_at)
    ),
    CONSTRAINT ck_consignment_reviewed CHECK (
        (reviewed_at IS NULL) = (reviewed_by_wallet IS NULL)
        AND (reviewed_at IS NULL) = (status IN ('PENDING_ARRIVAL', 'RECEIVED'))
        AND (reviewed_at IS NULL OR reviewed_at >= received_at)
    ),
    CONSTRAINT ck_consignment_rejection CHECK (
        (rejection_reason IS NOT NULL) = (status IN ('REJECTED_PENDING_RETURN', 'RETURNED'))
        AND (rejection_reason IS NULL OR length(btrim(rejection_reason)) > 0)
    ),
    CONSTRAINT ck_consignment_returned CHECK (
        (returned_at IS NOT NULL) = (status = 'RETURNED')
        AND (returned_at IS NULL OR returned_at >= reviewed_at)
        AND (return_tracking_number IS NULL OR returned_at IS NOT NULL)
    ),
    CONSTRAINT ck_consignment_timestamp_order CHECK (updated_at >= created_at)
);
-- 只有已完成拒收退件的单据释放原有实物编号。 / Only completed rejection returns release the existing physical identity.
CREATE UNIQUE INDEX uq_consignment_held_identity
    ON consignment (physical_identity) WHERE status <> 'RETURNED';
-- 支持我的寄存和仓管待办的时间排序。 / Sort the depositor history and warehouse queue by time.
CREATE INDEX ix_consignment_depositor_created ON consignment (depositor_wallet, created_at DESC, consignment_id);
CREATE INDEX ix_consignment_queue ON consignment (status, created_at, consignment_id);
COMMENT ON TABLE consignment IS '钱包寄存申报、收货验收、铸造结果及拒收退件记录。 / Wallet consignment submissions, receiving and inspection, minting results and rejection returns.';
COMMENT ON COLUMN consignment.consignment_id IS '后端生成的 UUID 寄存单编号。 / Server-generated UUID consignment identifier.';
COMMENT ON COLUMN consignment.depositor_wallet IS '原寄存人钱包，验收通过后 NFT 铸造到此地址。 / Original depositor wallet and recipient of the NFT after approval.';
COMMENT ON COLUMN consignment.client_request_id IS '客户端提供的重试标识，同一钱包内唯一。 / Client-provided idempotency identifier, unique per wallet.';
COMMENT ON COLUMN consignment.category IS '首版藏品类型，CARD 为卡牌，TOY 为潮玩。 / Phase-one category, CARD for cards and TOY for art toys.';
COMMENT ON COLUMN consignment.submitted_title IS '寄存人提交的名称，仓管验真后另存已核验标题。 / Name supplied by the depositor, with the verified title recorded separately after inspection.';
COMMENT ON COLUMN consignment.submitted_condition IS '寄存人申报的品相或描述。 / Condition or description reported by the depositor.';
COMMENT ON COLUMN consignment.identity_kind IS '实物凭据类型，鉴定证书或实物序列号。 / Type of physical identifier, grading certificate or item serial.';
COMMENT ON COLUMN consignment.identity_issuer IS '鉴定机构或制造商等编号命名空间的来源。 / Source of the identifier namespace, such as the grading service or manufacturer.';
COMMENT ON COLUMN consignment.identity_number IS '寄存人申报的证书编号或序列号原文。 / Original certificate number or serial as supplied by the depositor.';
COMMENT ON COLUMN consignment.physical_identity IS '后端按凭据类型、机构命名空间和机构规则规范化后的实物唯一键。 / Backend-normalized physical identity key combining kind, issuer namespace and the issuer-normalized identifier.';
COMMENT ON COLUMN consignment.inbound_carrier IS '寄入承运商，空值表示未登记。 / Inbound carrier, null when unrecorded.';
COMMENT ON COLUMN consignment.inbound_tracking_number IS '寄入运单号，空值表示未登记。 / Inbound tracking number, null when unrecorded.';
COMMENT ON COLUMN consignment.status IS '寄存业务状态，依次登记到仓、验收、待铸造或拒收退件。 / Consignment workflow state covering receipt, review, minting or rejected return.';
COMMENT ON COLUMN consignment.received_at IS '仓库登记到货时间，空值表示未收货。 / Warehouse receiving time, null before receipt.';
COMMENT ON COLUMN consignment.received_by_wallet IS '登记收货的仓管钱包，空值表示未收货。 / Warehouse operator wallet that recorded receipt, null before receipt.';
COMMENT ON COLUMN consignment.reviewed_at IS '仓管签发验收决定的时间，空值表示未决定。 / Time warehouse staff recorded a review decision, null before decision.';
COMMENT ON COLUMN consignment.reviewed_by_wallet IS '签发验收决定的仓管钱包，空值表示未决定。 / Warehouse operator wallet recording the review, null before decision.';
COMMENT ON COLUMN consignment.rejection_reason IS '拒收原因，接受或未决定时为空。 / Reject reason, null until rejection or for accepted consignments.';
COMMENT ON COLUMN consignment.returned_at IS '仓管办理拒收退件的时间，空值表示尚未退件或无需退件。 / Time the rejected item was returned, null while pending or not applicable.';
COMMENT ON COLUMN consignment.return_tracking_number IS '退件运单号，空值表示未登记或不适用。 / Return tracking number, null when unrecorded or inapplicable.';
COMMENT ON COLUMN consignment.created_at IS '寄存单创建时间，API 以 UTC 返回。 / Consignment creation time, exposed in UTC by the API.';
COMMENT ON COLUMN consignment.updated_at IS '后端最近一次更新寄存单的时间。 / Most recent consignment update time maintained by the backend.';

CREATE TABLE consignment_photo (
    photo_id uuid PRIMARY KEY,
    consignment_id uuid NOT NULL REFERENCES consignment (consignment_id),
    storage_key varchar(400) NOT NULL,
    content_type varchar(32) NOT NULL,
    sort_order smallint NOT NULL,
    uploaded_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (consignment_id, sort_order),
    CONSTRAINT ck_consignment_photo_storage_key CHECK (length(btrim(storage_key)) > 0),
    CONSTRAINT ck_consignment_photo_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_consignment_photo_sort_order CHECK (sort_order >= 0)
);
COMMENT ON TABLE consignment_photo IS '寄存提交的照片引用，由后端控制访问。 / References to submitted consignment photos with backend-controlled access.';
COMMENT ON COLUMN consignment_photo.photo_id IS '后端生成的 UUID 照片编号。 / Server-generated UUID photo identifier.';
COMMENT ON COLUMN consignment_photo.consignment_id IS '照片所属寄存单编号。 / Consignment identifier associated with the photo.';
COMMENT ON COLUMN consignment_photo.storage_key IS '受访问控制的存储对象键。 / Storage object key subject to backend access controls.';
COMMENT ON COLUMN consignment_photo.content_type IS '上传校验通过的图像 MIME 类型。 / Image MIME type verified during upload.';
COMMENT ON COLUMN consignment_photo.sort_order IS '寄存单内从零开始的展示顺序。 / Zero-based display position within the consignment.';
COMMENT ON COLUMN consignment_photo.uploaded_at IS '照片引用入库时间，API 以 UTC 返回。 / Time the photo reference was stored, exposed in UTC by the API.';

CREATE TABLE custody_item (
    item_id uuid PRIMARY KEY,
    consignment_id uuid NOT NULL UNIQUE,
    physical_identity varchar(280) COLLATE "C" NOT NULL UNIQUE,
    display_title varchar(120) NOT NULL,
    verified_condition varchar(120) NOT NULL,
    public_image_uri varchar(1024) CHECK (length(btrim(public_image_uri)) > 0),
    warehouse_location varchar(100) NOT NULL,
    state varchar(16) NOT NULL DEFAULT 'IN_VAULT',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipped_out_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 复合外键确保在库实物与对应寄存单沿用同一标识。 / Keep the accepted item and its consignment bound to the same identity.
    FOREIGN KEY (consignment_id, physical_identity) REFERENCES consignment (consignment_id, physical_identity),
    CONSTRAINT ck_custody_item_text CHECK (
        length(btrim(display_title)) > 0 AND length(btrim(verified_condition)) > 0
        AND length(btrim(warehouse_location)) > 0
    ),
    CONSTRAINT ck_custody_item_state CHECK (state IN ('IN_VAULT', 'SHIPPED_OUT')),
    CONSTRAINT ck_custody_item_shipped_out CHECK (
        (shipped_out_at IS NOT NULL) = (state = 'SHIPPED_OUT')
        AND (shipped_out_at IS NULL OR shipped_out_at >= created_at)
    ),
    CONSTRAINT ck_custody_item_timestamp_order CHECK (updated_at >= created_at)
);
-- 在库盘点按仓位定位。 / Locate active stock by warehouse location.
CREATE INDEX ix_custody_item_in_vault_location ON custody_item (warehouse_location) WHERE state = 'IN_VAULT';
COMMENT ON TABLE custody_item IS '验收入库后的实物身份、仓位与出库事实。 / Accepted physical item identity, storage location and outbound status.';
COMMENT ON COLUMN custody_item.item_id IS '实物稳定 UUID，对应链上 itemId，验收后复用及保留。 / Stable physical-item UUID corresponding to on-chain itemId and retained after acceptance.';
COMMENT ON COLUMN custody_item.consignment_id IS '对应的一笔已验收寄存单，每个寄存单最多生成一件在库实物。 / Related accepted consignment, with at most one custody item per consignment.';
COMMENT ON COLUMN custody_item.physical_identity IS '核验后的实物唯一键，与寄存单的规范化键一致并永久保留。 / Verified physical identity key matching the normalized consignment key and retained after checkout.';
COMMENT ON COLUMN custody_item.display_title IS '验真后允许公开的藏品标题。 / Collectible title approved for public display after verification.';
COMMENT ON COLUMN custody_item.verified_condition IS '仓管员核验后的品相。 / Physical condition verified by warehouse staff.';
COMMENT ON COLUMN custody_item.public_image_uri IS '经仓库审核且移除私人资料的公开封面 URI，可空表示尚无公开图。 / Approved public cover URI with private content removed, null when no public image is available.';
COMMENT ON COLUMN custody_item.warehouse_location IS '库管使用的实物仓位编码，出库后保留最后仓位。 / Internal warehouse location code, retaining the last location after checkout.';
COMMENT ON COLUMN custody_item.state IS '实物状态，IN_VAULT 为在库，SHIPPED_OUT 为已出库。 / Physical status, IN_VAULT for stored items and SHIPPED_OUT after warehouse release.';
COMMENT ON COLUMN custody_item.created_at IS '实物验收并建立入库记录的时间。 / Time the item was accepted and its custody record created.';
COMMENT ON COLUMN custody_item.shipped_out_at IS '仓库登记实物出库的时间，空值表示仍在库。 / Recorded warehouse release time, null while in storage.';
COMMENT ON COLUMN custody_item.updated_at IS '后端最近一次更新在库记录的时间。 / Most recent custody update time maintained by the backend.';

-- 链上事件先入库，读模型在同一事务内引用已验证的事件。
-- Record verified events before applying their projections in the same transaction.
CREATE TABLE chain_event (
    network varchar(8) NOT NULL CHECK (network IN ('localnet', 'devnet')),
    tx_signature varchar(88) NOT NULL CHECK (tx_signature ~ '^[1-9A-HJ-NP-Za-km-z]{64,88}$'),
    event_index integer NOT NULL CHECK (event_index >= 0),
    program_id varchar(44) NOT NULL CHECK (program_id ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    event_type varchar(64) NOT NULL CHECK (length(btrim(event_type)) > 0),
    slot numeric(20, 0) NOT NULL CHECK (slot BETWEEN 0 AND 18446744073709551615),
    block_time timestamptz,
    payload jsonb NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    indexed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (network, tx_signature, event_index)
);

-- 供按已配置程序和区块槽补扫及核对活动记录。
-- Support audits and event reconciliation by configured program and slot.
CREATE INDEX ix_chain_event_program_slot ON chain_event (network, program_id, slot DESC);

COMMENT ON TABLE chain_event IS '经 RPC 核验且已达到最终确认的 Trove 成功交易事件，作为索引去重凭证。 / Verified events from successful, finalized Trove transactions, used to deduplicate indexing.';
COMMENT ON COLUMN chain_event.network IS '事件所属 Solana 网络。 / Solana network of the event.';
COMMENT ON COLUMN chain_event.tx_signature IS '链上交易的 Base58 签名。 / Base58-encoded on-chain transaction signature.';
COMMENT ON COLUMN chain_event.event_index IS '交易内从 0 开始的稳定事件序号。 / Stable zero-based event ordinal within the transaction.';
COMMENT ON COLUMN chain_event.program_id IS '发出事件的 Trove 程序公钥。 / Address of the Trove program that emitted the event.';
COMMENT ON COLUMN chain_event.event_type IS '服务端按 Anchor IDL 解码后的事件类型。 / Event type decoded by the service using the Anchor IDL.';
COMMENT ON COLUMN chain_event.slot IS 'RPC 返回的区块槽编号。 / Slot number reported by RPC.';
COMMENT ON COLUMN chain_event.block_time IS 'RPC 提供的链上时间，未返回时为空。 / On-chain time from RPC, null when unavailable.';
COMMENT ON COLUMN chain_event.payload IS '仅包含已解码且允许保存的公开业务字段。 / Allowlisted public business fields decoded from the event.';
COMMENT ON COLUMN chain_event.indexed_at IS '事件首次写入数据库的时间。 / Time the event was first recorded in the database.';

CREATE TABLE chain_sync_cursor (
    network varchar(8) NOT NULL CHECK (network IN ('localnet', 'devnet')),
    program_id varchar(44) NOT NULL CHECK (program_id ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    last_tx_signature varchar(88) CHECK (last_tx_signature ~ '^[1-9A-HJ-NP-Za-km-z]{64,88}$'),
    last_slot numeric(20, 0) CHECK (last_slot BETWEEN 0 AND 18446744073709551615),
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (network, program_id),
    CHECK ((last_tx_signature IS NULL) = (last_slot IS NULL))
);

COMMENT ON TABLE chain_sync_cursor IS '按网络和程序分开的最终确认交易连续扫描进度。 / Per-network and per-program checkpoint of the contiguous finalized transaction scan.';
COMMENT ON COLUMN chain_sync_cursor.network IS '扫描任务对应的 Solana 网络。 / Solana network scanned by this task.';
COMMENT ON COLUMN chain_sync_cursor.program_id IS '扫描交易时使用的 Trove 程序公钥。 / Trove program address used in transaction scans.';
COMMENT ON COLUMN chain_sync_cursor.last_tx_signature IS '最后一笔已连续处理的交易签名，尚未建立扫描进度时为空。 / Last transaction signature processed in contiguous scan order, null before a checkpoint exists.';
COMMENT ON COLUMN chain_sync_cursor.last_slot IS '扫描断点交易所在区块槽，无断点时为空。 / Slot of the checkpoint transaction, null before a checkpoint exists.';
COMMENT ON COLUMN chain_sync_cursor.updated_at IS '最近更新扫描进度的时间。 / Time the scan checkpoint was last updated.';

CREATE TABLE chain_asset (
    network varchar(8) NOT NULL CHECK (network IN ('localnet', 'devnet')),
    item_id uuid NOT NULL REFERENCES custody_item (item_id),
    mint_address varchar(44) NOT NULL CHECK (mint_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    item_record_address varchar(44) NOT NULL CHECK (item_record_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    collection_mint_address varchar(44) NOT NULL CHECK (collection_mint_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    minted_to_wallet varchar(44) NOT NULL CHECK (minted_to_wallet ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    mint_tx_signature varchar(88) NOT NULL,
    mint_event_index integer NOT NULL,
    PRIMARY KEY (network, item_id),
    UNIQUE (item_id),
    UNIQUE (network, mint_address),
    UNIQUE (network, item_id, mint_address),
    UNIQUE (network, item_record_address),
    UNIQUE (network, mint_tx_signature, mint_event_index),
    FOREIGN KEY (network, mint_tx_signature, mint_event_index)
        REFERENCES chain_event (network, tx_signature, event_index)
);

COMMENT ON TABLE chain_asset IS '最终确认后形成的 Trove 实物与 NFT 的链上绑定投影，烧毁 NFT 后仍保留。 / Finalized projection of a Trove physical item NFT binding, retained after the NFT is burned.';
COMMENT ON COLUMN chain_asset.network IS '该 NFT 所属的 Solana 网络。 / Solana network of the NFT.';
COMMENT ON COLUMN chain_asset.item_id IS '仓库为实物登记的稳定 UUID。 / Stable UUID assigned to the physical item by the warehouse.';
COMMENT ON COLUMN chain_asset.mint_address IS 'NFT 的 Mint 公钥，在所属网络唯一。 / NFT mint address, unique within its network.';
COMMENT ON COLUMN chain_asset.item_record_address IS '记录实物和 Mint 绑定关系的 Trove ItemRecord PDA。 / Trove ItemRecord PDA that binds the physical item to the mint.';
COMMENT ON COLUMN chain_asset.collection_mint_address IS '铸造登记时验证通过的 Trove 集合 Mint 公钥。 / Trove collection mint address verified at registration and minting.';
COMMENT ON COLUMN chain_asset.minted_to_wallet IS '首次铸造的接收钱包公钥，用于追踪铸造来源。 / Wallet address that received the NFT at mint time, retained for provenance.';
COMMENT ON COLUMN chain_asset.mint_tx_signature IS '创建绑定的最终确认铸造交易签名。 / Finalized mint transaction signature that established the binding.';
COMMENT ON COLUMN chain_asset.mint_event_index IS '该铸造交易内的事件序号，从 0 开始。 / Zero-based ordinal of the mint event within the transaction.';

CREATE TABLE market_listing (
    network varchar(8) NOT NULL,
    item_id uuid NOT NULL,
    listing_no numeric(20, 0) NOT NULL CHECK (listing_no BETWEEN 1 AND 18446744073709551615),
    listing_address varchar(44) NOT NULL CHECK (listing_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    seller_wallet varchar(44) NOT NULL CHECK (seller_wallet ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    escrow_token_account varchar(44) NOT NULL CHECK (escrow_token_account ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    revision numeric(20, 0) NOT NULL CHECK (revision BETWEEN 0 AND 18446744073709551615),
    price_lamports numeric(20, 0) NOT NULL CHECK (price_lamports BETWEEN 1 AND 18446744073709551615),
    status varchar(12) NOT NULL CHECK (status IN ('ACTIVE', 'SOLD', 'CANCELLED')),
    open_tx_signature varchar(88) NOT NULL,
    open_event_index integer NOT NULL,
    last_tx_signature varchar(88) NOT NULL,
    last_event_index integer NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (network, item_id, listing_no),
    FOREIGN KEY (network, item_id) REFERENCES chain_asset (network, item_id),
    FOREIGN KEY (network, open_tx_signature, open_event_index)
        REFERENCES chain_event (network, tx_signature, event_index),
    FOREIGN KEY (network, last_tx_signature, last_event_index)
        REFERENCES chain_event (network, tx_signature, event_index)
);

-- 历史挂单可以共存，有效挂单在同一网络和藏品下只有一条。
-- Retain listing history while allowing one active listing per network and item.
CREATE UNIQUE INDEX ux_market_listing_active_item ON market_listing (network, item_id)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_market_listing_active_seller ON market_listing (network, seller_wallet, updated_at DESC)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_market_listing_active_newest ON market_listing (network, updated_at DESC)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE market_listing IS '链上固定价托管挂单的查询投影，更新报价会推进同一挂单的版本。 / Query projection of fixed-price on-chain escrow listings, with repricing advancing the version of the same listing.';
COMMENT ON COLUMN market_listing.network IS '该挂单所属的 Solana 网络。 / Solana network of the listing.';
COMMENT ON COLUMN market_listing.item_id IS '对应链上藏品的实物 UUID。 / Physical item UUID of the listed asset.';
COMMENT ON COLUMN market_listing.listing_no IS '链上分配的正整数挂单编号，重新挂售时使用新编号。 / Positive on-chain listing number, renewed when the item is relisted.';
COMMENT ON COLUMN market_listing.listing_address IS '该挂单对应的链上 Listing PDA。 / On-chain Listing PDA for this listing.';
COMMENT ON COLUMN market_listing.seller_wallet IS '链上挂单记录的卖家钱包公钥。 / Seller wallet address recorded on-chain.';
COMMENT ON COLUMN market_listing.escrow_token_account IS '挂单期间托管 NFT 的链上代币账户公钥。 / Address of the on-chain token account that holds the NFT in escrow while listed.';
COMMENT ON COLUMN market_listing.revision IS '当前已同步的链上挂单版本，用于识别旧报价。 / Indexed on-chain listing version used to identify stale quotes.';
COMMENT ON COLUMN market_listing.price_lamports IS '挂单固定报价，以整数 lamports 计。 / Fixed asking price in integer lamports.';
COMMENT ON COLUMN market_listing.status IS '挂单状态为有效、已售出或已撤销。 / Listing status is ACTIVE, SOLD, or CANCELLED.';
COMMENT ON COLUMN market_listing.open_tx_signature IS '创建该挂单的最终确认交易签名。 / Finalized transaction signature that opened the listing.';
COMMENT ON COLUMN market_listing.open_event_index IS '创建挂单的交易内事件序号，从 0 开始。 / Zero-based event ordinal of the listing creation within its transaction.';
COMMENT ON COLUMN market_listing.last_tx_signature IS '最近一次应用到该挂单的最终确认交易签名。 / Most recent finalized transaction signature applied to this listing.';
COMMENT ON COLUMN market_listing.last_event_index IS '最近一次应用到该挂单的交易内事件序号，从 0 开始。 / Zero-based ordinal of the most recent event applied to this listing within its transaction.';
COMMENT ON COLUMN market_listing.updated_at IS '最近应用链上变更的数据库时间。 / Database time at which the latest on-chain change was applied.';

CREATE TABLE market_sale (
    network varchar(8) NOT NULL,
    item_id uuid NOT NULL,
    listing_no numeric(20, 0) NOT NULL,
    sale_no numeric(20, 0) NOT NULL CHECK (sale_no BETWEEN 1 AND 18446744073709551615),
    sale_type varchar(12) GENERATED ALWAYS AS (
        CASE WHEN sale_no = 1 THEN 'PRIMARY' ELSE 'RESALE' END
    ) STORED,
    listing_revision numeric(20, 0) NOT NULL CHECK (listing_revision BETWEEN 0 AND 18446744073709551615),
    seller_wallet varchar(44) NOT NULL CHECK (seller_wallet ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    buyer_wallet varchar(44) NOT NULL CHECK (buyer_wallet ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$'),
    price_lamports numeric(20, 0) NOT NULL CHECK (price_lamports BETWEEN 1 AND 18446744073709551615),
    tx_signature varchar(88) NOT NULL,
    event_index integer NOT NULL,
    PRIMARY KEY (network, item_id, listing_no),
    UNIQUE (network, item_id, sale_no),
    UNIQUE (network, tx_signature, event_index),
    FOREIGN KEY (network, item_id, listing_no) REFERENCES market_listing (network, item_id, listing_no),
    FOREIGN KEY (network, tx_signature, event_index) REFERENCES chain_event (network, tx_signature, event_index),
    CHECK (buyer_wallet <> seller_wallet)
);

-- 成交买卖双方可以是从未登录过 Trove 的链上地址。
-- On-chain buyers and sellers may never have logged in to Trove.
CREATE INDEX ix_market_sale_buyer ON market_sale (network, buyer_wallet);
CREATE INDEX ix_market_sale_seller ON market_sale (network, seller_wallet);

COMMENT ON TABLE market_sale IS '经最终确认的 Trove 成交记录，每笔挂单最多对应一次成交。 / Finalized Trove sales, with at most one sale for each listing.';
COMMENT ON COLUMN market_sale.network IS '成交所属的 Solana 网络。 / Solana network of the sale.';
COMMENT ON COLUMN market_sale.item_id IS '被购买藏品的实物 UUID。 / Physical item UUID of the purchased asset.';
COMMENT ON COLUMN market_sale.listing_no IS '本次交易成交并关闭的挂单编号。 / Listing number filled and closed by this trade.';
COMMENT ON COLUMN market_sale.sale_no IS 'Trove 程序记录的该藏品成交序号，从 1 开始。 / One-based sale sequence for this item recorded by the Trove program.';
COMMENT ON COLUMN market_sale.sale_type IS '由成交序号生成，首次成交为 PRIMARY，其后为 RESALE。 / Derived from the sale sequence, PRIMARY for the first sale and RESALE thereafter.';
COMMENT ON COLUMN market_sale.listing_revision IS '成交时买家确认并由程序校验的挂单版本。 / Listing version accepted by the buyer and verified by the program at purchase.';
COMMENT ON COLUMN market_sale.seller_wallet IS '链上收到成交款的卖家钱包公钥。 / Seller wallet address that received the on-chain payment.';
COMMENT ON COLUMN market_sale.buyer_wallet IS '链上收到 NFT 的买家钱包公钥。 / Buyer wallet address that received the NFT on-chain.';
COMMENT ON COLUMN market_sale.price_lamports IS '成交支付额，以整数 lamports 计，不含网络手续费。 / Purchase amount in integer lamports, excluding network transaction fees.';
COMMENT ON COLUMN market_sale.tx_signature IS '最终确认的成交交易签名。 / Finalized purchase transaction signature.';
COMMENT ON COLUMN market_sale.event_index IS '成交事件在该交易内的序号，从 0 开始。 / Zero-based ordinal of the sale event within the transaction.';

-- 兑换草稿、链上兑换凭证与仓库履约。 / Redemption drafts, on-chain receipts, and warehouse fulfillment.
-- 后端锁定草稿行后才允许进入签名，收件资料仅能在 DRAFT 时修改，签名后保留同一密文。
-- Lock the draft row when entering signing, allow recipient changes only in DRAFT, and keep the same envelope after signing.
-- RPC 确认执行失败才写入 FAILED，块哈希失效且未成交才写入 EXPIRED，超时本身保持 SIGNING。
-- Record FAILED after a definitive RPC execution error, record EXPIRED after blockhash invalidation without success, and keep SIGNING on a timeout alone.

CREATE TABLE redemption_draft (
    draft_id uuid PRIMARY KEY,
    network varchar(8) NOT NULL,
    item_id uuid NOT NULL,
    redeemer_wallet varchar(44) NOT NULL REFERENCES wallet_account (wallet_address),
    status varchar(12) NOT NULL DEFAULT 'DRAFT',
    recipient_ciphertext bytea,
    submitted_tx_signature varchar(88),
    signing_blockhash varchar(44),
    last_valid_block_height numeric(20, 0),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_redemption_draft_asset FOREIGN KEY (network, item_id)
        REFERENCES chain_asset (network, item_id),
    CONSTRAINT uq_redemption_draft_binding UNIQUE (draft_id, network, item_id, redeemer_wallet),
    CONSTRAINT ck_redemption_draft_status CHECK (
        status IN ('DRAFT', 'SIGNING', 'CONFIRMED', 'FAILED', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT ck_redemption_draft_recipient CHECK (
        (recipient_ciphertext IS NULL OR octet_length(recipient_ciphertext) > 0)
        AND (status NOT IN ('DRAFT', 'SIGNING') OR recipient_ciphertext IS NOT NULL)
    ),
    CONSTRAINT ck_redemption_draft_signature CHECK (
        submitted_tx_signature IS NULL OR submitted_tx_signature ~ '^[1-9A-HJ-NP-Za-km-z]{64,88}$'
    ),
    -- 保留签名交易的有效期证据，支持服务重启后核对原交易。 / Preserve transaction expiry evidence for recovery after restart.
    CONSTRAINT ck_redemption_draft_signing_window CHECK (
        (signing_blockhash IS NULL) = (last_valid_block_height IS NULL)
        AND (status <> 'SIGNING' OR signing_blockhash IS NOT NULL)
        AND (signing_blockhash IS NULL OR signing_blockhash ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$')
        AND (last_valid_block_height IS NULL OR last_valid_block_height BETWEEN 0 AND 18446744073709551615)
    ),
    CONSTRAINT ck_redemption_draft_times CHECK (expires_at > created_at AND updated_at >= created_at)
);

-- 同一钱包对同一链上藏品至多有一个可签名草稿。 / Keep at most one live draft for a wallet and its asset.
CREATE UNIQUE INDEX uq_redemption_draft_live ON redemption_draft (network, item_id, redeemer_wallet)
    WHERE status IN ('DRAFT', 'SIGNING');

-- 支持按当前钱包和网络查询最近的兑换进度。 / Support recent redemption queries by network and wallet.
CREATE INDEX idx_redemption_draft_wallet ON redemption_draft (network, redeemer_wallet, created_at DESC);

COMMENT ON TABLE redemption_draft IS '持有人私有兑换草稿及加密收件资料，进入签名后冻结资料。 / Holder-private redemption draft and encrypted shipping details, frozen after entering signing.';
COMMENT ON COLUMN redemption_draft.draft_id IS '服务端生成的随机 UUID，同时作为唯一公开草稿引用。 / Server-generated random UUID also used as the unique public draft reference.';
COMMENT ON COLUMN redemption_draft.network IS '藏品所属的 Solana 网络，由同一藏品外键确定。 / Solana network of the asset, bound by the asset foreign key.';
COMMENT ON COLUMN redemption_draft.item_id IS '要兑换的稳定实体藏品 UUID。 / Stable UUID of the collectible being redeemed.';
COMMENT ON COLUMN redemption_draft.redeemer_wallet IS '提交草稿并签名的持有人钱包公钥。 / Public wallet address of the holder who creates and signs the draft.';
COMMENT ON COLUMN redemption_draft.status IS '草稿状态，含待签名、签名中、确认成功、执行失败、取消和过期。 / Draft status covering editable, signing, confirmed, failed, cancelled, and expired.';
COMMENT ON COLUMN redemption_draft.recipient_ciphertext IS '服务端认证加密封装，内含收件人、电话和地址，含解密所需版本引用，履约及保留要求结束后可清除。 / Authenticated encrypted envelope containing recipient name, phone, and address with key-version information, nullable after fulfillment and retention allow removal.';
COMMENT ON COLUMN redemption_draft.submitted_tx_signature IS '客户端回传的交易签名，仅用于 RPC 查询，可空表示尚未知。 / Client-reported transaction signature used only for RPC lookup, nullable when unknown.';
COMMENT ON COLUMN redemption_draft.signing_blockhash IS '协签交易使用的近期块哈希，未进入签名时可空。 / Recent blockhash of the partially signed transaction, null before signing begins.';
COMMENT ON COLUMN redemption_draft.last_valid_block_height IS '对应块哈希的最后有效区块高度，无符号整数，未进入签名时可空。 / Last valid block height for the blockhash as an unsigned integer, null before signing begins.';
COMMENT ON COLUMN redemption_draft.expires_at IS '草稿审核授权的应用截止时刻，过期签名还需核对链上交易和块哈希。 / Application deadline for draft authorization, with on-chain transaction and blockhash verification still required for signed drafts.';
COMMENT ON COLUMN redemption_draft.created_at IS '创建草稿的数据库时间。 / Database timestamp when the draft was created.';
COMMENT ON COLUMN redemption_draft.updated_at IS '应用最后修改草稿的时间。 / Application timestamp of the latest draft update.';

-- 仅投影通过 RPC 验证的 finalized 成功兑换事件，并校验程序、凭证、草稿、Mint 和持有人。
-- Project only RPC-verified finalized successful redemptions and verify the program, receipt, draft, Mint, and holder.

CREATE TABLE redemption_receipt (
    network varchar(8) NOT NULL,
    item_id uuid NOT NULL,
    draft_id uuid NOT NULL UNIQUE,
    redeemer_wallet varchar(44) NOT NULL,
    mint_address varchar(44) NOT NULL,
    receipt_address varchar(44) NOT NULL,
    tx_signature varchar(88) NOT NULL,
    event_index integer NOT NULL,
    verified_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (network, item_id),
    CONSTRAINT uq_redemption_receipt_address UNIQUE (network, receipt_address),
    CONSTRAINT uq_redemption_receipt_event UNIQUE (network, tx_signature, event_index),
    CONSTRAINT fk_redemption_receipt_draft FOREIGN KEY (draft_id, network, item_id, redeemer_wallet)
        REFERENCES redemption_draft (draft_id, network, item_id, redeemer_wallet),
    CONSTRAINT fk_redemption_receipt_asset FOREIGN KEY (network, item_id, mint_address)
        REFERENCES chain_asset (network, item_id, mint_address),
    CONSTRAINT fk_redemption_receipt_event FOREIGN KEY (network, tx_signature, event_index)
        REFERENCES chain_event (network, tx_signature, event_index),
    CONSTRAINT ck_redemption_receipt_address CHECK (receipt_address ~ '^[1-9A-HJ-NP-Za-km-z]{32,44}$')
);

COMMENT ON TABLE redemption_receipt IS '已最终确认的链上兑换凭证投影，也是创建发货任务的依据。 / Projection of a finalized on-chain redemption receipt and prerequisite for shipment creation.';
COMMENT ON COLUMN redemption_receipt.network IS '兑换发生的 Solana 网络。 / Solana network on which redemption occurred.';
COMMENT ON COLUMN redemption_receipt.item_id IS '已兑换的实体藏品 UUID，每个网络只能登记一次。 / Redeemed physical collectible UUID, recorded only once per network.';
COMMENT ON COLUMN redemption_receipt.draft_id IS '链上凭证包含的随机草稿引用，绑定网络、藏品和兑换人。 / Random public draft reference from the on-chain receipt, bound to the network, asset, and redeemer.';
COMMENT ON COLUMN redemption_receipt.redeemer_wallet IS '链上核验的兑换人钱包公钥，与关联草稿相同。 / On-chain-verified redeemer wallet address matching the associated draft.';
COMMENT ON COLUMN redemption_receipt.mint_address IS '在兑换交易中销毁的 NFT Mint 公钥。 / NFT Mint address burned by the redemption transaction.';
COMMENT ON COLUMN redemption_receipt.receipt_address IS '链上一次性兑换凭证的 PDA 地址。 / PDA address of the on-chain one-time redemption receipt.';
COMMENT ON COLUMN redemption_receipt.tx_signature IS '经验证的兑换交易签名。 / Verified redemption transaction signature.';
COMMENT ON COLUMN redemption_receipt.event_index IS '指定交易中兑换事件的确定性序号。 / Deterministic index of the redemption event within the transaction.';
COMMENT ON COLUMN redemption_receipt.verified_at IS '后端确认并持久化最终兑换结果的时间。 / Timestamp when the backend verified and persisted the finalized redemption.';

-- 每张已确认凭证只生成一笔发货任务。 / Create only one shipment task per finalized receipt.
CREATE TABLE shipment (
    shipment_id uuid PRIMARY KEY,
    network varchar(8) NOT NULL,
    item_id uuid NOT NULL,
    status varchar(12) NOT NULL DEFAULT 'READY',
    carrier_code varchar(32),
    tracking_number varchar(128),
    shipped_at timestamptz,
    delivered_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_shipment_receipt UNIQUE (network, item_id),
    CONSTRAINT fk_shipment_receipt FOREIGN KEY (network, item_id)
        REFERENCES redemption_receipt (network, item_id),
    CONSTRAINT ck_shipment_status CHECK (
        (status = 'READY' AND shipped_at IS NULL AND delivered_at IS NULL)
        OR (status = 'SHIPPED' AND shipped_at IS NOT NULL AND delivered_at IS NULL)
        OR (status = 'DELIVERED' AND shipped_at IS NOT NULL AND delivered_at IS NOT NULL)
        OR (status = 'EXCEPTION' AND delivered_at IS NULL)
    ),
    CONSTRAINT ck_shipment_tracking CHECK (
        (carrier_code IS NULL) = (tracking_number IS NULL)
        AND (carrier_code IS NULL OR (btrim(carrier_code) <> '' AND btrim(tracking_number) <> ''))
        AND (shipped_at IS NULL OR tracking_number IS NOT NULL)
    ),
    CONSTRAINT ck_shipment_times CHECK (
        updated_at >= created_at
        AND (shipped_at IS NULL OR shipped_at >= created_at)
        AND (delivered_at IS NULL OR delivered_at >= shipped_at)
    )
);

-- 待办列表按网络、履约状态和创建时间筛选。 / Query fulfillment work queues by network, status, and creation time.
CREATE INDEX idx_shipment_queue ON shipment (network, status, created_at);

COMMENT ON TABLE shipment IS '已最终确认兑换的私有发货任务，收件资料通过凭证回查草稿。 / Private shipment task for a finalized redemption, with recipient details resolved through the receipt and draft.';
COMMENT ON COLUMN shipment.shipment_id IS '服务端生成的发货任务 UUID。 / Server-generated UUID of the shipment task.';
COMMENT ON COLUMN shipment.network IS '关联兑换凭证的 Solana 网络。 / Solana network of the linked redemption receipt.';
COMMENT ON COLUMN shipment.item_id IS '关联兑换凭证的实体藏品 UUID。 / Physical collectible UUID of the linked redemption receipt.';
COMMENT ON COLUMN shipment.status IS '履约当前状态，含待发货、已发货、已签收和异常。 / Current fulfillment status covering ready, shipped, delivered, and exception.';
COMMENT ON COLUMN shipment.carrier_code IS '承运商代码，模拟发货可使用演示代码，未登记时为空。 / Carrier code, including a demo code for simulated shipping, nullable before registration.';
COMMENT ON COLUMN shipment.tracking_number IS '私有物流运单号，未登记时为空。 / Private shipment tracking number, nullable before registration.';
COMMENT ON COLUMN shipment.shipped_at IS '仓库登记实物交付物流的时间，发出前为空。 / Time the warehouse recorded release to the carrier, nullable before shipment.';
COMMENT ON COLUMN shipment.delivered_at IS '登记签收的时间，签收前为空。 / Recorded delivery timestamp, nullable before delivery.';
COMMENT ON COLUMN shipment.created_at IS '发货任务创建时间。 / Shipment task creation timestamp.';
COMMENT ON COLUMN shipment.updated_at IS '应用最后更新发货任务的时间。 / Application timestamp of the latest shipment task update.';

-- 履约状态与时间线在同一事务内写入，事件键须跨重试保持一致。
-- Commit fulfillment status and its timeline in one transaction and keep the event key stable across retries.

CREATE TABLE shipment_event (
    shipment_id uuid NOT NULL REFERENCES shipment (shipment_id),
    event_key varchar(128) NOT NULL,
    event_type varchar(20) NOT NULL,
    actor_wallet varchar(44) REFERENCES wallet_account (wallet_address),
    reason_code varchar(64),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (shipment_id, event_key),
    CONSTRAINT ck_shipment_event_key CHECK (btrim(event_key) <> ''),
    CONSTRAINT ck_shipment_event_type CHECK (
        event_type IN ('READY', 'SHIPPED', 'DELIVERED', 'EXCEPTION', 'EXCEPTION_RESOLVED')
    ),
    CONSTRAINT ck_shipment_event_reason CHECK (
        (reason_code IS NULL OR btrim(reason_code) <> '')
        AND (event_type <> 'EXCEPTION' OR reason_code IS NOT NULL)
    )
);

-- 查询发货任务时间线，事件键作为同一时刻的稳定排序项。
-- Query a shipment timeline with the event key as a stable tie breaker for equal timestamps.
CREATE INDEX idx_shipment_event_timeline ON shipment_event (shipment_id, occurred_at, event_key);

COMMENT ON TABLE shipment_event IS '单笔发货任务的私有履约时间线，按事件键去重。 / Private fulfillment timeline for a shipment, deduplicated by event key.';
COMMENT ON COLUMN shipment_event.shipment_id IS '发货任务 UUID，对应当前履约状态。 / Shipment task UUID linking to the current fulfillment state.';
COMMENT ON COLUMN shipment_event.event_key IS '同任务内跨重试稳定的事件幂等键。 / Idempotency key that remains stable across retries for the same shipment.';
COMMENT ON COLUMN shipment_event.event_type IS '履约事件类型，含任务创建、发出、签收、异常或异常关闭。 / Fulfillment event type covering task creation, shipping, delivery, exception, or exception resolution.';
COMMENT ON COLUMN shipment_event.actor_wallet IS '操作仓管员的钱包公钥，自动记录的事件可为空。 / Public wallet address of the warehouse operator, nullable for automatic events.';
COMMENT ON COLUMN shipment_event.reason_code IS '异常的非敏感原因代码，普通事件为空。 / Non-sensitive exception reason code, nullable for ordinary events.';
COMMENT ON COLUMN shipment_event.occurred_at IS '履约事件发生时间。 / Time when the fulfillment event occurred.';
