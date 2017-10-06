package com.example.baidu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;
/*
需求：完成一些基本的地图操作
操作一：显示当前纬度，精度，和能看懂的位置信息
步骤：
1）配置一些必备的文件和导入该有的包
2）获取LocationClient对象，传入context变量
3）给LocationClient绑定监听器，该监听器用于当返回具体的数据之后，会进行自动调用
4）写好监听器
5）启动LocationClient的start方法，从而请求定位功能
注意：这边涉及到几个危险权限，所以需要动态申请，且因为是多个，所以使用到集合，进行多个危险权限的同时申请

操作二：完成进行定时重复请求定位功能
步骤：
1）获取LocationClientOption对象
2）设置好间隔时间
3）将其绑定在LocationClient中
4）重写onDestroy()，并在内部写好mLocationClient.stop()方法，因为如果没有调用这个方法
，那么其会在后天一直请求定位，大量的耗电。
注意：如果没有设置1）对象，那么当LocationClient调用了start方法之后，只会请求一次定位功能，
并不会像这边如果没关闭，就会大量的耗电。

操作三：选择定位模式
1）获取LocationClientOption对象
2）调用该对象的setLocationMode()方法设置定位模式

操作四：让地图显示出来
1）写好Mapview控件，注意，这个是百度提供的自定义控件，需要将完整的包名加上。
2）调用SDKInitializer的initialize()方法，并且传入context来进行初始化操作。注意：其要在setContentView()方法之间进行调用，不然会出错。
3）获取mapview实例
4）重写onResume(),onPause(),onDestroy()方法进行对mapview管理

//这两个操作其实都是对baidumap进行操作

操作五：移动到我的位置
1）利用mapview得到BaiduMap对象
2）获取LatLng对象，传入纬度和经度
3）设置好属性，从而获取MapStatusUpdate对象，然后baidumap对象调用animateMapStatus（），并且传入
设置好更新属性的MapStatusUpdata对象

操作六：让我显示在地图上（小红点）
1）调用baidumap的setMyLocationEnabled(true)
2)获取MyLocationData.Builder对象并传入经纬度
3）调用MyLocationData.Builder的build()方法从而获取MyLocationData对象
4）调用baidumap.setMyLocationData()方法，并且传入MylocationData变量
5）在onDestroy中，使得baidumap的setMyLocationEnabled(true)

 */
public class MainActivity extends AppCompatActivity {
    public LocationClient mLocationClient;
    private TextView positionText;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView=(MapView)findViewById(R.id.bmapView);
        baiduMap=mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        positionText=(TextView)findViewById(R.id.position_text_view);
        //一次性注册多个动态权限
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
        permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.
        permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private void navigateTo(BDLocation location){
        if(isFirstLocate){
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update=MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate=false;
        }
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }
    public class MyLocationListener implements BDLocationListener{
        public void onReceiveLocation(BDLocation location){
        /*
            StringBuilder currentPosition=new StringBuilder();

            currentPosition.append("纬度: ").append(location.getLatitude()).append("\n");
            currentPosition.append("经度： ").append(location.getLongitude()).append("\n");
            currentPosition.append("国家：").append(location.getCountry()).append("\n");
            currentPosition.append("省：").append(location.getProvince()).append("\n");
            currentPosition.append("市：").append(location.getCity()).append("\n");
            currentPosition.append("区：").append(location.getDistrict()).append("\n");
            currentPosition.append("街道：").append(location.getStreet()).append("\n");
            currentPosition.append("定位方式： ");
            if(location.getLocType()==BDLocation.TypeGpsLocation){
                currentPosition.append("GPS");
            }else  if(location.getLocType()==BDLocation.TypeNetWorkLocation){
                currentPosition.append("网络");
            }
            positionText.setText(currentPosition);
            */

            if(location.getLocType()==BDLocation.TypeGpsLocation
                    ||location.getLocType()==BDLocation.TypeNetWorkLocation){
                navigateTo(location);
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }
    protected void onDestroy(){
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }
}
