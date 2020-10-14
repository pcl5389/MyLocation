package com.juntai.mylocation.ui.login;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;

import com.juntai.mylocation.R;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.tencent.map.geolocation.TencentLocationRequest.REQUEST_LEVEL_GEO;

public class LocService extends Service {
    /** 标识服务如果被杀死之后的行为 */
    static final int LOCATION_RATE=120000;
    int mStartMode = START_STICKY;
    static boolean bConnected=false;
    static Socket socket;
    private static final String IP="106.13.178.13";
    private static final int PORT = 9527;
    private static Timer timer=null;
    public static  String mac="";
    static String last_loc_time="";
    static String _last_loc_time="";
    NotificationManager notificationManager=null;
    boolean isCreateChannel=false;
    String NOTIFICATION_CHANNEL_NAME="获取位置";

    static BigDecimal lat= BigDecimal.valueOf(0);
    static BigDecimal lng= BigDecimal.valueOf(0);
    static String addr="";
    static String area="";

    static TencentLocationManager mLocationManager=null;
    static InnerLocationListener mLocationListener;
    static TencentLocationRequest request;

    /** 绑定的客户端接口 */
    IBinder mBinder;
    /** 标识是否可以使用onRebind */
    boolean mAllowRebind;
    /** 当服务被创建时调用. */
    @Override
    public void onCreate() {
        super.onCreate();
        //连接服务器
        if(mLocationManager==null) {
            mLocationManager = TencentLocationManager.getInstance(this);
            mLocationListener = new InnerLocationListener();
            request = TencentLocationRequest.create();
            request.setAllowCache(false);
            request.setInterval(LOCATION_RATE / 2);
            request.setAllowGPS(true);
            request.setRequestLevel(REQUEST_LEVEL_GEO);
            request.setAllowDirection(false);

            mLocationManager.enableForegroundLocation(2020, buildNotification());
            mLocationManager.requestLocationUpdates(request, mLocationListener, Looper.getMainLooper());

            Send("Hello!" + mac);
        }
        //启动定时器
        if(timer==null)
        {
            start_timer();
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = null;
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (notificationManager == null) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            String channelId = getPackageName();
            if (!isCreateChannel) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId,
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
                .setContentText("正在定位")
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setWhen(System.currentTimeMillis());

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        return notification;
    }

    public static void Send(String content)
    {
        content=content+"\0";
        Runnable networkTask = new MyRunnable(content) {
            @Override
            public void run() {
                if(bConnected)
                {
                    try
                    {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(Content.getBytes("UTF-8"));
                        outputStream.flush();
                    }
                    catch (IOException e)
                    {
                        bConnected=false;
                    }
                }
                else
                {
                    try{
                        socket=new Socket(IP,PORT);
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(("Hello!"+ mac+"\0"+Content).getBytes("UTF-8"));
                        outputStream.flush();
                        bConnected=true;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(networkTask, content).start();
    }
    /**
     * 网络操作相关的子线程
     */
    public static void start_timer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                writeLogs("请求定位");
                send_location();
            }
        }, 1000, LOCATION_RATE);
    }

    /** 调用startService()启动服务时回调 */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onCreate();
        return mStartMode;
    }

    /** 通过bindService()绑定到服务的客户端 */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** 通过unbindService()解除所有客户端绑定时调用 */
    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    /** 通过bindService()将客户端绑定到服务时调用*/
    @Override
    public void onRebind(Intent intent) {
    }
    /** 服务不再有用且将要被销毁时调用 */
    @Override
    public void onDestroy() {
        //断开服务器连接
        if(bConnected)
        {
            try
            {
                socket.close();
            }
           catch (Exception e){}

            mLocationManager=null;
            bConnected=false;
        }
        //关闭计时器
        if(timer!=null)
        {
            timer.cancel();
            timer=null;
        }
    }
    public static class MyRunnable implements Runnable {
        String Content="";
        public MyRunnable(String parameter) {
            Content=parameter;
        }
        public void run() {}
    }

    public static void writeLogs(String str) {
        String filePath = null;
        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (hasSDCard) {
            filePath =Environment.getExternalStorageDirectory().toString() + File.separator +"hello.txt";
        } else
            filePath =Environment.getDownloadCacheDirectory().toString() + File.separator +"hello.txt";
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());

            Date day=new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            raf.write((df.format(day)+":"+str + "\r\n").getBytes());
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void send_location()
    {
        if(_last_loc_time!=last_loc_time)
        {
            _last_loc_time=last_loc_time;
            String my_location=String.format("%s|%s|%s|%s|%s", lat, lng, addr, area, last_loc_time);
            LocService.Send(my_location);
        }
    }

    public static class InnerLocationListener implements TencentLocationListener {
        private WeakReference<LoginActivity> mMainActivityWRF;
        public InnerLocationListener() {}
        @Override
        public void onLocationChanged(TencentLocation _loc, int error, String reason) {
            last_loc_time=(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
            if(lat!=BigDecimal.valueOf(_loc.getLatitude()) || lng!=BigDecimal.valueOf(_loc.getLongitude()))
            {
                lat=BigDecimal.valueOf(_loc.getLatitude());
                lng=BigDecimal.valueOf(_loc.getLongitude());
                addr=_loc.getAddress();
                area=_loc.getName();
                writeLogs("->定位成功！"+ String.format("%s|%s|%s|%s", lat, lng, addr, area));
            }
            else
            {
                writeLogs("->定位成功！位置同上");
            }
        }
        @Override
        public void onStatusUpdate(String name, int status, String desc) {
            writeLogs("StatusUpdate:"+ "name: " + name + "status: " + status + "desc: " + desc);
        }
    }
}
