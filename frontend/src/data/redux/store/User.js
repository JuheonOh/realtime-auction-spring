import { getCookie } from "@data/storage/Cookie";
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  authenticated: getCookie("accessToken") ? true : false,
  accessToken: getCookie("accessToken") ? getCookie("accessToken") : null,
  info: {},
};

const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    RESET_USER: () => initialState,
    LOGOUT: (state) => {
      state.info = {};
      state.authenticated = false;
      state.accessToken = null;
    },
    INIT_INFO: (state) => {
      state.info = {};
    },
    SET_INFO: (state, action) => {
      state.info = action.payload;
    },
    INIT_ACCESS_TOKEN: (state) => {
      state.authenticated = false;
      state.accessToken = null;
    },
    SET_ACCESS_TOKEN: (state, action) => {
      state.authenticated = true;
      state.accessToken = action.payload;
    },
  },
});

// actions를 export
export const { RESET_USER, LOGOUT, INIT_INFO, SET_INFO, INIT_ACCESS_TOKEN, SET_ACCESS_TOKEN } = userSlice.actions;

// reducer를 default export
export default userSlice.reducer;
