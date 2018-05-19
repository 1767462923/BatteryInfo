package com.liming.batteryinfo.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import com.liming.batteryinfo.R;
import com.liming.batteryinfo.adapter.ToolAdapter;
import com.liming.batteryinfo.entity.ToolBean;
import com.liming.batteryinfo.utils.BatteryUtil;
import com.liming.batteryinfo.utils.RootCmd;
import com.liming.batteryinfo.utils.SystemInfo;
import com.liming.batteryinfo.utils.ViewInject;
import com.liming.batteryinfo.view.BatteryView;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    @ViewInject(R.id.linearLayout)
    LinearLayout linearLayout;
    @ViewInject(R.id.battery_current)
    TextView battery_current;
    @ViewInject(R.id.battery_currenttip)
    TextView battery_currenttip;
    @ViewInject(R.id.device_name)
    TextView device_name;
    @ViewInject(R.id.sjbattery)
    TextView sjbattery;
    @ViewInject(R.id.xdbattery)
    TextView xdbattery;
    @ViewInject(R.id.device_nametip)
    TextView device_nametip;
    @ViewInject(R.id.batteryviewtip)
    TextView batteryviewtip;
    @ViewInject(R.id.temp)
    TextView temp;
    @ViewInject(R.id.voltage)
    TextView voltage;
    @ViewInject(R.id.gridview)
    GridView gridView;
    @ViewInject(R.id.batteryview)
    private BatteryView verticalBattery;
    @ViewInject(R.id.abouttip)
    TextView abouttip;
    ToolAdapter toolAdapter;
    static int charge_current_max;
    static int quantity;
    static int health;
    public static int color;
    private static int []colors={
            R.color.app_color_theme_1,
            R.color.app_color_theme_2,
            R.color.app_color_theme_3,
            R.color.app_color_theme_4,
            R.color.app_color_theme_5,
            R.color.app_color_theme_6,
            R.color.app_color_theme_7,
            R.color.app_color_theme_8,
            R.color.app_color_theme_9
    };
    static int stopnum=101;
    static int stopdo=0;
    final ArrayList<ToolBean> listItemArrayList=new ArrayList<ToolBean>();
    Handler mTimeHandler=new Handler(){
        public void handleMessage(Message message) {
            super.handleMessage(message);
            if (message.what == 0) {
                initViews();
                sendEmptyMessageDelayed(0,1500);
            }
            if (message.what == 1) {
                if (color>=colors.length-1){
                    startAnimation(linearLayout,colors[colors.length-1],colors[0]);
                    color=0;
                }else {
                    startAnimation(linearLayout,colors[color],colors[++color]);
                }
                sendEmptyMessageDelayed(1,3000);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==1){
            stopnum=data.getIntExtra("stopnum",101);
            stopdo=data.getIntExtra("stopdo",1);
            listItemArrayList.get(3).setItemTip(data.getStringExtra("memo"));
            listItemArrayList.get(3).setItemNum(String.valueOf(stopnum)+"%");
            toolAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimeHandler.sendEmptyMessage(0);
        mTimeHandler.sendEmptyMessage(1);
        mTimeHandler.sendEmptyMessage(2);
        Typeface mtypeface=Typeface.createFromFile("/system/fonts/Roboto-Thin.ttf");
        batteryviewtip.setTypeface(mtypeface);
        temp.setTypeface(mtypeface);
        voltage.setTypeface(mtypeface);
        abouttip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,AboutActivity.class));
            }
        });

        listItemArrayList.add(new ToolBean("电池健康程度（仅供参考）","数据不准确？点击进行电量校准",SystemInfo.getHealthy()+"%"));
        listItemArrayList.add(new ToolBean("电池电量百分比","点击可修改电量百分比及充电状态",SystemInfo.getQuantity(this)+"%"));
        listItemArrayList.add(new ToolBean("电池充电最大电流","点击可突破限制",SystemInfo.getConstant_charge_current_max()+"mA"));
        listItemArrayList.add(new ToolBean("定量停冲(实验性)","电量冲至指定电量禁止充电",stopnum+"%"));
        //生成适配器的ImageItem 与动态数组的元素相对应
        toolAdapter = new ToolAdapter(this,listItemArrayList);
        //添加并且显示
        gridView.setAdapter(toolAdapter);
        //添加消息处理
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        if (SystemInfo.getQuantity(MainActivity.this)==100){
                            new AlertDialog.Builder(MainActivity.this).setTitle("温馨提示")//设置对话框标题
                                    .setMessage("确定要清除电池信息吗？清除成功后会自动重启！")//设置显示的内容
                                    .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                            if (RootCmd.execRootCmdSilent("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;reboot;\n",true)!=-1){
                                                Toast.makeText(MainActivity.this, "清空电池信息成功", Toast.LENGTH_SHORT).show();
                                            }else {
                                                Toast.makeText(MainActivity.this, "清空电池信息失败", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }).setNegativeButton("取消",null).show();//在按键响应事件中显示此对话框
                        }else {
                            Toast.makeText(MainActivity.this,"请将电量充满再执行操作",Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        Intent intent=new Intent(MainActivity.this,BatterySettingActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this,MaxCurrentSettingActivity.class));
                        break;
                    case 3:
                        startActivityForResult(new Intent(MainActivity.this,TimeChargeActivity.class).putExtra("stopnum",stopnum),1);
                        break;
                    default:
                        Toast.makeText(getBaseContext(),listItemArrayList.get(position).getItemTool().toString()+"功能加紧开发中",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        initViews();
    }

    private static int power;
    private void initViews() {
        device_name.setText(SystemInfo.getBrand()+" "+SystemInfo.getModel());
        device_nametip.setText(SystemInfo.getDevice()+" Android"+SystemInfo.getRelease()+" SDK"+SystemInfo.getSdk());
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start=System.currentTimeMillis();
                final int  current=SystemInfo.getCurrent();
                final Double tempstr=SystemInfo.getTemp();
                final int voltagenum=SystemInfo.getVoltage();
                final int charge_full_design=SystemInfo.getCharge_full_design();
                final int charge_full=SystemInfo.getCharge_full();
                final int cycle_count=SystemInfo.getCycle_count();
                final int charge_current_max=SystemInfo.getConstant_charge_current_max();
                final int quantity=SystemInfo.getQuantity(getBaseContext());
                final int health=(charge_full==0?0:100*charge_full/charge_full_design);
              //向Handler发送处理操作
                mTimeHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        long start2=System.currentTimeMillis();
                        sjbattery.setText("设计容量"+SystemInfo.getCharge_full_design()+"mA");
                        xdbattery.setText("实际容量"+SystemInfo.getCharge_full()+"mA");
                        temp.setText(tempstr+"℃");
                        voltage.setText(voltagenum+"mV");
                        batteryviewtip.setText(quantity+"%");
                        battery_current.setText(current+("mA"));
                        battery_currenttip.setText(((current>0) ? "正在充电" : "正在放电")+" 循环充电"+cycle_count+"次");
                        if (quantity!=100&&current>0){
                            if (power>=100){power=0;}
                            verticalBattery.setPower(power+=5);
                        }else {
                            verticalBattery.setPower(quantity);
                        }
                        if (MainActivity.health!=health||MainActivity.charge_current_max!=charge_current_max||MainActivity.quantity!=quantity){
                            MainActivity.health=health;
                            MainActivity.charge_current_max=charge_current_max;
                            MainActivity.quantity=quantity;
                            listItemArrayList.get(0).setItemNum(health+"%");
                            listItemArrayList.get(1).setItemNum(quantity+"%");
                            listItemArrayList.get(2).setItemNum(charge_current_max+"mA");
                            listItemArrayList.get(3).setItemNum(stopnum+"%");
                            toolAdapter.notifyDataSetChanged();
                            Log.d(TAG, "---------------run: 正在跟新列表");
                        }
                        if (quantity>=stopnum){
                            boolean bool=BatteryUtil.disbleCharge();
                            if (bool&&stopdo==0){
                                System.exit(1);
                            }else if (bool&&stopdo==1){
                                RootCmd.execRootCmdSilent("reboot -p\n",true);
                            }else if (bool&&stopdo==2){
                                RootCmd.execRootCmdSilent("root\n",true);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void startAnimation(final View view, int color1, int color2) {
        //创建动画,这里的关键就是使用ArgbEvaluator, 后面2个参数就是 开始的颜色,和结束的颜色.
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), getResources().getColor(color1),  getResources().getColor(color2));
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setBackgroundColor((int)animation.getAnimatedValue());//设置一下, 就可以看到效果.
            }
        });
        colorAnimator.setDuration(3000);
        colorAnimator.start();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.exit(1);
    }
}
