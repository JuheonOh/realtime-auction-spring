import Layout from "@layouts/Layout";
import { BrowserRouter, Route, Routes } from "react-router-dom";

import ScrollToTop from "@components/common/navigation/ScrollToTop";
import Private from "@config/Private";
import AuctionCreatePage from "@pages/auctions/AuctionCreatePage";
import AuctionDetailPage from "@pages/auctions/AuctionDetailPage";
import AuctionListPage from "@pages/auctions/AuctionListPage";
import ContactPage from "@pages/common/ContactPage";
import HomePage from "@pages/common/HomePage";
import NotFoundPage from "@pages/common/NotFoundPage";
import SupportPage from "@pages/common/SupportPage";
import LoginPage from "@pages/user/LoginPage";
import ProfilePage from "@pages/user/ProfilePage";
import SignUpPage from "@pages/user/SignUpPage";

export default function Router() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        <Route element={<Private />}>
          <Route element={<Layout />}>
            <Route path="/user/profile" element={<ProfilePage />} />
            <Route path="/auctions/create" element={<AuctionCreatePage />} />
          </Route>
        </Route>

        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/auctions" element={<AuctionListPage />} />
          <Route path="/auctions/:auctionId/" element={<AuctionDetailPage />} />
          <Route path="/contact" element={<ContactPage />} />
          <Route path="/support" element={<SupportPage />} />
        </Route>

        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/signup" element={<SignUpPage />} />

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
