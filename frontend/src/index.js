import Router from "@config/Router";
import React from "react";
import ReactDOM from "react-dom/client";

import "@assets/scss/global.scss";
import store from "@data/redux/store";
import { CookiesProvider } from "react-cookie";
import { Provider } from "react-redux";

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
