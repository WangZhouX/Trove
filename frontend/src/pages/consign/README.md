# 寄存页面 / Consignment Page

`/consign` 使用当前 Phantom 签名会话提交卡牌或潮玩寄存，筛选本人记录，并展开一笔申请查看收货、验收和退件信息。验收后详情显示后端分配的实物编号。

`/consign` uses the current signed-in Phantom session to submit cards or toys, filter the depositor's records, and review the receipt, inspection, and return details. Approved records show the assigned custody item ID.

提交失败后，未改动的表单用同一请求编号重试。退出或切换钱包会取消旧页面的请求，由新会话重新加载本人记录。

An unchanged form reuses its request UUID after a failed submission. Sign-out or a wallet switch cancels the old page's requests and loads the depositor's records under the new session.
