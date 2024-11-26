import { getUser } from "@apis/UserAPI";
import LoadingSpinner from "@components/common/loading/LoadingSpinner";
import { SET_INFO } from "@data/redux/store/User";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Navigate, Outlet } from "react-router-dom";

export default function Private() {
  const dispatch = useDispatch();
  const user = useSelector((state) => state.user);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      if (!user.accessToken) return setLoading(false);

      try {
        const res = await getUser();
        dispatch(SET_INFO(res.data));
      } catch (err) {
        console.log(err);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [dispatch, user.accessToken]);

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!user.info.id) {
    return <Navigate to="/auth/login" />;
  }

  return <Outlet />;
}
