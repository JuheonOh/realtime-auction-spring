import { configureStore } from "@reduxjs/toolkit";
import userReducer from "./User";
import auctionReducer from "./Auction";

const initialState = {};
export default configureStore({
  reducer: {
    user: userReducer,
    auction: auctionReducer,
  },
  devTools: true,
  preloadedState: initialState,
});
