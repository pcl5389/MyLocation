package com.juntai.mylocation.ui.login;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.juntai.mylocation.R;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.lang.ref.WeakReference;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.tencent.map.geolocation.TencentLocationRequest.REQUEST_LEVEL_GEO;

public class LoginActivity extends AppCompatActivity {
    private static final int LOC_NOTIFICATIONID = 2020;
    static TencentLocationManager mLocationManager;
    static InnerLocationListener mLocationListener;
    static TencentLocationRequest request;
    NotificationManager notificationManager=null;
    public static String mac="";
    String TAG="Login";

    static double lat=0;
    static double lng=0;
    static String addr="";
    static String area="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        if (Build.VERSION.SDK_INT >= 23) {
            requirePermission();
        }
        mac= LocService.mac=getMacAddress();

        mLocationManager= TencentLocationManager.getInstance(this);
        mLocationListener = new InnerLocationListener(new WeakReference<LoginActivity>(this));
        request= TencentLocationRequest.create();
        request.setAllowGPS(true);
        request.setRequestLevel(REQUEST_LEVEL_GEO);
        request.setAllowDirection(false);
        mLocationManager.enableForegroundLocation(LOC_NOTIFICATIONID, buildNotification());

        Intent intentOne = new Intent(this, LocService.class);
        intentOne.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intentOne);

        final EditText usernameEditText = findViewById(R.id.username);
        usernameEditText.setText(mac);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    requirePermission();
                }
                loadingProgressBar.setVisibility(View.VISIBLE);
                mLocationManager.requestSingleFreshLocation(request, mLocationListener, Looper.getMainLooper());

                String my_location=String.format("%s|%s|%s|%s", lat, lng, addr, area);
                Toast.makeText(getApplicationContext(), my_location, Toast.LENGTH_LONG).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }
    boolean isCreateChannel=false;
    String NOTIFICATION_CHANNEL_NAME="获取位置";

    private Notification buildNotification() {
        Notification.Builder builder = null;
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (notificationManager == null) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            String channelId = getPackageName();
            if (!isCreateChannel) {NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableLights(true);//是否在桌面icon右上角展示小圆点
                notificationChannel.setLightColor(Color.BLUE); //小圆点颜色
                notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                notificationManager.createNotificationChannel(notificationChannel);
                isCreateChannel = true;
            }
            builder = new Notification.Builder(getApplicationContext(), channelId);
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }

        builder.setSmallIcon(R.mipmap.ic_launcher_small)
                .setContentTitle("定位中")
                .setContentText("正在定位");
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                //.setWhen(System.currentTimeMillis());

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        return notification;
    }

    public static void send_location()
    {
        mLocationManager.requestLocationUpdates(request, mLocationListener, Looper.getMainLooper());
    }

    @AfterPermissionGranted(1)
    private void requirePermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        String[] permissionsForQ = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION, //target为Q时，动态请求后台定位权限
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (Build.VERSION.SDK_INT >= 29 ? EasyPermissions.hasPermissions(this, permissionsForQ) :
                EasyPermissions.hasPermissions(this, permissions)) {
            //Toast.makeText(this, "权限OK", Toast.LENGTH_LONG).show();
        } else {
            EasyPermissions.requestPermissions(this, "需要权限",
                    1, Build.VERSION.SDK_INT >= 29 ? permissionsForQ : permissions);
        }
    }

    private static class InnerLocationListener implements TencentLocationListener {
        private WeakReference<LoginActivity> mMainActivityWRF;
        public InnerLocationListener(WeakReference<LoginActivity> mainActivityWRF) {
            mMainActivityWRF = mainActivityWRF;
        }
        @Override
        public void onLocationChanged(TencentLocation _loc, int error, String reason) {
            if(_loc.getLatitude()>1)
                mLocationManager.removeUpdates(mLocationListener);
            if(lat!=_loc.getLatitude() || lng!=_loc.getLongitude())
            {
                String my_location=String.format("%s|%s|%s|%s",
                        lat=_loc.getLatitude(),
                        lng=_loc.getLongitude(),
                        addr=_loc.getAddress(),
                        area=_loc.getName());
                LocService.Send(my_location);
            }
            else
                LocService.Send("==");
        }
        @Override
        public void onStatusUpdate(String name, int status, String desc) {
            Log.i("Log", "name: " + name + "status: " + status + "desc: " + desc);
        }
    }
    public String getMacAddress() {
        String macAddress = null;
        WifiManager wifiManager =
                (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = ( null == wifiManager ? null : wifiManager.getConnectionInfo());
        if (!wifiManager.isWifiEnabled())
        {
            //必须先打开，才能获取到MAC地址
            wifiManager.setWifiEnabled( true );
            wifiManager.setWifiEnabled( false );
        }
        if ( null != info) {
            macAddress = info.getMacAddress();
        }
        return macAddress;
    }
}


