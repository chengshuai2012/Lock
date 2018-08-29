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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.link.cloud.R;
import com.link.cloud.bean.CabinetNumber;
import com.link.cloud.bean.Person;
import com.link.cloud.utils.HexUtil;
import com.link.cloud.venue.MdDevice;
import com.link.cloud.venue.MdUsbService;
import com.link.cloud.venue.ModelImgMng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    @BindView(R.id.layout_one)
    LinearLayout layoutOne;
    @BindView(R.id.edit_01)
    EditText edit01;
    @BindView(R.id.clean_other)
    Button cleanOther;
    @BindView(R.id.clean_all)
    Button cleanAll;
    @BindView(R.id.back)
    Button back;
    @BindView(R.id.openlock_one)
    EditText openlockOne;
    @BindView(R.id.openlock_button)
    Button openlockButton;
    @BindView(R.id.openlock_all)
    Button openlockAll;
    @BindView(R.id.cabinet_used)
    EditText cabinetUsed;
    @BindView(R.id.record_button)
    Button recordButton;
    @BindView(R.id.lock_message)
    Button lockMessage;
    @BindView(R.id.back_home)
    Button backHome;
    @BindView(R.id.edit_02)
    EditText edit02;
    @BindView(R.id.edit_03)
    EditText edit03;
    @BindView(R.id.openlock_other)
    Button openlockOther;
    @BindView(R.id.chang_pdw)
    Button changPdw;
    @BindView(R.id.button4)
    Button button4;
    @BindView(R.id.textView2)
    TextView textView2;
    @BindView(R.id.adminmessage)
    LinearLayout adminmessage;
    @BindView(R.id.versionName)
    TextView versionName;
    @BindView(R.id.time_forfinger)
    TextView timeForfinger;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.text_error)
    TextView textError;
    @BindView(R.id.qrcode)
    EditText qrcode;
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
    LinearLayout workIdentify;
    @BindView(R.id.main_button)
    LinearLayout mainButton;

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
    private List<Person> peoples;
    private Unbinder bind;
    private RealmResults<CabinetNumber> allBox;
    private Random random;

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
        random = new Random();
    }
    List<CabinetNumber> boxs =new ArrayList<>();
    private void getBox() {
        allBox = realm.where(CabinetNumber.class).equalTo("isUser","可用").findAll();
        this.allBox.addChangeListener(new RealmChangeListener<RealmResults<CabinetNumber>>() {
            @Override
            public void onChange(RealmResults<CabinetNumber> cabinetNumbers) {
                boxs.clear();
                boxs.addAll(cabinetNumbers);
            }
        });
        boxs.clear();
        boxs.addAll(realm.copyFromRealm(allBox));
    }

    @OnClick({R.id.main_bt_01, R.id.main_bt_02, R.id.main_bt_03})
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
                time = 40;
                isIdentyFinsh = false;
                isWorkFinsh = true;
                isLive = false;
                workHandler.sendEmptyMessage(19);
                break;
            case R.id.main_bt_03:
                time = 40;
                isIdentyFinsh = false;
                isWorkFinsh = true;
                isLive = true;
                workHandler.sendEmptyMessage(19);
                break;
        }
    }

    boolean isLive = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mdSrvConn);
        realm.close();
        bind.unbind();
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
                    time--;
                    if (time == 0) {
                        finish();
                    }
                    textError.setText(getString(R.string.register_template_tip));
                    timeForfinger.setText(time+"");
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
                    time--;
                    if (time == 0) {
                        finish();
                    }
                    workHandler.removeMessages(19);
                    int state2 = getState();
                    if (state2 == 3) {
                        identifyModel();
                    }

                    if (!isIdentyFinsh) {
                        workHandler.sendEmptyMessageDelayed(19, 1000);
                    }

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
                        final Person person = new Person();
                        textError.setText(getString(R.string.check_successful));
                        layoutThree.setVisibility(View.INVISIBLE);
                        openLockLayout.setVisibility(View.VISIBLE);
                        int i = random.nextInt(boxs.size());
                        String cabinetNumber = boxs.get(i).getCabinetNumber();
                        person.setUid(cabinetNumber);
                        person.setFeature(HexUtil.bytesToHexString(feature));
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealm(person);
                            }
                        });
                        openLock(cabinetNumber);
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

    private void openLock(String cabinetNumber) {
        StringBuffer fingerstr=new StringBuffer();
        fingerstr.append(byte2hex(feauter3));
        cabinetNumberDao=BaseApplication.getInstances().getDaoSession().getCabinetNumberDao();
        QueryBuilder queryBuilder=cabinetNumberDao.queryBuilder();
        list_cabinet=queryBuilder.where(CabinetNumberDao.Properties.IsUser.eq("可用")).list();
        lockcounts=new int[list_cabinet.size()];
        if (list_cabinet.size()>0) {
            for (int i = 0; i < list_cabinet.size(); i++) {
                lockcounts[i] = Integer.parseInt(list_cabinet.get(i).getCabinetNumber());
            }
            if (lockcounts.length >= 1) {
                Random random = new Random();//创建随机对象
                int arrIdx = random.nextInt(lockcounts.length);
                count = lockcounts[arrIdx] + "";
                layout_three.setVisibility(View.GONE);
                open_lock_layout.setVisibility(View.VISIBLE);
                text_number.setText(count);
                Logger.e("FirstFragment" + "======" + count);
                member = new Member();
                cabinetNumber = new CabinetNumber();
                save_lock = queryBuilder.where(CabinetNumberDao.Properties.CabinetNumber.eq(count)).list();
                if (save_lock.size() > 0) {
                    text_error.setText("存件成功");
                    cabinetNumber.setId(save_lock.get(0).getId());
                    cabinetNumber.setIsUser("使用中");
                    cabinetNumber.setCabinetLockPlate(save_lock.get(0).getCabinetLockPlate());
                    cabinetNumber.setCircuitNumber(save_lock.get(0).getCircuitNumber());
                    cabinetNumber.setCabinetNumber(count);
                    cabinetNumber.setBegintime(opentime);
                    cabinetNumber.setFINGERMODEL(fingerstr.toString());
                    cabinetNumberDao.update(cabinetNumber);
                }
                memberDao = BaseApplication.getInstances().getDaoSession().getMemberDao();
                member.setId(save_lock.get(0).getId());
                member.setBegintime(opentime);
                member.setCabinetLockPlate(save_lock.get(0).getCabinetLockPlate());
                member.setCabinetNumber(count);
                member.setCircuitNumber(save_lock.get(0).getCircuitNumber());
                member.setFingermodel(fingerstr.toString());
                memberDao.insert(member);

            }else {
                Toast.makeText(getActivity(),"没有可用的柜号",Toast.LENGTH_LONG).show();
            }
        }
        Logger.e("FirstFragment"+"count"+count);
        if(count!=null&&!count.equals("")) {
            CabinetRecordDao cabinetRecordDao = BaseApplication.getInstances().getDaoSession().getCabinetRecordDao();
            CabinetRecord cabinetRecord = new CabinetRecord();
            cabinetRecord.setCabinetNumber(count);
            cabinetRecord.setCabinetStating("存件");
            cabinetRecord.setMemberName("会员");
            cabinetRecord.setOpentime(opentime);
            cabinetRecordDao.insert(cabinetRecord);
            queryBuilder = cabinetNumberDao.queryBuilder();
            list_cabinet = queryBuilder.where(CabinetNumberDao.Properties.CabinetNumber.eq(count)).list();
            int nuberlock = Integer.parseInt(list_cabinet.get(0).getCircuitNumber());
            if (nuberlock > 10) {
                nuberlock = nuberlock % 10;
                Logger.e("FirstFragment===" + nuberlock);
                if (nuberlock == 0) {
                    nuberlock = 10;
                }
            }
            try {
                if (Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) <= 10) {
                    serialpprt_wk1.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()), nuberlock));
                } else if (Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) > 10 && Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) < 20) {
                   serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) % 10, nuberlock));
                } else if (Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) > 20 && Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) < 30) {
                    serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) % 10, nuberlock));
                } else if (Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) == 20) {
                   serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(10, nuberlock));
                } else if (Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) == 30) {
                    serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(10, nuberlock));
                }
                Logger.e("FirstFragment===" + Integer.parseInt(list_cabinet.get(0).getCabinetLockPlate()) + "====" + count);
            } catch (Exception e) {
            } finally {
//                                if (timer != null) {
//                                    timer.cancel();
//                                }
            }
        }else {
            Toast.makeText(getActivity(),"没有可用的柜号",Toast.LENGTH_LONG).show();
        }
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
            String featureName = uidss[pos[0]];
            Log.e(TAG, "identifyResult: " + featureName);
            isIdentyFinsh = true;
            return identifyResult;
        } else {

            return identifyResult;


        }


    }
    private void fingersign(){

        if (workHandler!=null) {


            workHandler.postDelayed(new Runnable() {

                @Override

                public void run() {
                    mainButton.setVisibility(View.VISIBLE);
                    workIdentify.setVisibility(View.GONE);
                }

            }, 3000);

        }

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