import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";

import Layout from "../layouts/Layout";

import NotFoundPage from "../pages/NotFoundPage";

import HomePage from "../pages/HomePage";
import AuctionPage from "../pages/AuctionPage";
import ProfilePage from "../pages/ProfilePage";
import LoginPage from "../pages/LoginPage";
import SignUpPage from "../pages/SignUpPage";
import ContactPage from "../pages/ContactPage";

export default function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />

        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/auction" element={<AuctionPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/contact" element={<ContactPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
