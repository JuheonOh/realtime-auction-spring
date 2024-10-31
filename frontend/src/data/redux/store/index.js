import auctionReducer from "@data/redux/store/Auction";
import userReducer from "@data/redux/store/User";
import { configureStore } from "@reduxjs/toolkit";

const initialState = {};
export default configureStore({
  reducer: {
    user: userReducer,
    auction: auctionReducer,
  },
  devTools: true,
  preloadedState: initialState,
});
