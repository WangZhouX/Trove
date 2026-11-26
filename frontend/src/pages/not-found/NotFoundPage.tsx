import { Link } from "react-router";

export function NotFoundPage() {
  return (
    <section className="page">
      <h1>找不到这个页面</h1>
      <Link to="/market">返回市场</Link>
    </section>
  );
}
