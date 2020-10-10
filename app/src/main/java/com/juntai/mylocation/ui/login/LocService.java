package com.juntai.mylocation.ui.login;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class LocService extends Service {
    /** 标识服务如果被杀死之后的行为 */
    int mStartMode = START_STICKY;
    static boolean bConnected=false;
    static Socket socket;
    private static final String IP="106.13.178.13";
    private static final int PORT = 9527;
    private static Timer timer=null;
    /** 绑定的客户端接口 */
    IBinder mBinder;
    /** 标识是否可以使用onRebind */
    boolean mAllowRebind;
    /** 当服务被创建时调用. */
    @Override
    public void onCreate() {
        //连接服务器
        Send("Hello!"+ LoginActivity.mac);
        //启动定时器
        if(timer==null)
        {
            start_timer();
        }
        super.onCreate();
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
                        outputStream.write(Content.getBytes("UTF-8"));
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
                //Date day=new Date();
                //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //System.out.println(df.format(day));
                //Send(df.format(day));
                LoginActivity.send_location();
                //System.err.println("-------延迟5000毫秒，每1000毫秒执行一次--------"+df.format(day));
            }
        }, 1000, 120000);
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
        System.err.println("连接已断开！");
        //断开服务器连接
        if(bConnected)
        {
            try
            {
                socket.close();
            }
           catch (Exception e)
           {}
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
}
