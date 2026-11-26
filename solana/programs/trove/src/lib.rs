//! Trove 的 Anchor 程序入口。
//! Anchor entry point for the Trove program.

use anchor_lang::prelude::*;

mod instructions;

pub use instructions::*;

// 构建使用的公开程序地址，与 Anchor.toml 中的 Localnet 配置一致。
// Public program address for builds, matching the Localnet configuration in Anchor.toml.
declare_id!("Aksy3SzNJhz65n3UfwJGjPJd2gsTRvuVrKur19ryfEQT");

/// 向客户端公开当前可调用的 Trove 指令。
/// Exposes the currently available Trove instructions to clients.
#[program]
pub mod trove {
    use super::*;

    /// 返回成功，用于验证程序入口和生成的 IDL。
    /// Returns success to verify the program entry point and generated IDL.
    ///
    /// `context` 包含发起调用的钱包签名。
    /// `context` contains the signature of the calling wallet.
    pub fn ping(context: Context<Ping>) -> Result<()> {
        instructions::ping::handle(context)
    }
}
