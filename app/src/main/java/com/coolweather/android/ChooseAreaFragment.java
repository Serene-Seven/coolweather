package com.coolweather.android;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2018/2/11.
 * 这是个能遍历全国省市县数据的碎片，碎片是不能直接显示在页面上的，需要将其添加到活动里。
 * 这里添加到activity_main.xml布局中作为类使用，
 * 于是启动MainActivity活动的时候，会渲染activity_main布局，然后会使用到这个碎片。
 * 于是碎片启动，使用onCreateView，在这个函数再使用choose_area.xml布局。
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    //记录当前所选的位置信息
    private Province selectedProvince;
    private City selectedCity;
    //记录当前在哪个页面
    private int currentLevel;
    private final String TAG = "ChooseAreaFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        //获取实例
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        //给listview的适配器绑定数据并且配置好
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //接下来这两个点击监听函数能造成一种，同一个页面上加载出省市县三个页面的效果。
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //点到条目的时候判断当前是在省级界面还是市级界面，从而捕获到点到的那个条目的信息。然后进入下一层界面。
                if (currentLevel == LEVEL_PROVINCE) {
                    Log.d(TAG, "onItemClick: woa");
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //同上，点到回退图标的时候判断当前是在市级界面还是县级页面，从而回退到上一层界面。
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    //查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
    private void queryProvinces() {
        //展示省级数据的时候标题为中国，且由于处于在顶层级别所以不显示回退按钮
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        //查询数据库当中所有保存的province类对象
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                //页面上只显示省的名字就够了，所以省列表只插入名字
                dataList.add(province.getProvinceName());
            }
            //刷新页面
            adapter.notifyDataSetChanged();
            //让第一项数据显示在页面最上面，因为插入的数据足够多超出页面后，页面刷新会自动往下滚动，所以要重置页面定位。
            listView.setSelection(0);
            //记录当前页面的级别。
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
        Log.d(TAG, "queryProvinces: pumu");
    }

    //查询选中的省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        //查询数据库当中，所有保存的，provinceid为选中的省的id的，市级类对象
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                //页面上只显示市的名字就够了，所以市列表只插入名字
                dataList.add(city.getCityName());
            }
            //刷新页面
            adapter.notifyDataSetChanged();
            //让第一项数据显示在页面最上面，因为插入的数据超出页面后，页面刷新会自动往下滚动，所以要重置页面定位。
            listView.setSelection(0);
            //记录当前页面的级别。
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    //查询选中的市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //查询数据库当中，所有保存的，cityid为选中的市的id的，县级类对象
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                //页面上只显示县的名字就够了，所以县列表只插入名字
                dataList.add(county.getCountyName());
            }
            //刷新页面
            adapter.notifyDataSetChanged();
            //让第一项数据显示在页面最上面，因为插入的数据超出页面后，页面刷新会自动往下滚动，所以要重置页面定位。
            listView.setSelection(0);
            //记录当前页面的级别。
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县的数据
    private void queryFromServer(String address, final String type) {
        //开启通知窗口
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //请求失败也要记得关掉通知窗口。
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败", Toast.LENGTH_SHORT);
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                //根据不同类型选择不同解析函数
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    //请求的是city数据，那么这些数据都要打上其对应的省的id
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    //请求的是county数据，那么这些数据都要打上其对应的市的id
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //查询结束，关掉通知窗口，根据所查类型跳转
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    //显示进度会话框
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            //不准取消这个会话框
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度会话框
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
