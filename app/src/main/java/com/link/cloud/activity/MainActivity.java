package com.link.cloud.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hotelmanager.xzy.util.OpenDoorUtil;
import com.link.cloud.R;
import com.link.cloud.base.BaseApplication;
import com.link.cloud.bean.CabinetNumber;
import com.link.cloud.bean.CabinetRecord;
import com.link.cloud.bean.Person;
import com.link.cloud.utils.HexUtil;
import com.link.cloud.utils.ToastUtils;
import com.link.cloud.venue.MdDevice;
import com.link.cloud.venue.MdUsbService;
import com.link.cloud.venue.ModelImgMng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android_serialport_api.SerialPort;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import md.com.sdk.MicroFingerVein;


public class MainActivity extends Activity {
    private final static int MSG_SHOW_LOG = 0;
    private final static int MSG_SWITCH_POP_CONTENT = 6;
    private final static float IDENTIFY_SCORE_THRESHOLD = 0.63f;//认证通过的得分阈值，超过此得分才认为认证通过；
    private final static float MODEL_SCORE_THRESHOLD = 0.4f;//同一手指第2，3次建模模版与前1，2次的匹配得分阈值，低于此值认为换用了其他手指；
    public static MdDevice mdDevice;
    @BindView(R.id.edit_code)
    EditText editCode;
    @BindView(R.id.head_text_01)
    TextView headText01;
    @BindView(R.id.head_text_02)
    TextView headText02;
    @BindView(R.id.head_text_03_main)
    TextView headText03Main;
    @BindView(R.id.text_num1)
    TextView textNum1;
    @BindView(R.id.text_num2)
    TextView textNum2;
    @BindView(R.id.text_num3)
    TextView textNum3;
    @BindView(R.id.main_bt_01)
    LinearLayout mainBt01;
    @BindView(R.id.main_bt_02)
    LinearLayout mainBt02;
    @BindView(R.id.main_bt_03)
    LinearLayout mainBt03;
    @BindView(R.id.main_button)
    LinearLayout mainButton;
    @BindView(R.id.back_top)
    TextView backTop;
    @BindView(R.id.time_forfinger)
    TextView timeForfinger;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.text_error)
    TextView textError;
    @BindView(R.id.layout_error_text)
    LinearLayout layoutErrorText;
    @BindView(R.id.layout_three)
    LinearLayout layoutThree;
    @BindView(R.id.text_start)
    TextView textStart;
    @BindView(R.id.text_number)
    TextView textNumber;
    @BindView(R.id.text_end)
    TextView textEnd;
    @BindView(R.id.open_lock_layout)
    LinearLayout openLockLayout;
    @BindView(R.id.work_identify)
    RelativeLayout workIdentify;
    @BindView(R.id.layout_one)
    LinearLayout layoutOne;
    @BindView(R.id.versionName)
    TextView versionName;


    private List<MdDevice> mdDevicesList = new ArrayList<MdDevice>();

    private Handler listManageH = new Handler(new Handler.Callback() {

        @Override

        public boolean handleMessage(Message msg) {

            switch (msg.what) {

                case MSG_REFRESH_LIST: {

                    mdDevicesList.clear();

                    mdDevicesList = getDevList();

                    if (mdDevicesList.size() > 0) {

                        mdDevice = mdDevicesList.get(0);

                    } else {

                        listManageH.sendEmptyMessageDelayed(MSG_REFRESH_LIST, 1500L);

                    }

                    break;

                }

            }

            return false;

        }

    });
    private RealmResults<Person> all;
    private List<Person> peoples = new ArrayList<>();
    private Unbinder bind;
    private RealmResults<CabinetNumber> allBox;
    private Random random;
    private String uid;
    private RealmResults<CabinetNumber> isUser;
    private RealmResults<CabinetNumber> allCabinetNumber;

    private List<MdDevice> getDevList() {

        List<MdDevice> mdDevList = new ArrayList<MdDevice>();

        if (mdDeviceBinder != null) {

            int deviceCount = MicroFingerVein.fvdev_get_count();

            for (int i = 0; i < deviceCount; i++) {

                MdDevice mdDevice = new MdDevice();

                mdDevice.setNo(i);

                mdDevice.setIndex(mdDeviceBinder.getDeviceNo(i));

                mdDevList.add(mdDevice);

            }

        } else {

            Log.e(TAG, "microFingerVein not initialized by MdUsbService yet,wait a moment...");

        }

        return mdDevList;

    }


    public MdUsbService.MyBinder mdDeviceBinder;

    private String TAG = "BindActivity";

    private ServiceConnection mdSrvConn = new ServiceConnection() {

        @Override

        public void onServiceConnected(ComponentName name, IBinder service) {

            mdDeviceBinder = (MdUsbService.MyBinder) service;


            if (mdDeviceBinder != null) {

                mdDeviceBinder.setOnUsbMsgCallback(mdUsbMsgCallback);

                listManageH.sendEmptyMessage(MSG_REFRESH_LIST);

                Log.e(TAG, "bind MdUsbService success.");

            } else {

                Log.e(TAG, "bind MdUsbService failed.");

                finish();

            }

        }

        @Override

        public void onServiceDisconnected(ComponentName name) {

            Log.e(TAG, "disconnect MdUsbService.");

        }


    };

    private final int MSG_REFRESH_LIST = 0;

    private MdUsbService.UsbMsgCallback mdUsbMsgCallback = new MdUsbService.UsbMsgCallback() {

        @Override

        public void onUsbConnSuccess(String usbManufacturerName, String usbDeviceName) {

            String newUsbInfo = "USB厂商：" + usbManufacturerName + "  \nUSB节点：" + usbDeviceName;

            Log.e(TAG, newUsbInfo);

        }

        @Override

        public void onUsbDisconnect() {

            Log.e(TAG, "USB连接已断开");


        }

    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            return false;
        }
    });
    private byte[] img;
    Realm realm;
    SharedPreferences userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        bind = ButterKnife.bind(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = new Intent(this, MdUsbService.class);
        bindService(intent, mdSrvConn, Service.BIND_AUTO_CREATE);
        realm = Realm.getDefaultInstance();
        getPerson();
        getBox();
        getTotal();
        getUsed();
        PackageInfo pi = null;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName.setText(pi.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        headText02.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ExitAlertDialog dialog = new ExitAlertDialog(MainActivity.this);
                dialog.show();
                return false;
            }
        });
        userInfo = getSharedPreferences("user_info", 0);
        String devicepwd = userInfo.getString("devicepwd", "");
        if (TextUtils.isEmpty(devicepwd)) {
            userInfo.edit().putString("devicepwd", "888888").commit();
        }
        random = new Random();
        openDoorUtil = new OpenDoorUtil();
        try {
            serialpprt_wk1 = new SerialPort(new File("/dev/ttysWK1"), 9600, 0);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            serialpprt_wk2 = new SerialPort(new File("/dev/ttysWK2"), 9600, 0);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            serialpprt_wk3 = new SerialPort(new File("/dev/ttysWK3"), 9600, 0);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mesReceiver = new MesReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseApplication.ACTION_UPDATEUI);
        registerReceiver(mesReceiver, intentFilter);
    }
    int userType=1;


    private void getUsed() {
        isUser = realm.where(CabinetNumber.class).equalTo("isUser", "使用中").findAll();
        isUser.addChangeListener(new RealmChangeListener<RealmResults<CabinetNumber>>() {
            @Override
            public void onChange(RealmResults<CabinetNumber> cabinetNumbers) {
                textNum2.setText("已用:"+cabinetNumbers.size() + "");
                Log.e("onChange: ", "userchanged");
            }
        });
        textNum2.setText("已用:"+isUser.size() );
    }

    private void getTotal() {
        allCabinetNumber = realm.where(CabinetNumber.class).findAll();
        allCabinetNumber.addChangeListener(new RealmChangeListener<RealmResults<CabinetNumber>>() {
            @Override
            public void onChange(RealmResults<CabinetNumber> cabinetNumbers) {
                textNum1.setText("全部:"+cabinetNumbers.size() + "");
            }
        });
        textNum1.setText("全部:"+allCabinetNumber.size() );
    }

    MesReceiver mesReceiver;
    String opentime;

    public class MesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            headText03Main.setText(intent.getStringExtra("timeStr"));
            opentime = intent.getStringExtra("timeStr");
            headText01.setText(intent.getStringExtra("timeData"));
        }
    }

    List<CabinetNumber> boxs = new ArrayList<>();

    private void getBox() {
        allBox = realm.where(CabinetNumber.class).equalTo("isUser", "可用").findAll();
        this.allBox.addChangeListener(new RealmChangeListener<RealmResults<CabinetNumber>>() {
            @Override
            public void onChange(RealmResults<CabinetNumber> cabinetNumbers) {
                boxs.clear();
                boxs.addAll(cabinetNumbers);
                textNum3.setText("剩余:"+cabinetNumbers.size() );
            }
        });
        boxs.clear();
        boxs.addAll(realm.copyFromRealm(allBox));
        textNum3.setText("剩余:"+allBox.size() + "");
    }

    int openType = 1;

    @OnClick({R.id.main_bt_01, R.id.main_bt_02, R.id.main_bt_03, R.id.head_text_02,R.id.back_top})
    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.main_bt_01:
                mainButton.setVisibility(View.INVISIBLE);
                workIdentify.setVisibility(View.VISIBLE);
                layoutThree.setVisibility(View.VISIBLE);
                openLockLayout.setVisibility(View.INVISIBLE);
                textError.setText(getString(R.string.register_template_tip));
                isWorkFinsh = false;
                isIdentyFinsh = true;
                time = 40;
                workHandler.sendEmptyMessage(18);
                break;
            case R.id.main_bt_02:
                mainButton.setVisibility(View.INVISIBLE);
                workIdentify.setVisibility(View.VISIBLE);
                layoutThree.setVisibility(View.VISIBLE);
                openLockLayout.setVisibility(View.INVISIBLE);
                textError.setText(getString(R.string.register_template_tip));
                openType = 1;
                time = 40;
                isIdentyFinsh = false;
                isWorkFinsh = true;
                isLive = false;
                workHandler.sendEmptyMessage(19);
                break;
            case R.id.main_bt_03:
                mainButton.setVisibility(View.INVISIBLE);
                workIdentify.setVisibility(View.VISIBLE);
                layoutThree.setVisibility(View.VISIBLE);
                openLockLayout.setVisibility(View.INVISIBLE);
                textError.setText(getString(R.string.register_template_tip));
                time = 40;
                openType = 2;
                isIdentyFinsh = false;
                isWorkFinsh = true;
                isLive = true;
                workHandler.sendEmptyMessage(19);
                break;
            case R.id.back_top:
                mainButton.setVisibility(View.VISIBLE);
                workIdentify.setVisibility(View.INVISIBLE);
                layoutThree.setVisibility(View.INVISIBLE);
                openLockLayout.setVisibility(View.VISIBLE);
                isIdentyFinsh=true;
                isWorkFinsh=true;
                workHandler.removeMessages(20);
                break;

        }
    }

    private class ExitAlertDialog extends Dialog implements View.OnClickListener {
        private Context mContext;
        private EditText etPwd;
        private Button btCancel;
        private Button btConfirm;
        private TextView texttilt;

        public ExitAlertDialog(Context context, int theme) {
            super(context, theme);
            mContext = context;
            initDialog();
        }

        public ExitAlertDialog(Context context) {
            super(context, R.style.customer_dialog);
            mContext = context;
            initDialog();
        }

        private void initDialog() {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_exit_confirm, null);
            setContentView(view);
            btCancel = (Button) view.findViewById(R.id.btCancel);
            btConfirm = (Button) view.findViewById(R.id.btConfirm);
            etPwd = (EditText) view.findViewById(R.id.deviceCode);
            texttilt = (TextView) view.findViewById(R.id.text_title);
            btCancel.setOnClickListener(this);
            btConfirm.setOnClickListener(this);
        }

        @Override
        public void show() {
            etPwd.setText("");
            super.show();
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btCancel:
                    this.dismiss();
                    break;
                case R.id.btConfirm:
                    String pwd = etPwd.getText().toString().trim();
                    if (TextUtils.isEmpty(pwd)) {
                        ToastUtils.show(mContext, "请输入密码", ToastUtils.LENGTH_SHORT);
                        return;
                    }


                    String repwd = userInfo.getString("devicepwd", "0");

                    if (!pwd.equals(repwd)) {
                        ToastUtils.show(mContext, "密码不正确", ToastUtils.LENGTH_SHORT);
                        this.dismiss();
                        return;
                    } else {
                        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                        startActivityForResult(intent,188);
                        this.dismiss();
                    }
                    break;
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==188&&resultCode==RESULT_OK){
            userType = data.getIntExtra("userType",1);
            if(userType==2){
                mainButton.setVisibility(View.INVISIBLE);
                workIdentify.setVisibility(View.VISIBLE);
                layoutThree.setVisibility(View.VISIBLE);
                openLockLayout.setVisibility(View.INVISIBLE);
                textError.setText(getString(R.string.register_template_tip));
                isWorkFinsh = false;
                isIdentyFinsh = true;
                time = 40;
                workHandler.sendEmptyMessage(18);
            }
        }
    }

    boolean isLive = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mdSrvConn);
        realm.close();
        bind.unbind();
        workHandler.removeCallbacksAndMessages(null);
        isIdentyFinsh=true;
        isWorkFinsh=true;
        unregisterReceiver(mesReceiver);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean bOpen = false;//设备是否打开
    private int[] pos = new int[1];
    private float[] score = new float[1];
    private boolean ret;
    private ModelImgMng modelImgMng = new ModelImgMng();
    private int[] tipTimes = {0, 0};//后两次次建模时用了不同手指或提取特征识别时，最多重复提醒限制3次
    private int lastTouchState = 0;//记录上一次的触摸状态
    private int modOkProgress = 0;
    boolean isWorkFinsh = false;
    boolean isIdentyFinsh = false;
    int time = 0;
    Handler workHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case 18:
                    if(textError==null){
                        return;
                    }
                    time--;
                    if (time == 0) {
                        isWorkFinsh = true;
                        mainButton.setVisibility(View.VISIBLE);
                        workIdentify.setVisibility(View.GONE);
                    }
                    textError.setText(getString(R.string.register_template_tip));
                    timeForfinger.setText(time + "");
                    handler.removeMessages(19);
                    int state = getState();
                    Log.e(TAG, state + "");
                    if (state == 3) {
                        workModel();
                    }
                    if (!isWorkFinsh) {
                        workHandler.sendEmptyMessageDelayed(18, 1000);
                    }

                    break;
                case 19:
                    if(textError==null){
                        return;
                    }
                    time--;
                    if (time == 0) {
                        isIdentyFinsh = true;
                        mainButton.setVisibility(View.VISIBLE);
                        workIdentify.setVisibility(View.GONE);
                    }
                    textError.setText(getString(R.string.register_template_tip));
                    workHandler.removeMessages(19);
                    int state2 = getState();
                    if (state2 == 3) {
                        identifyModel();
                    }
                    timeForfinger.setText(time + "");
                    if (!isIdentyFinsh) {
                        workHandler.sendEmptyMessageDelayed(19, 1000);
                    }

                    break;
                case 20:
                    if(mainButton==null){
                        return;
                    }
                    mainButton.setVisibility(View.VISIBLE);
                    workIdentify.setVisibility(View.GONE);
                    break;
            }

        }
    };

    public int getState() {
        if (!bOpen) {
            modOkProgress = 0;
            modelImgMng.reset();
            bOpen = mdDeviceBinder.openDevice(0);//开启指定索引的设备
            if (bOpen) {
                Log.e(TAG, "open device success");
                handler.obtainMessage(MSG_SHOW_LOG, "打开指静脉设备成功，请点击下方功能键选择操作").sendToTarget();
            } else {
                Log.e(TAG, "open device failed,stop identifying and modeling.");
                handler.obtainMessage(MSG_SHOW_LOG, "打开设备失败，暂停建模及认证功能，请检查设备连接状态\n").sendToTarget();
            }
        }
        int state = mdDeviceBinder.getDeviceTouchState(0);
        if (state != 3) {
            if (lastTouchState != 0) {
                mdDeviceBinder.setDeviceLed(0, MdUsbService.getFvColorRED(), true);
            }
            lastTouchState = 0;
        }
        if (state == 3) {
            //返回值state=3表检测到了双Touch触摸,返回1表示仅指腹触碰，返回2表示仅指尖触碰，返回0表示未检测到触碰
            if (lastTouchState == 3) {
                textError.setText(getString(R.string.move_finger));
                return 4;
            }
            lastTouchState = 3;
            mdDeviceBinder.setDeviceLed(0, MdUsbService.getFvColorGREEN(), false);
            //optional way 3
            img = mdDeviceBinder.tryGrabImg(0);
            if (img == null) {
                Log.e(TAG, "get img failed,please try again");
            }
        }
        return state;
    }

    public void identifyModel() {
        byte[] feature = MdUsbService.extractImgModel(img, null, null);

        if (feature == null) {
            Log.e(TAG, "extractImgModel get feature from img fail,retry soon");
            handler.obtainMessage(MSG_SHOW_LOG, "提取指静脉特征信息失败，请重试").sendToTarget();
        } else {
            if (identifyNewImg(img, pos, score)) {//比对及判断得分放到identifyNewImg()内实现
                Log.e(TAG, "identify success(pos=" + pos[0] + ")");
                handler.obtainMessage(MSG_SHOW_LOG, "认证成功").sendToTarget();
                mdDeviceBinder.closeDevice(0);
                bOpen = false;
                textError.setText(getString(R.string.check_successful));
                layoutThree.setVisibility(View.INVISIBLE);
                openLockLayout.setVisibility(View.VISIBLE);
                textNumber.setText(uid);
                fingersign();
            } else {
                Log.e("identify fail,", "pos=" + pos[0]);
                handler.obtainMessage(MSG_SHOW_LOG, "认证失败").sendToTarget();
            }
        }

    }

    public void workModel() {
        float[] quaScore = {0f, 0f, 0f, 0f};
        int quaRtn = MdUsbService.qualityImgEx(img, quaScore);
        String oneResult = ("quality return=" + quaRtn) + ",result=" + quaScore[0] + ",score=" + quaScore[1] + ",fLeakRatio=" + quaScore[2] + ",fPress=" + quaScore[3];
        Log.e(TAG, oneResult);
        int quality = (int) quaScore[0];
        if (quality != 0) {

            textError.setText(getString(R.string.move_finger));
        }
        byte[] feature = MdUsbService.extractImgModel(img, null, null);
        if (feature == null) {
            textError.setText(getString(R.string.move_finger));
        } else {
            modOkProgress++;
            if (modOkProgress == 1) {//first model
                tipTimes[0] = 0;
                tipTimes[1] = 0;
                modelImgMng.setImg1(img);
                modelImgMng.setFeature1(feature);
                textError.setText(getString(R.string.again_finger));
            } else if (modOkProgress == 2) {//second model
                ret = MdUsbService.fvSearchFeature(modelImgMng.getFeature1(), 1, img, pos, score);
                if (ret && score[0] > MODEL_SCORE_THRESHOLD) {
                    feature = MdUsbService.extractImgModel(img, null, null);//无须传入第一张图片，第三次混合特征值时才同时传入3张图；
                    if (feature != null) {
                        tipTimes[0] = 0;
                        tipTimes[1] = 0;
                        modelImgMng.setImg2(img);
                        modelImgMng.setFeature2(feature);
                        textError.setText(getString(R.string.again_finger));
                    } else {//第二次建模从图片中取特征值无效
                        modOkProgress = 1;
                        if (++tipTimes[0] <= 3) {
                            textError.setText(getString(R.string.same_finger));
                        } else {//连续超过3次放了不同手指则忽略此次建模重来
                            modOkProgress = 0;
                            modelImgMng.reset();
                            textError.setText(getString(R.string.same_finger));
                        }
                    }
                } else {
                    modOkProgress = 1;
                    if (++tipTimes[0] <= 3) {
                        textError.setText(getString(R.string.same_finger));
                    } else {//连续超过3次放了不同手指则忽略此次建模重来
                        modOkProgress = 0;
                        modelImgMng.reset();
                        textError.setText(getString(R.string.same_finger));
                    }
                }
            } else if (modOkProgress == 3) {//third model
                ret = MdUsbService.fvSearchFeature(modelImgMng.getFeature2(), 1, img, pos, score);
                if (ret && score[0] > MODEL_SCORE_THRESHOLD) {
                    feature = MdUsbService.extractImgModel(modelImgMng.getImg1(), modelImgMng.getImg2(), img);
                    if (feature != null) {//成功生成一个3次建模并融合的融合特征数组
                        tipTimes[0] = 0;
                        tipTimes[1] = 0;
                        modelImgMng.setImg3(img);
                        modelImgMng.setFeature3(feature);
                        handler.obtainMessage(MSG_SHOW_LOG, "模板3创建成功（3/3）,模板已保存至数据库\n此手指建模完成\n").sendToTarget();
                        handler.obtainMessage(MSG_SHOW_LOG, "现在你可以放其他手指进行新的建模\n").sendToTarget();
                        //----------------------------------------------------------
                        //if(isMdDebugOpen) {//保存3次建模后的3张图片用于分析异常情况;
                        //    String tips="DEBUG:本次建模图片及日志已经保存到:\n"+MdDebugger.debugModelSrcByTimeMillis(modelImgMng.getImg1(),modelImgMng.getImg2(),modelImgMng.getImg3());
                        //    Log.e(TAG,tips);
                        //    handler.obtainMessage(MSG_SHOW_LOG,tips).sendToTarget();
                        //}
                        //----------------------------------------------------------

                        if (boxs.size() > 0&&userType==1) {
                            final Person person = new Person();
                            textError.setText(getString(R.string.check_successful));
                            layoutThree.setVisibility(View.INVISIBLE);
                            openLockLayout.setVisibility(View.VISIBLE);
                            int i = random.nextInt(boxs.size());
                            String cabinetNumber = boxs.get(i).getCabinetNumber();
                            person.setUid(cabinetNumber);
                            person.setFeature(HexUtil.bytesToHexString(feature));
                            person.setFingerId(userType);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealm(person);
                                }
                            });
                            openLock(cabinetNumber);
                        } else if(userType==2){
                            final Person person = new Person();
                            person.setUid("管理员");
                            person.setFeature(HexUtil.bytesToHexString(feature));
                            person.setFingerId(2);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealm(person);
                                }
                            });
                            userType=1;
                            textError.setText("绑定成功");
                            fingersign();
                        }else {
                            Toast.makeText(MainActivity.this, "没有可用的柜号", Toast.LENGTH_LONG).show();
                        }

                        isWorkFinsh = true;
                        modelImgMng.reset();
                        fingersign();
                        mdDeviceBinder.closeDevice(0);
                        bOpen = false;
                    } else {//第三次建模从图片中取特征值无效
                        modOkProgress = 2;
                        if (++tipTimes[1] <= 3) {
                            textError.setText(getString(R.string.same_finger));
                        }
                    }
                } else {
                    modOkProgress = 2;
                    if (++tipTimes[1] <= 3) {
                        textError.setText(getString(R.string.same_finger));
                    } else {//连续超过3次放了不同手指则忽略此次建模重来
                        modOkProgress = 0;
                        modelImgMng.reset();
                        textError.setText(getString(R.string.same_finger));
                    }
                }
            } else {
                modOkProgress = 0;
                modelImgMng.reset();
            }
        }

    }

    OpenDoorUtil openDoorUtil;
    public SerialPort serialpprt_wk1 = null;
    public SerialPort serialpprt_wk2 = null;
    public SerialPort serialpprt_wk3 = null;

    private void openLock(String cabinetNumber) {
        final CabinetNumber openCabinet = realm.where(CabinetNumber.class).equalTo("cabinetNumber", cabinetNumber).findFirst();
        boolean b = openLock(cabinetNumber, openCabinet);
        if (b) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    openCabinet.setIsUser("使用中");
                }
            });
            final CabinetRecord cabinetRecord = new CabinetRecord();
            cabinetRecord.setCabinetNumber(cabinetNumber);
            cabinetRecord.setCabinetStating("存件");
            cabinetRecord.setMemberName("会员");
            cabinetRecord.setOpentime(opentime);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealm(cabinetRecord);
                }
            });
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            isIdentyFinsh=true;
            isWorkFinsh=true;
            workHandler.removeCallbacksAndMessages(null);
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean openLock(String cabinetNumber, CabinetNumber openCabinet) {
        boolean isOpenSuccess = false;
        String circuitNumber = openCabinet.getCircuitNumber();
        int nuberlock = Integer.parseInt(circuitNumber);
        if (nuberlock > 10) {
            nuberlock = nuberlock % 10;
            if (nuberlock == 0) {
                nuberlock = 10;
            }
        }
        try {
            if (Integer.parseInt(openCabinet.getCabinetLockPlate()) <= 10) {
                serialpprt_wk1.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(openCabinet.getCabinetLockPlate()), nuberlock));
            } else if (Integer.parseInt(openCabinet.getCabinetLockPlate()) > 10 && Integer.parseInt(openCabinet.getCabinetLockPlate()) < 20) {
                serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(openCabinet.getCabinetLockPlate()) % 10, nuberlock));
            } else if (Integer.parseInt(openCabinet.getCabinetLockPlate()) > 20 && Integer.parseInt(openCabinet.getCabinetLockPlate()) < 30) {
                serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(openCabinet.getCabinetLockPlate()) % 10, nuberlock));
            } else if (Integer.parseInt(openCabinet.getCabinetLockPlate()) == 20) {
                serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(10, nuberlock));
            } else if (Integer.parseInt(openCabinet.getCabinetLockPlate()) == 30) {
                serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(10, nuberlock));
            }
            textNumber.setText(cabinetNumber);
            isOpenSuccess = true;
        } catch (Exception e) {
            textEnd.setText("开柜失败,请重新开柜");
        } finally {

        }
        return isOpenSuccess;
    }

    private boolean identifyNewImg(final byte[] img, int[] pos, float[] score) {
        boolean identifyResult = false;
        String[] uidss = new String[peoples.size()];
        Log.e(TAG, "identifyNewImg: " + uidss.length);
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < peoples.size(); x++) {
            builder.append(peoples.get(x).getFeature());
            uidss[x] = peoples.get(x).getUid();

        }

        byte[] allFeaturesBytes = HexUtil.hexStringToByte(builder.toString());
        builder.delete(0, builder.length());
        Log.e(TAG, "allFeaturesBytes: " + allFeaturesBytes.length);
        //比对是否通过
        identifyResult = MicroFingerVein.fv_index(allFeaturesBytes, allFeaturesBytes.length / 3352, img, pos, score);
        Log.e(TAG, "identifyResult: " + identifyResult);
        identifyResult = identifyResult && score[0] > IDENTIFY_SCORE_THRESHOLD;//得分是否达标
        Log.e(TAG, "identifyResult: " + identifyResult);

        if (identifyResult) {//比对通过且得分达标时打印此手指绑定的用户名
            int fingerId = peoples.get(pos[0]).getFingerId();
            if(fingerId==1){
            uid = uidss[pos[0]];
            Log.e(TAG, "identifyResult: " + uid);
            final CabinetNumber cabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetNumber", uid).findFirst();
            boolean b = openLock(uid, cabinetNumber);
            textError.setText(getString(R.string.check_successful));
            if (b) {
                    if (openType == 1) {
                        final CabinetRecord cabinetRecord = new CabinetRecord();
                        cabinetRecord.setCabinetNumber(uid);
                        cabinetRecord.setCabinetStating("续存");
                        cabinetRecord.setMemberName("会员");
                        cabinetRecord.setOpentime(opentime);
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealm(cabinetRecord);
                            }
                        });
                    } else if (openType == 2) {
                        final CabinetRecord cabinetRecord = new CabinetRecord();
                        cabinetRecord.setCabinetNumber(uid);
                        cabinetRecord.setCabinetStating("离场");
                        cabinetRecord.setMemberName("会员");
                        cabinetRecord.setOpentime(opentime);
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealm(cabinetRecord);
                            }
                        });
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                cabinetNumber.setIsUser("可用");
                            }
                        });
                        final Person uid = realm.where(Person.class).equalTo("uid", this.uid).findFirst();
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                uid.deleteFromRealm();
                            }
                        });
                    }
                }
                isIdentyFinsh = true;
            }else {
                isIdentyFinsh = true;
                identifyResult=false;
                fingersign();
                startActivity(new Intent(this,SettingActivity.class));
            }

            return identifyResult;
        } else {
            textError.setText(getString(R.string.check_failed));
            return identifyResult;

        }


    }

    private void fingersign() {
        workHandler.sendEmptyMessageDelayed(20,3000);
    }

    public void getPerson() {
        all = realm.where(Person.class).findAll();
        all.addChangeListener(new RealmChangeListener<RealmResults<Person>>() {
            @Override
            public void onChange(RealmResults<Person> people) {
                peoples.clear();
                peoples.addAll(people);

            }
        });
        peoples.clear();
        peoples.addAll(all);
    }


}