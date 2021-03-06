package cn.androiders.simpleweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;

import cn.androiders.simpleweather.Gson.Forecast;
import cn.androiders.simpleweather.Gson.Weather;
import cn.androiders.simpleweather.Service.AutoUpdateService;
import cn.androiders.simpleweather.Utils.HttpUtil;
import cn.androiders.simpleweather.Utils.Utility;
import cn.androiders.simpleweather.Utils.WeatherURL;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    public DrawerLayout drawerLayout;
    private Button drawerHomeButton;



    private ScrollView weahterLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;

    private TextView apiTextView;
    private TextView pm25TextView;

    private TextView comfortTextView;
    private TextView carwashTextView;
    private TextView sportTextView;

    private ImageView bingPicImg;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        findView();

        SharedPreferences pfs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
        String weatherString = pfs.getString("weather", null);
        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            String mWeahterId = getIntent().getStringExtra("weatherId");
            weahterLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeahterId);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        drawerHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.START);
            }
        });

        String bingPic= pfs.getString("bing_pic", null);
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }
    }

    private void loadBingPic() {
        String url = WeatherURL.BING_PIC_URL;
        HttpUtil.httpSendRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPicBodyString = response.body().string();
                SharedPreferences.Editor pfsEditor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                pfsEditor.putString("bingPic", bingPicBodyString);
                pfsEditor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPicBodyString).into(bingPicImg);
                    }
                });

            }
        });
    }

    public void requestWeather(final String weahterId) {
        String url = WeatherURL.WEATHER_URL + "&city=" + weahterId;
        //Log.d(TAG, "requestWeather: " + url);
        HttpUtil.httpSendRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseBody = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseBody);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if( weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseBody);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }

                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

        loadBingPic();
    }


    private void findView() {
        weahterLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        apiTextView = (TextView) findViewById(R.id.api_text);
        pm25TextView = (TextView) findViewById(R.id.pm25_text);

        comfortTextView = (TextView) findViewById(R.id.comfort_text);
        carwashTextView = (TextView) findViewById(R.id.car_wash_text);
        sportTextView = (TextView) findViewById(R.id.sport_text);

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerHomeButton = (Button) findViewById(R.id.draw_home);


    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        String comfortText = "舒适度：" + weather.suggestion.comfort.info;
        String carWashText = "洗车建议：" + weather.suggestion.carwash.info;
        String sportText = "运动建议：" + weather.suggestion.sport.info;


        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }

        if(weather.aqi != null) {
            String apiText = weather.aqi.city.aqi;
            String pm25text = weather.aqi.city.pm25;
            apiTextView.setText(apiText);
            pm25TextView.setText(pm25text);
        }

        if(weather.suggestion != null) {
            comfortTextView.setText(comfortText);
            carwashTextView.setText(carWashText);
            sportTextView.setText(sportText);
        }


        weahterLayout.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

}
