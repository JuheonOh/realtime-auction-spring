import React from 'react';
import ReactDOM from 'react-dom/client';
import Router from './config/Router';

import "./scss/global.scss";

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <Router />
  </React.StrictMode>
);
