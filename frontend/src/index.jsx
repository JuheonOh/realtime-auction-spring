import Router from "@config/Router";
import ReactDOM from "react-dom/client";

import "@assets/scss/global.scss";
import store from "@data/redux/store";
import { Provider } from "react-redux";

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  <Provider store={store}>
    <Router />
  </Provider>
);
