package com.link.cloud.base;/*
 * {EasyGank}  Copyright (C) {2015}  {CaMnter}
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * The hypothetical commands `show w' and `show c' should show the appropriate
 * parts of the General Public License.  Of course, your program's commands
 * might be different; for a GUI interface, you would use an "about box".
 *
 * You should also get your employer (if you work as a programmer) or school,
 * if any, to sign a "copyright disclaimer" for the program, if necessary.
 * For more information on this, and how to apply and follow the GNU GPL, see
 * <http://www.gnu.org/licenses/>.
 *
 * The GNU General Public License does not permit incorporating your program
 * into proprietary programs.  If your program is a subroutine library, you
 * may consider it more useful to permit linking proprietary applications with
 * the library.  If this is what you want to do, use the GNU Lesser General
 * Public License instead of this License.  But first, please read
 * <http://www.gnu.org/philosophy/why-not-lgpl.html>.
 */


import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.tencent.bugly.crashreport.CrashReport;

import java.util.Calendar;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmConfiguration;


/**
 * Description：BaseApplication
 * Created by Shaozy on 2016/8/10.
 */
public class BaseApplication extends Application {
    public static final String ACTION_UPDATEUI = "com.link.cloud.updateTiemStr";
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .name("Lock.realm")
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(configuration);
       // ifspeaking();
        CrashReport.initCrashReport(getApplicationContext(), "62ab7bf668", true);
        mHandler.sendEmptyMessage(1);

    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    mHandler.removeMessages(1);
                    final Intent intent = new Intent();
                    intent.setAction(ACTION_UPDATEUI);
                    intent.putExtra("timeStr",getTime());
                    intent.putExtra("timeData",getData());
                    sendBroadcast(intent);
                    Log.e("handleMessage: ", getTime());
                    mHandler.sendEmptyMessageDelayed(1,1000);
                    break;
                default:
                    break;
            }
        }
    };
    public String getData(){
        String timeStr=null;
        String mMonth=null;
        String mDay=null;
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String mYear = String.valueOf(c.get(Calendar.YEAR)); // 获取当前年份
        if ((c.get(Calendar.MONTH) + 1)<10){
            mMonth = "0"+String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份
        }else {
            mMonth = String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份
        }
        if(c.get(Calendar.DAY_OF_MONTH)<10){
            mDay = "0"+String.valueOf(c.get(Calendar.DAY_OF_MONTH));// 获取当前月份的日期号码
        }else {
            mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));// 获取当前月份的日期号码
        }
        return mYear+"-"+mMonth + "-" + mDay;
    }
    public String getTime(){
        String timeStr=null;
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        int mtime=c.get(Calendar.HOUR_OF_DAY);
        int mHour = c.get(Calendar.HOUR);//时
        int mMinute = c.get(Calendar.MINUTE);//分
        int seconds=c.get(Calendar.SECOND);
        if (mtime>=0&&mtime<=5){
            timeStr="凌晨";
        }else if (mtime>5&&mtime<8){
            timeStr="早晨";
        }else if(mtime>8&&mtime<12){
            timeStr="上午";
        }else if(mtime>=12&&mtime<14){
            timeStr="中午";
        }else if(mtime>=14&&mtime<18){
            timeStr="下午";
        }else if(mtime>=18&&mtime<19){
            timeStr="傍晚";
        }else if(mtime>=19&&mtime<=22){
            timeStr="晚上";
        }else if(mtime>22){
            timeStr="深夜";
        }
        return checknum(mtime)+":"+checknum(mMinute)+":"+checknum(seconds);
    }
    private String checknum(int num){
        String strnum=null;
        if (num<10){
            strnum="0"+num;
        }else {
            strnum=num+"";
        }
        return strnum;
    }


//    void ifspeaking(){
//        StringBuffer param = new StringBuffer();
//        param.append("appid="+getString(R.string.app_id));
//        param.append(",");
//        // 设置使用v5+
//        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
//        SpeechUtility.createUtility(BaseApplication.this, param.toString());
//    }

}
