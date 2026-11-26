use anchor_lang::prelude::*;

/// 连通性指令所需的账户。
/// Accounts required for the connectivity instruction.
#[derive(Accounts)]
pub struct Ping<'info> {
    /// 发起指令并签名的钱包。
    /// Wallet that initiates and signs the instruction.
    pub caller: Signer<'info>,
}

/// 在验证调用方签名后写入一条日志，不读写业务账户。
/// Emits a log after caller verification and leaves business accounts untouched.
///
/// `_context` 由 Anchor 验证，处理器无需访问其中的账户。
/// Anchor validates `_context` before this handler runs.
pub fn handle(_context: Context<Ping>) -> Result<()> {
    msg!("Trove program ready");
    Ok(())
}
