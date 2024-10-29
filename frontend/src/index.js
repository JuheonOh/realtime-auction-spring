import React from "react";
import ReactDOM from "react-dom/client";
import Router from "./config/Router";

import { CookiesProvider } from "react-cookie";
import { Provider } from "react-redux";
import store from "./data/redux/store";
import "./assets/scss/global.scss";

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  // <React.StrictMode>
    <CookiesProvider>
      <Provider store={store}>
        <Router />
      </Provider>
    </CookiesProvider>
  // </React.StrictMode>
);
