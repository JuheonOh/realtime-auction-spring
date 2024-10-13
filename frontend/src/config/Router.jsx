import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";

import Layout from "../layouts/Layout";

import HomePage from "../pages/common/HomePage";
import NotFoundPage from "../pages/common/NotFoundPage";
import ContactPage from "../pages/common/ContactPage";

import AuctionPage from "../pages/auction/AuctionPage";
import AuctionDetailPage from "../pages/auction/AuctionDetailPage";
import AuctionAddItemPage from "../pages/auction/AuctionAddItemPage";

import ProfilePage from "../pages/user/ProfilePage";
import LoginPage from "../pages/user/LoginPage";
import SignUpPage from "../pages/user/SignUpPage";

export default function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/signup" element={<SignUpPage />} />

        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/auction" element={<AuctionPage />} />
          <Route path="/auction/:id" element={<AuctionDetailPage />} />
          <Route path="/auction/sell" element={<AuctionAddItemPage />} />
          <Route path="/user/profile" element={<ProfilePage />} />
          <Route path="/contact" element={<ContactPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
