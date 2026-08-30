import Layout from "@layouts/Layout";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { lazy, Suspense } from "react";

import LoadingSpinner from "@components/common/loading/LoadingSpinner";
import ScrollToTop from "@components/common/navigation/ScrollToTop";
import Private from "@layouts/Private";

const AuctionCreatePage = lazy(() => import("@pages/auctions/AuctionCreatePage"));
const AuctionDetailPage = lazy(() => import("@pages/auctions/AuctionDetailPage"));
const AuctionListPage = lazy(() => import("@pages/auctions/AuctionListPage"));
const ContactPage = lazy(() => import("@pages/common/ContactPage"));
const HomePage = lazy(() => import("@pages/common/HomePage"));
const NotFoundPage = lazy(() => import("@pages/common/NotFoundPage"));
const SupportPage = lazy(() => import("@pages/common/SupportPage"));
const LoginPage = lazy(() => import("@pages/user/LoginPage"));
const ProfilePage = lazy(() => import("@pages/user/ProfilePage"));
const SignUpPage = lazy(() => import("@pages/user/SignUpPage"));

export default function Router() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Suspense fallback={<LoadingSpinner isLoading message="페이지" />}>
        <Routes>
          <Route element={<Private />}>
            <Route element={<Layout />}>
              <Route path="/user/profile" element={<ProfilePage />} />
              <Route path="/auctions/new" element={<AuctionCreatePage />} />
            </Route>
          </Route>

          <Route element={<Layout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/auctions" element={<AuctionListPage />} />
            <Route path="/auctions/:auctionId" element={<AuctionDetailPage />} />
            <Route path="/contact" element={<ContactPage />} />
            <Route path="/support" element={<SupportPage />} />
          </Route>

          <Route path="/auth/login" element={<LoginPage />} />
          <Route path="/auth/signup" element={<SignUpPage />} />

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
