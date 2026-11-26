import { Link, useParams } from "react-router";

export function ItemDetailPage() {
  const { itemId } = useParams<{ itemId: string }>();

  return (
    <section className="page">
      <Link to="/market">返回市场</Link>
      <h1>藏品详情</h1>
      <p>藏品编号 {itemId}。详情和购买功能待接入。</p>
    </section>
  );
}
