import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";

import Layout from "../layouts/Layout";

import ContactPage from "../pages/common/ContactPage";
import HomePage from "../pages/common/HomePage";
import NotFoundPage from "../pages/common/NotFoundPage";

import AuctionAddPage from "../pages/auction/AuctionAddPage";
import AuctionDetailPage from "../pages/auction/AuctionDetailPage";
import AuctionPage from "../pages/auction/AuctionPage";

import LoginPage from "../pages/user/LoginPage";
import ProfilePage from "../pages/user/ProfilePage";
import SignUpPage from "../pages/user/SignUpPage";
import Private from "./Private";

export default function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Private />}>
          <Route element={<Layout />}>
            <Route path="/user/profile" element={<ProfilePage />} />
            <Route path="/auction/sell" element={<AuctionAddPage />} />
          </Route>
        </Route>

        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/auction" element={<AuctionPage />} />
          <Route path="/auction/:id" element={<AuctionDetailPage />} />
          <Route path="/contact" element={<ContactPage />} />
        </Route>

        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/signup" element={<SignUpPage />} />

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
