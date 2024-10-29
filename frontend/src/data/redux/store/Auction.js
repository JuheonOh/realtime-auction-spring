import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  categoryList: [],
  auctionList: [],
  auctionDetail: null,
};

const auctionSlice = createSlice({
  name: "auction",
  initialState,
  reducers: {
    RESET: () => initialState,
    INIT_CATEGORY_LIST: (state) => {
      state.categoryList = [];
    },
    INIT_AUCTION_LIST: (state) => {
      state.auctionList = [];
    },
    INIT_AUCTION_DETAIL: (state) => {
      state.auctionDetail = null;
    },
    SET_CATEGORY_LIST: (state, action) => {
      state.categoryList = action.payload;
    },
    SET_AUCTION_LIST: (state, action) => {
      state.auctionList = action.payload;
    },
    SET_AUCTION_DETAIL: (state, action) => {
      state.auctionDetail = { ...state.auctionDetail, ...action.payload };
    },
  },
});

export const { INIT_CATEGORY_LIST, INIT_AUCTION_LIST, INIT_AUCTION_DETAIL, SET_CATEGORY_LIST, SET_AUCTION_LIST, SET_AUCTION_DETAIL } = auctionSlice.actions;

export default auctionSlice.reducer;
