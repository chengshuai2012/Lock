package com.link.cloud.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hotelmanager.xzy.util.OpenDoorUtil;
import com.link.cloud.R;
import com.link.cloud.base.BaseApplication;
import com.link.cloud.bean.CabinetNumber;
import com.link.cloud.bean.CabinetRecord;
import com.link.cloud.bean.Person;
import com.link.cloud.utils.ToastUtils;
import com.link.cloud.view.CheckUsedRecored;
import com.link.cloud.view.LockMessage;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android_serialport_api.SerialPort;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmResults;


/**
 * Created by OFX002 on 2018/8/27.
 */

public class SettingActivity extends Activity {
    @BindView(R.id.head_text_01)
    TextView headText01;
    @BindView(R.id.head_text_02)
    TextView headText02;
    @BindView(R.id.head_text_03_main)
    TextView headText03Main;
    @BindView(R.id.et_input)
    EditText et_input;
    @BindView(R.id.open_cabinetnum)
    Button openCabinetnum;
    @BindView(R.id.clean_cabinetnum)
    Button cleanCabinetnum;
    @BindView(R.id.check_record)
    Button checkRecord;
    @BindView(R.id.open_lockPlate)
    Button openLockPlate;
    @BindView(R.id.et_cabinetnum)
    EditText etCabinetnum;
    @BindView(R.id.cabinet_repair)
    Button cabinetRepair;
    @BindView(R.id.cabinet_isuse)
    Button cabinetIsuse;
    @BindView(R.id.check_all)
    Button checkAll;
    @BindView(R.id.clean_all)
    Button cleanAll;
    @BindView(R.id.open_all)
    Button openAll;
    @BindView(R.id.open_app)
    Button openApp;
    @BindView(R.id.chang_pdw)
    Button changPdw;
    @BindView(R.id.back)
    Button back;
    @BindView(R.id.et_lockPlate)
    EditText et_lockPlate;
    @BindView(R.id.et_cabinet_count)
    EditText et_cabinet_count;
    @BindView(R.id.start_lock)
    EditText start_lock;
    @BindView(R.id.start_number)
    EditText start_num;
    @BindView(R.id.cabinet_add)
    Button cabinetAdd;
    @BindView(R.id.cabinet_delect)
    Button cabinetDelect;
    @BindView(R.id.adminmessage)
    LinearLayout adminmessage;
    private Realm realm;
    OpenDoorUtil openDoorUtil;
    public SerialPort serialpprt_wk1 = null;
    public SerialPort serialpprt_wk2 = null;
    public SerialPort serialpprt_wk3 = null;
    MyExitAlertDialog dialog;
    ExitAlertDialog exitAlertDialog;
    private String input;
    SerialPort serialPort;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);
        ButterKnife.bind(this);
        realm = Realm.getDefaultInstance();
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
    String num;
    MesReceiver mesReceiver;
    @OnClick({R.id.cabinet_add,R.id.cabinet_delect,R.id.cabinet_repair,R.id.cabinet_isuse,R.id.open_all,R.id.open_lockPlate,R.id.open_cabinetnum, R.id.clean_cabinetnum,
            R.id.check_record,R.id.chang_pdw,R.id.check_all,R.id.back,R.id.clean_all,R.id.open_app,R.id.add_manager})
    public void OnClick(View view){
        num=etCabinetnum.getText().toString().trim();
        input = et_input.getText().toString().trim();
        switch (view.getId()){
            case R.id.add_manager:
                Intent intent = new Intent();
                intent.putExtra("userType",2);
                setResult(RESULT_OK,intent);
                finish();
                break;
            case R.id.cabinet_add:
                if (TextUtils.isEmpty(et_lockPlate.getText().toString().trim())) {
                    Toast.makeText(SettingActivity.this, "请输入锁板号", Toast.LENGTH_LONG).show();
                } else if (!TextUtils.isEmpty(et_lockPlate.getText().toString().trim()) && TextUtils.isEmpty(et_cabinet_count.getText().toString().trim())) {
                    Toast.makeText(SettingActivity.this, "请输入添加的柜子数量", Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(start_lock.getText().toString().trim())) {
                    Toast.makeText(SettingActivity.this, "请输入起始线路号", Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(start_num.getText().toString().trim())) {
                    Toast.makeText(SettingActivity.this, "请输入起始柜号", Toast.LENGTH_LONG).show();
                } else if (!TextUtils.isEmpty(start_num.getText().toString().trim()) && !TextUtils.isEmpty(start_lock.getText().toString().trim()) && !TextUtils.isEmpty(et_lockPlate.getText().toString().trim()) && !TextUtils.isEmpty(et_cabinet_count.getText().toString().trim())) {

                    if (Integer.parseInt(start_lock.getText().toString().trim()) > 10 || Integer.parseInt(start_lock.getText().toString().trim()) < 1 || Integer.parseInt(start_num.getText().toString().trim()) < 1) {
                        Toast.makeText(SettingActivity.this, "请输入正确的线路号或柜号", Toast.LENGTH_LONG).show();
                    } else {
                        long cabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetNumber", start_num.getText().toString().trim()).count();
                        if (cabinetNumber != 0) {
                            Toast.makeText(SettingActivity.this, "不能添加相同的柜号", Toast.LENGTH_LONG).show();
                        } else {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    for (int i = 0; i < Integer.parseInt(et_cabinet_count.getText().toString().trim()); i++) {
                                        CabinetNumber cabinetNumber = new CabinetNumber();
                                        cabinetNumber.setCabinetLockPlate(et_lockPlate.getText().toString());
                                        cabinetNumber.setCircuitNumber(Integer.parseInt(start_lock.getText().toString().trim()) + i + "");
                                        cabinetNumber.setIsUser("可用");
                                        cabinetNumber.setCabinetNumber(Integer.parseInt(start_num.getText().toString().trim()) + i + "");
                                        realm.copyToRealm(cabinetNumber);
                                    }
                                }
                            });

                        }
                    }
                } else if (Integer.parseInt(et_lockPlate.getText().toString()) > 30 && Integer.parseInt(et_lockPlate.getText().toString()) < 0) {
                    Toast.makeText(SettingActivity.this, "不能添加超过范围的锁板", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.open_cabinetnum:

                if (!TextUtils.isEmpty(input)) {
                    List<CabinetNumber> list = realm.where(CabinetNumber.class).equalTo("cabinetNumber", input).findAll();
                    if (list.size() > 0) {
                        int lockPlate = Integer.parseInt(list.get(0).getCabinetLockPlate());
                        int circuit = Integer.parseInt(list.get(0).getCircuitNumber());
                        if (lockPlate <= 10) {
                            try {
                                serialpprt_wk1.getOutputStream().write(openDoorUtil.openOneDoor(lockPlate, circuit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (lockPlate < 20 && lockPlate > 10) {
                            try {
                                serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(lockPlate % 10, circuit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (lockPlate < 30 && lockPlate > 20) {
                            try {
                                serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(lockPlate % 10, circuit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (lockPlate == 20) {
                            try {
                                serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(10, circuit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (lockPlate == 30) {
                            try {
                                serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(10, circuit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                CabinetRecord cabinetRecord1=new CabinetRecord();
                                cabinetRecord1.setMemberName(getResources().getString(R.string.manager));
                                cabinetRecord1.setPhoneNum("***********");
                                cabinetRecord1.setCabinetNumber(input +"");
                                cabinetRecord1.setCabinetStating(getResources().getString(R.string.manager_open));
                                cabinetRecord1.setOpentime(opentime);
                                realm.copyToRealm(cabinetRecord1);
                            }
                        });
                    } else {
                        Toast.makeText(SettingActivity.this, "柜号不存在", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SettingActivity.this, "请输入柜号", Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.cabinet_delect:
                if (!TextUtils.isEmpty(et_lockPlate.getText().toString().trim())) {
                    final RealmResults<CabinetNumber> cabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetLockPlate", et_lockPlate.getText().toString().trim()).findAll();
                    if (cabinetNumber.size()>0){
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                cabinetNumber.deleteAllFromRealm();
                            }
                        });
                    }else {
                        Toast.makeText(SettingActivity.this,"锁板号不存在",Toast.LENGTH_LONG).show();
                    }
                }else {
                    Toast.makeText(SettingActivity.this,"请输入锁板号",Toast.LENGTH_LONG).show();

                }
                break;
            case R.id.cabinet_repair:
                dialog=new MyExitAlertDialog(SettingActivity.this,"确定设置"+num+"号柜为维修？","1");
                dialog.show();
                break;
            case R.id.cabinet_isuse:
                dialog=new MyExitAlertDialog(SettingActivity.this,"确定设置"+num+"号柜为可用？","2");
                dialog.show();
                break;
            case R.id.open_lockPlate:
                if (!TextUtils.isEmpty(input)){
                    RealmResults<CabinetNumber> cabinetLockPlate = realm.where(CabinetNumber.class).equalTo("cabinetLockPlate", input).findAll();
                    if (cabinetLockPlate.size()>0){
                        if (Integer.parseInt(input)<=10){
                            for (int i = 0; i <= 10; i++) {
                                try {
                                    Thread.sleep(500);
                                    serialpprt_wk1.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(input), i));
                                } catch (Exception e) {
                                }
                            }
                        }else if (Integer.parseInt(input)<20&&Integer.parseInt(input)>10){
                            for (int i = 0; i <= 10; i++) {
                                try {
                                    Thread.sleep(500);
                                    serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(input)%10, i));
                                } catch (Exception e) {
                                }
                            }
                        }else if (Integer.parseInt(input)<30&&Integer.parseInt(input)>20){
                            for (int i = 0; i <= 10; i++) {
                                try {
                                    Thread.sleep(500);
                                    serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(Integer.parseInt(input)%10, i));
                                } catch (Exception e) {
                                }
                            }
                        }else if (Integer.parseInt(input)==20){
                            for (int i=0;i<=10;i++){
                                try {
                                    Thread.sleep(500);
                                    serialpprt_wk2.getOutputStream().write(openDoorUtil.openOneDoor(10, i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }else if (Integer.parseInt(input)==30){
                            for (int i=0;i<=10;i++){
                                try {
                                    Thread.sleep(500);
                                    serialpprt_wk3.getOutputStream().write(openDoorUtil.openOneDoor(10, i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }else {
                        Toast.makeText(SettingActivity.this,"请输入存在的锁板",Toast.LENGTH_LONG).show();
                    }
                }else {
                    Toast.makeText(SettingActivity.this,"请输入需要打开的锁板",Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.open_all:
                try {
                    serialPort =new SerialPort(new File("/dev/ttysWK1"),9600,0);
                    serialPort.getOutputStream().write(openDoorUtil.openAllDoor());
                }catch (IOException e){
                    e.printStackTrace();
                }
                try {
                    serialPort =new SerialPort(new File("/dev/ttysWK2"),9600,0);
                    serialPort.getOutputStream().write(openDoorUtil.openAllDoor());
                }catch (IOException e){
                    e.printStackTrace();
                }
                try {
                    serialPort =new SerialPort(new File("/dev/ttysWK3"),9600,0);
                    serialPort.getOutputStream().write(openDoorUtil.openAllDoor());
                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
            case R.id.check_record:
                if (!TextUtils.isEmpty(input)){
                    RealmResults<CabinetRecord> cabinetNumber = realm.where(CabinetRecord.class).equalTo("cabinetNumber", input).findAll();
                    if (cabinetNumber.size()>0){
                        CheckUsedRecored checkUsedRecored=new CheckUsedRecored(SettingActivity.this,cabinetNumber, input);
                        checkUsedRecored.show();
                    }else {
                        Toast.makeText(SettingActivity.this,"此柜号暂无操作记录",Toast.LENGTH_LONG).show();
                    }
                }else {
                    Toast.makeText(SettingActivity.this,"请输入查看的柜号",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.check_all:
                RealmResults<CabinetNumber> allCabinetNumber = realm.where(CabinetNumber.class).findAll();
                if (allCabinetNumber.size()>0) {
                    LockMessage lockMessage = new LockMessage(SettingActivity.this,allCabinetNumber);
                    lockMessage.show();
                }else {
                    Toast.makeText(SettingActivity.this,"请先添加柜号",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.head_text_02:
                exitAlertDialog=new ExitAlertDialog(SettingActivity.this);
                exitAlertDialog.show();
                break;
            case R.id.back:
                finish();
                break;
            case R.id.chang_pdw:
                pwdmodel="2";
                exitAlertDialog=new ExitAlertDialog(SettingActivity.this);
                exitAlertDialog.show();
                break;
            case R.id.clean_cabinetnum:
                String cbnum =et_input.getText().toString().trim();
                dialog=new MyExitAlertDialog(SettingActivity.this,"确定清除"+cbnum+"号柜？","3");
                dialog.show();
                break;
            case R.id.clean_all:
                dialog=new MyExitAlertDialog(SettingActivity.this,"确定清除所有柜子？","4");
                dialog.show();
                break;
            case R.id.open_app:
                Intent intent2 = getPackageManager().getLaunchIntentForPackage(getApplication().getPackageName());
                PendingIntent restartIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() , restartIntent); // 1秒钟后重启应用
                System.exit(0);
                break;
        }
    }
    String opentime;
    public class MesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            headText03Main.setText(intent.getStringExtra("timeStr"));
            opentime=intent.getStringExtra("timeStr");
            headText01.setText(intent.getStringExtra("timeData"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        unregisterReceiver(mesReceiver);
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
            texttilt=(TextView)view.findViewById(R.id.text_title);
            btCancel.setOnClickListener(this);
            btConfirm.setOnClickListener(this);
        }
        @Override
        public void show() {
            etPwd.setText("");
            if (pwdmodel=="1"){
            }else if (pwdmodel=="2"){
                texttilt.setText("修改密码");
                etPwd.setHint("请输入新密码");
            }
            super.show();
        }
        String devicepwd;
        SharedPreferences userInfo;
        Intent intent;
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btCancel:
                    this.dismiss();
                    break;
                case R.id.btConfirm:
                    if(pwdmodel.equals("1")){
                        String pwd = etPwd.getText().toString().trim();
                        if (TextUtils.isEmpty(pwd)) {
                            ToastUtils.show(mContext, "请输入密码", ToastUtils.LENGTH_SHORT);
                            return;
                        }
                            userInfo=getSharedPreferences("user_info",0);
                            String repwd = userInfo.getString("devicepwd","0");

                        if (!pwd.equals(repwd)) {
                            ToastUtils.show(mContext, "密码不正确", ToastUtils.LENGTH_SHORT);
                            return;
                        }else {
                            userInfo = getSharedPreferences("user_info", 0);
                            userInfo.edit().putString("devicepwd", pwd).commit();
                            adminmessage.setVisibility(View.VISIBLE);
                            this.dismiss();
                        }
                    }else if (pwdmodel.equals("2")){
                        userInfo=getSharedPreferences("user_info",0);
                        String pwd = etPwd.getText().toString().trim();
                        if (userInfo.getString("devicepwd","").toString().trim()==pwd) {
                            ToastUtils.show(mContext, "密码不能跟上一次相同", ToastUtils.LENGTH_SHORT);
                        }else {
                            userInfo.edit().putString("devicepwd",pwd).commit();
                            ToastUtils.show(mContext, "密码修改成功", ToastUtils.LENGTH_SHORT);
                            this.dismiss();
                        }
                    }
                    break;
            }
        }
    }
    String pwdmodel="0";
    private class MyExitAlertDialog extends Dialog implements View.OnClickListener{
        private Context mContext;
        private EditText etPwd;
        private Button btCancel;
        private Button btConfirm;
        private TextView texttilt;
        String title,text;

        public MyExitAlertDialog(Context context,int theme) {
            super(context,theme);
            mContext = context;
            this.text=text;
            initDialog();
        }
        public MyExitAlertDialog(Context context,String tiltstr,String text) {
            super(context, R.style.customer_dialog);
            mContext = context;
            this.title=tiltstr;
            this.text=text;
            initDialog();
        }
        private void initDialog() {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_exit, null);
            setContentView(view);
            btCancel = (Button) view.findViewById(R.id.btCancel);
            btConfirm = (Button) view.findViewById(R.id.btConfirm);
            texttilt=(TextView)view.findViewById(R.id.text_title);
            btCancel.setOnClickListener(this);
            btConfirm.setOnClickListener(this);
        }
        @Override
        public void show() {
            texttilt.setText(title);
            super.show();
        }
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btCancel:
                    this.dismiss();
                    break;
                case R.id.btConfirm:
                    if ("1".equals(text)){
                        cabinet_repair();
                    }else if("2".equals(text)){
                        cabinet_isuse();
                    }
                    else if("3".equals(text)){
                        clean_cabinetnum();
                    }else if ("4".equals(text)){
                        clean_all();
                    }
                    this.dismiss();
                    break;
            }
        }
    }
    private void clean_all(){
        final RealmResults<CabinetNumber> allUseCabinetNumber = realm.where(CabinetNumber.class).equalTo("isUser", "使用中").findAll();
        if (allUseCabinetNumber.size()>0){
            final RealmResults<Person> allPerson = realm.where(Person.class).equalTo("fingerId",1).findAll();
            if(allPerson.size()>0){
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        allPerson.deleteAllFromRealm();
                    }
                });
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    while (allUseCabinetNumber.size()>0){
                        allUseCabinetNumber.get(0).setIsUser("可用");
                    }
                }
            });
        }else {

        }
    }
    private void clean_cabinetnum(){
        String cbnum =et_input.getText().toString().trim();
        final RealmResults<CabinetNumber> clearCabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetNumber", cbnum).findAll();
        if (clearCabinetNumber.size()>0){
            if (!"维修".equals(clearCabinetNumber.get(0).getIsUser())){
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        clearCabinetNumber.get(0).setIsUser("可用");
                        realm.copyToRealm(clearCabinetNumber.get(0));
                    }
                });
                final CabinetRecord cabinetRecord = new CabinetRecord();
                cabinetRecord.setOpentime(opentime);
                cabinetRecord.setCabinetNumber(input);
                cabinetRecord.setCabinetStating("清柜");
                cabinetRecord.setMemberName("管理员");
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealm(cabinetRecord);
                    }
                });
            }else {
                Toast.makeText(SettingActivity.this,"该柜号处于维修中不可用",Toast.LENGTH_LONG).show();
            }
            final RealmResults<Person> uid = realm.where(Person.class).equalTo("uid", cbnum).findAll();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    uid.deleteAllFromRealm();
                }
            });
        }else {
            Toast.makeText(SettingActivity.this,"该柜号不存在",Toast.LENGTH_LONG).show();
        }
    }
    private void cabinet_isuse (){
        if (!TextUtils.isEmpty(num)){
            final RealmResults<CabinetNumber> clearCabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetNumber", num).findAll();
            if (clearCabinetNumber.size()>0){
                if (!"使用中".equals(clearCabinetNumber.get(0).getIsUser())){
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            clearCabinetNumber.get(0).setIsUser("可用");
                            realm.copyToRealm(clearCabinetNumber.get(0));
                        }
                    });
                    final CabinetRecord cabinetRecord = new CabinetRecord();
                    cabinetRecord.setOpentime(opentime);
                    cabinetRecord.setCabinetNumber(input);
                    cabinetRecord.setCabinetStating("可用");
                    cabinetRecord.setMemberName("管理员");
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealm(cabinetRecord);
                        }
                    });}else{
                    Toast.makeText(SettingActivity.this,"该柜号使用中，不可设置为可用",Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(SettingActivity.this,"该柜号不存在，请输入已存在的柜号",Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(SettingActivity.this,"请输入柜号",Toast.LENGTH_LONG).show();
        }
    }
    private void cabinet_repair(){
        num=etCabinetnum.getText().toString().trim();
        if (!TextUtils.isEmpty(num)){

            final RealmResults<CabinetNumber> clearCabinetNumber = realm.where(CabinetNumber.class).equalTo("cabinetNumber", num).findAll();
            if (clearCabinetNumber.size()>0){
                if (!"使用中".equals(clearCabinetNumber.get(0).getIsUser())){
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            clearCabinetNumber.get(0).setIsUser("维修");
                            realm.copyToRealm(clearCabinetNumber.get(0));
                        }
                    });
                    final CabinetRecord cabinetRecord = new CabinetRecord();
                    cabinetRecord.setOpentime(opentime);
                    cabinetRecord.setCabinetNumber(input);
                    cabinetRecord.setCabinetStating("维修");
                    cabinetRecord.setMemberName("管理员");
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealm(cabinetRecord);
                        }
                    });}else {
                    Toast.makeText(SettingActivity.this,"该柜号使用中，不可设置为维修",Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(SettingActivity.this,"该柜号不存在，请输入已存在的柜号",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(SettingActivity.this,"请输入柜号",Toast.LENGTH_LONG).show();
        }
    }

}
