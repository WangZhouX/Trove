import { Navigate, Route, Routes } from "react-router";
import { CollectionPage } from "../pages/collection/CollectionPage";
import { ConsignPage } from "../pages/consign/ConsignPage";
import { ItemDetailPage } from "../pages/market/ItemDetailPage";
import { MarketPage } from "../pages/market/MarketPage";
import { NotFoundPage } from "../pages/not-found/NotFoundPage";
import { RedemptionPage } from "../pages/redemption/RedemptionPage";
import { WarehousePage } from "../pages/warehouse/WarehousePage";
import { AppLayout } from "./layout/AppLayout";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/market" replace />} />
        <Route path="market" element={<MarketPage />} />
        <Route path="market/:itemId" element={<ItemDetailPage />} />
        <Route path="collection" element={<CollectionPage />} />
        <Route path="consign" element={<ConsignPage />} />
        <Route path="redemption" element={<RedemptionPage />} />
        <Route path="warehouse" element={<WarehousePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
