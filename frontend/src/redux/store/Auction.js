import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  categoryList: [],
};

const auctionSlice = createSlice({
  name: "auction",
  initialState,
  reducers: {
    RESET: () => initialState,
    INIT_CATEGORY: (state) => {
      state.categoryList = [];
    },
    SET_CATEGORY: (state, action) => {
      state.categoryList = action.payload;
    },
  },
});

export const { INIT_CATEGORY, SET_CATEGORY } = auctionSlice.actions;

export default auctionSlice.reducer;
