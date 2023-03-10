package unic.cicoco.cordova.amap;

import android.Manifest;
import android.content.pm.PackageManager;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Location extends CordovaPlugin implements AMapLocationListener {

    private static final String TAG = "GeolocationPlugin";
    private static final String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private boolean keepSendBack = false;
    private CallbackContext callback;

    public static final int GPS_REQUEST_CODE = 100001;

    public static final int PERMISSION_DENIED_ERROR = 20;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("getCurrentLocation")) {
            callback = callbackContext;
            getCurrentLocation();
            return true;
        } else {
            return false;
        }
    }

    private void getCurrentLocation() {
        boolean hasGpsLocationPermission = hasPermisssion();
        if (!hasGpsLocationPermission) {
            requestPermissions(GPS_REQUEST_CODE);
            return;
        }

        AMapLocationClient.updatePrivacyAgree(this.cordova.getActivity().getApplicationContext(), true);
        AMapLocationClient.updatePrivacyShow(this.cordova.getActivity().getApplicationContext(), true, true);

        try {
            locationClient = new AMapLocationClient(this.cordova.getActivity().getApplicationContext());
        } catch (Exception e) {
            callback.error("init locationClient failed.");
            return;
        }

        locationOption = new AMapLocationClientOption();
        // ????????????????????????????????????
        locationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        //?????????????????????
        locationOption.setOnceLocation(true);
        // ??????????????????
        locationClient.setLocationListener(this);
        locationOption.setNeedAddress(true);
        locationOption.setInterval(2000);

        locationClient.setLocationOption(locationOption);
        // ????????????
        locationClient.startLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//????????????

                JSONObject locationInfo = new JSONObject();
                try {
                    locationInfo.put("locationType", aMapLocation.getLocationType()); //??????????????????????????????????????????????????????????????????????????????
                    locationInfo.put("latitude", aMapLocation.getLatitude()); //????????????
                    locationInfo.put("longitude", aMapLocation.getLongitude()); //????????????
                    locationInfo.put("accuracy", aMapLocation.getAccuracy()); //??????????????????
                    locationInfo.put("speed", aMapLocation.getSpeed()); //??????????????????
                    locationInfo.put("bearing", aMapLocation.getBearing()); //??????????????????
                    locationInfo.put("date", date); //????????????
                    locationInfo.put("address", aMapLocation.getAddress()); //???????????????option?????????isNeedAddress???false?????????????????????
                    locationInfo.put("country", aMapLocation.getCountry()); //????????????
                    locationInfo.put("province", aMapLocation.getProvince()); //?????????
                    locationInfo.put("city", aMapLocation.getCity()); //????????????
                    locationInfo.put("district", aMapLocation.getDistrict()); //????????????
                    locationInfo.put("street", aMapLocation.getStreet()); //????????????
                    locationInfo.put("streetNum", aMapLocation.getStreetNum()); //???????????????
                    locationInfo.put("cityCode", aMapLocation.getCityCode()); //????????????
                    locationInfo.put("adCode", aMapLocation.getAdCode()); //????????????
                    locationInfo.put("poiName", aMapLocation.getPoiName());
                    locationInfo.put("aoiName", aMapLocation.getAoiName());
                } catch (JSONException e) {
                    LOG.e(TAG, "Assemble Location json error:" + e);
                }
                PluginResult result = new PluginResult(PluginResult.Status.OK, locationInfo);
                if (!keepSendBack) { //???????????????????????????
                    locationClient.stopLocation(); //??????????????????????????????
                } else {
                    result.setKeepCallback(true);
                }
                callback.sendPluginResult(result);
            } else {
                LOG.e(TAG, "Get Location error:" + aMapLocation.getErrorCode());
                callback.error(String.format("[%d]%s", aMapLocation.getErrorCode(), aMapLocation.getErrorInfo()));
            }
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case GPS_REQUEST_CODE: {
                getCurrentLocation();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean hasPermisssion() {
        for (String p : permissions) {
            if (!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    /*
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     */
    @Override
    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }
}
