import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';

import Layout from '../layouts/Layout';

import Home from '../pages/Home';
import Auction from '../pages/Auction';
import Profile from '../pages/Profile';

export default function Router(){
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/auction" element={<Auction />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}