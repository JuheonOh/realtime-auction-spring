import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Navigate, Outlet } from "react-router-dom";
import { fetchUser } from "../apis/UserAPI";
import LoadingSpinner from "../components/LoadingSpinner";
import { RESET_USER, SET_INFO } from "../redux/store/User";

const Private = () => {
  const dispatch = useDispatch();
  const user = useSelector((state) => state.user);
  const [loading, setLoading] = useState(true);


  useEffect(() => {
    if (user.accessToken) {
      fetchUser()
        .then((res) => {
          dispatch(SET_INFO(res.data));
        })
        .catch((err) => {
          console.log(err);
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [dispatch, user.accessToken]);

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!user.info.id) {
    return <Navigate to="/auth/login" />;
  }

  return <Outlet />;
};

export default Private;
