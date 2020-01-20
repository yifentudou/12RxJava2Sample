package com.potato.rxjavasample;

/**
 * Created by li.zhirong on 2020/1/20
 */
public class WeatherEnity {

    private WeatherInfo weatherInfo;

    public WeatherInfo getWeatherinfo() {
        return weatherInfo;
    }

    public class WeatherInfo {

        private String city;
        private String temperature;
        private String oritation;
        private String speed;

        public String getCity() {
            return city;
        }

        public String getTemp() {
            return temperature;
        }

        public String getWD() {
            return oritation;
        }

        public String getWS() {
            return speed;
        }
    }
}
