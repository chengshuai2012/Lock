package com.link.cloud.activity;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.link.cloud.R;
import com.link.cloud.venue.MdUsbService;
import com.link.cloud.venue.ModelImgMng;
import com.link.cloud.venue.PermissionUtils;


public class MainActivity extends Activity {
    private final static String TAG=MainActivity.class.getSimpleName()+"_DEBUG";
    private final static int MSG_SHOW_LOG=0;
    private final static int MSG_SWITCH_POP_CONTENT=6;

    private final static float IDENTIFY_SCORE_THRESHOLD=0.63f;//认证通过的得分阈值，超过此得分才认为认证通过；
    private final static float MODEL_SCORE_THRESHOLD=0.4f;//同一手指第2，3次建模模版与前1，2次的匹配得分阈值，低于此值认为换用了其他手指；
    public final static String TAG_MD_DEVICE="MD_DEVICE";//仅用作传递设备编号的标签；

    private MdUsbService.MyBinder mdDeviceBinder;//对指静脉的所有操作放到服务中进行，界面与指静脉设备之间完全隔离；
    private boolean isMdDebugOpen=false;//打开则记录建模或认证的图片及日志到sd卡，消耗较多资源建议关闭；
    private ServiceConnection mdSrvConn=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mdDeviceBinder=(MdUsbService.MyBinder)service;
            if(mdDeviceBinder!=null){
                mdDeviceBinder.setOnUsbMsgCallback(mdUsbMsgCallback);
                Log.e(TAG,"bind MdUsbService success.");
            }else{
                Log.e(TAG,"bind MdUsbService failed.");
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG,"disconnect MdUsbService.");
        }
    };

    private MdUsbService.UsbMsgCallback mdUsbMsgCallback=new MdUsbService.UsbMsgCallback(){
        @Override
        public void onUsbConnSuccess(String usbManufacturerName, String usbDeviceName) {
            String newUsbInfo="USB厂商："+usbManufacturerName+"  \nUSB节点："+usbDeviceName;
            handler.obtainMessage(MSG_SHOW_LOG,newUsbInfo).sendToTarget();
        }
        @Override
        public void onUsbDisconnect() {
            handler.obtainMessage(MSG_SHOW_LOG,"USB连接已断开").sendToTarget();
            deviceTouchState=2;
            bOpen=false;
        }
    };

    private Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            return false;
        }
    });
    private byte[] img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupExtra();
        findViewById(R.id.identfy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workHandler.sendEmptyMessage(19);
            }
        });
        findViewById(R.id.work).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workHandler.sendEmptyMessage(18);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mdSrvConn);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    //当前app的设备状态标识，0:触摸，1:移开(无触摸)，2:设备断开或其他状态.（注意区分fvDev_get_state()的返回值中,0:表移开，1指腹触碰，2指尖触碰，3:表触摸)
    public int deviceTouchState=1;

    private void setupExtra() {
        Intent intent=new Intent(this,MdUsbService.class);
        if(!bindService(intent,mdSrvConn,Service.BIND_AUTO_CREATE)){
            Log.e(TAG,"bind MdUsbService failed,can't get fingerVein object.");
            handler.removeCallbacksAndMessages(null);
            finish();
        }
        PermissionUtils.verifyStoragePermissions(this);
    }
    private boolean bOpen=false;//设备是否打开
    private Thread mdWorkThread=null;//进行建模或认证的全局工作线程
    private int[] pos=new int[1];
    private float[] score=new float[1];
    private boolean ret;
    private ModelImgMng modelImgMng=new ModelImgMng();
    private int[] tipTimes={0,0};//后两次次建模时用了不同手指或提取特征识别时，最多重复提醒限制3次
    private int lastTouchState=0;//记录上一次的触摸状态
    private int modOkProgress=0;
    Handler workHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 18:
                    int state = getState();
                    if(state==3){
                        workModel();
                    }else if(state==1||state==2){
                        deviceTouchState=1;
                    }else if(state==0){
                        deviceTouchState=1;
                    }
                    if (bOpen){
                        mdDeviceBinder.closeDevice(0);
                        bOpen=false;
                    }
                    break;
                    case 19:
                        int state2 = getState();
                        if(state2==3){
                            identifyModel();
                        }else if(state2==1||state2==2){
                            deviceTouchState=1;
                        }else if(state2==0){
                            deviceTouchState=1;
                        }
                        if (bOpen){
                            mdDeviceBinder.closeDevice(0);
                            bOpen=false;
                        }
                    break;
            }

        }
    };
    public int getState(){
        if(!bOpen){
        deviceTouchState=2;
        modOkProgress=0;
        modelImgMng.reset();
        bOpen=mdDeviceBinder.openDevice(0);//开启指定索引的设备
        if(bOpen){
            Log.e(TAG,"open device success");
            handler.obtainMessage(MSG_SHOW_LOG,"打开指静脉设备成功，请点击下方功能键选择操作").sendToTarget();
        } else{
            Log.e(TAG,"open device failed,stop identifying and modeling.");
            handler.obtainMessage(MSG_SHOW_LOG,"打开设备失败，暂停建模及认证功能，请检查设备连接状态\n").sendToTarget();
        }
    }
        int state = mdDeviceBinder.getDeviceTouchState(0);
        if(state!=3){
            if(lastTouchState!=0){
                mdDeviceBinder.setDeviceLed(0,MdUsbService.getFvColorRED(),true);
            }
            lastTouchState=0;
        }
        if(state==3) {//返回值state=3表检测到了双Touch触摸,返回1表示仅指腹触碰，返回2表示仅指尖触碰，返回0表示未检测到触碰
            lastTouchState = 3;
            deviceTouchState = 0;
            mdDeviceBinder.setDeviceLed(0, MdUsbService.getFvColorGREEN(), false);
            //optional way 3
            img = mdDeviceBinder.tryGrabImg(0);
            if (img == null) {
                Log.e(TAG, "get img failed,please try again");
            }
        }
     return state;
    }
    public void identifyModel(){
        byte[] feature=MdUsbService.extractImgModel(img,null,null);

        if(feature==null) {
            Log.e(TAG,"extractImgModel get feature from img fail,retry soon");
            handler.obtainMessage(MSG_SHOW_LOG,"提取指静脉特征信息失败，请重试").sendToTarget();
        } else {

                tipTimes[0] = 0;
                tipTimes[1] = 0;
                modelImgMng.reset();
                modOkProgress = 0;
                if (identifyNewImg(img, pos, score)) {//比对及判断得分放到identifyNewImg()内实现
                    Log.e(TAG, "identify success(pos=" + pos[0] + ")");
                    handler.obtainMessage(MSG_SHOW_LOG, "认证成功").sendToTarget();
                } else {
                    Log.e("identify fail,", "pos=" + pos[0]);
                    handler.obtainMessage(MSG_SHOW_LOG, "认证失败").sendToTarget();
                    //----------------------------------------------------------
                    //if(isMdDebugOpen&&!featureSpUtils.isFeatureDbEmpty()){//数据库非空+debug模式开启+认证失败时才打印
                    //    String tips="DEBUG:本次认证图片及状态日志已保存到目录：\n"+MdDebugger.debugIdentifySrcByTimeMillis(false,pos[0],score[0],img);
                    //    Log.e(TAG,tips);
                    //    handler.obtainMessage(MSG_SHOW_LOG,tips).sendToTarget();
                    //}
                    //----------------------------------------------------------
                }
            }

    }
    public void workModel(){
        float[] quaScore={0f,0f,0f,0f};
        int quaRtn=MdUsbService.qualityImgEx(img,quaScore);
        String oneResult= ("quality return=" + quaRtn) + ",result=" + quaScore[0] + ",score=" + quaScore[1] + ",fLeakRatio=" + quaScore[2] + ",fPress=" + quaScore[3];
        Log.e(TAG,oneResult);
        int quality=(int)quaScore[0];
        if(quality!=0){
            handler.obtainMessage(MSG_SHOW_LOG,"图像指静脉特征不明显，请重试").sendToTarget();
            handler.obtainMessage(MSG_SWITCH_POP_CONTENT,false).sendToTarget();
            if(isMdDebugOpen) {
                String tips="本次质量评估图片已保存到以下目录：\n";
                Log.e(TAG,tips);
                handler.obtainMessage(MSG_SHOW_LOG,tips).sendToTarget();
            }
        }
        byte[] feature=MdUsbService.extractImgModel(img,null,null);
        if(feature==null) {
            Log.e(TAG,"extractImgModel get feature from img fail,retry soon");
            handler.obtainMessage(MSG_SHOW_LOG,"提取指静脉特征信息失败，请重试").sendToTarget();
        } else {


        modOkProgress++;

            handler.obtainMessage(MSG_SHOW_LOG,"\n数据库中已有此手指特征记录，请勿重复注册此手指！\n请换用其他手指建模").sendToTarget();
            if(modOkProgress==1) {//first model
            tipTimes[0]=0;
            tipTimes[1]=0;
            modelImgMng.setImg1(img);
            modelImgMng.setFeature1(feature);
            Log.e(TAG,"first model ok");
            handler.obtainMessage(MSG_SHOW_LOG,"模板1创建成功（1/3）").sendToTarget();
            handler.obtainMessage(MSG_SHOW_LOG,"请重放同一手指进行第2次建模").sendToTarget();
        }else if(modOkProgress==2){//second model
            ret=MdUsbService.fvSearchFeature(modelImgMng.getFeature1(),1,img,pos,score);
            if(ret && score[0]>MODEL_SCORE_THRESHOLD) {
                feature=MdUsbService.extractImgModel(img,null,null);//无须传入第一张图片，第三次混合特征值时才同时传入3张图；
                if(feature != null) {
                    tipTimes[0]=0;
                    tipTimes[1]=0;
                    modelImgMng.setImg2(img);
                    modelImgMng.setFeature2(feature);
                    Log.e(TAG,"second model ok");
                    handler.obtainMessage(MSG_SHOW_LOG,"模板2创建成功（2/3）").sendToTarget();
                    handler.obtainMessage(MSG_SHOW_LOG,"请重放同一手指进行第3次建模").sendToTarget();
                }else {//第二次建模从图片中取特征值无效
                    modOkProgress=1;
                    if(++tipTimes[0]<=3){
                        Log.e(TAG,"get feature from img failed when try second modeling");
                        handler.obtainMessage(MSG_SHOW_LOG,"提取指静脉特征失败，请重放同一手指进行第2次建模").sendToTarget();
                    }else {//连续超过3次放了不同手指则忽略此次建模重来
                        modOkProgress=0;
                        modelImgMng.reset();
                        Log.e(TAG,"put different finger more than 3 times,this modeling is ignored,a new modeling start.");
                        handler.obtainMessage(MSG_SHOW_LOG,"\n累计超过3次操作异常，本次建模取消，请重新开始建模\n").sendToTarget();
                    }
                }
            }else{
                modOkProgress=1;
                if(++tipTimes[0]<=3){
                    Log.e(TAG,"high difference to last model,seems to has used different finger,please retry for second model.");
                    handler.obtainMessage(MSG_SHOW_LOG,"与上一模板差异太大，请重放同一手指进行第2次建模").sendToTarget();
                }else {//连续超过3次放了不同手指则忽略此次建模重来
                    modOkProgress=0;
                    modelImgMng.reset();
                    Log.e(TAG,"put different finger more than 3 times,this modeling is ignored,a new modeling start.");
                    handler.obtainMessage(MSG_SHOW_LOG,"\n累计超过3次操作异常，本次建模取消，请重新开始建模\n").sendToTarget();
                }
            }
        }else if(modOkProgress==3){//third model
            ret=MdUsbService.fvSearchFeature(modelImgMng.getFeature2(),1,img,pos,score);
            if (ret && score[0]>MODEL_SCORE_THRESHOLD) {
                feature=MdUsbService.extractImgModel(modelImgMng.getImg1(),modelImgMng.getImg2(),img);
                if(feature!=null) {//成功生成一个3次建模并融合的融合特征数组
                    tipTimes[0]=0;
                    tipTimes[1]=0;
                    modelImgMng.setImg3(img);
                    modelImgMng.setFeature3(feature);
                    handler.obtainMessage(MSG_SHOW_LOG,"模板3创建成功（3/3）,模板已保存至数据库\n此手指建模完成\n").sendToTarget();
                    handler.obtainMessage(MSG_SHOW_LOG,"现在你可以放其他手指进行新的建模\n").sendToTarget();
                    //----------------------------------------------------------
                    //if(isMdDebugOpen) {//保存3次建模后的3张图片用于分析异常情况;
                    //    String tips="DEBUG:本次建模图片及日志已经保存到:\n"+MdDebugger.debugModelSrcByTimeMillis(modelImgMng.getImg1(),modelImgMng.getImg2(),modelImgMng.getImg3());
                    //    Log.e(TAG,tips);
                    //    handler.obtainMessage(MSG_SHOW_LOG,tips).sendToTarget();
                    //}
                    //----------------------------------------------------------
                    modelImgMng.reset();
                }else {//第三次建模从图片中取特征值无效
                    modOkProgress=2;
                    if(++tipTimes[1]<=3) {
                        Log.e(TAG,"extract feature get third feature fail.(isAllImgDataOk="+modelImgMng.isAllImgDataOk()+")");
                        handler.obtainMessage(MSG_SHOW_LOG,"提取指静脉特征失败，请重放同一手指进行第3次建模").sendToTarget();
                    }
                }
            } else {
                modOkProgress=2;
                if(++tipTimes[1]<=3) {
                    handler.obtainMessage(MSG_SHOW_LOG,"与上一模板差异太大，请重放同一手指进行第3次建模").sendToTarget();
                }else {//连续超过3次放了不同手指则忽略此次建模重来
                    modOkProgress=0;
                    modelImgMng.reset();
                    Log.e(TAG,"put different finger more than 3 times,this modeling is ignored,a new modeling start.");
                    handler.obtainMessage(MSG_SHOW_LOG,"\n累计超过3次操作异常，本次建模取消，请重新开始建模\n").sendToTarget();
                }
            }
        }else {
            modOkProgress=0;
            modelImgMng.reset();
        }
    }

}

    //-------------------------------------------------------------------------------------------------//抓图与质量评估

    //检查当前手指模版是否已经被注册,不允许数据库中已存在模板的手指重复建模,当比对成功且得分大于认证阈值时返回true，否则返回false；
    private boolean checkIsModelRepeatRegister(final byte[] img){

        return false;
    }

    private boolean identifyNewImg(final byte[] img,int[] pos,float[] score) {


        return false;
    }
}