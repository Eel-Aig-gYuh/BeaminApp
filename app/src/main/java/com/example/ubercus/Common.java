package com.example.ubercus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.ArraySet;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.example.ubercus.Model.AnimationModel;
import com.example.ubercus.Model.CustomerInfoModel;
import com.example.ubercus.Model.DriverGeoModel;
import com.example.ubercus.Model.DriverInfoModel;
import com.example.ubercus.Services.MyFireBaseMessagingService;
import com.google.android.gms.common.api.DataBufferResponse;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolygonOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Common {
    public static final String CUSTOMER_INFO_REFERENCE = "Customers";
    public static final String CUSTOMERS_LOCATION_REFERENCES = "CustomersLocation";


    public static final String DRIVERS_INFO_REFERENCES = "Drivers";
    public static final String DRIVERS_LOCATION_REFERENCES = "availableDriver";


    public static final String TOKEN_REFERENCE = "Token";
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT = "body";

    public static CustomerInfoModel currenCustomer;
    public static DriverInfoModel currenDriver;
    public static Set<DriverGeoModel> driversFound = new ArraySet<DriverGeoModel>();
    public static HashMap<String, Marker> makerList = new HashMap<>();
    public static HashMap<String, AnimationModel> driverLocationSubscribe = new HashMap<String, AnimationModel>();

    public static String buildWelcomeMessage(){
        if (Common.currenCustomer != null)
            return new StringBuilder("Xin chào ")
                    .append(Common.currenCustomer.getFirstName())
                    .append(" ")
                    .append(Common.currenCustomer.getLastName()).toString();
        else
            return "";
    }
    public static String buildDriverWelcomeMessage() {
        if (Common.currenDriver != null)
            return new StringBuilder("Xin chào ")
                    .append(Common.currenDriver.getFirstName())
                    .append(" ")
                    .append(Common.currenDriver.getLastName()).toString();
        else
            return "";
    }

    public static void showNotification(Context context, int id,
                                        String title, String body, Intent intent) {
        PendingIntent pendingIntent = null;
        if (intent!=null)
            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "gia_huy_uber";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Uber remake", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Uber Remake");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.baseline_car_crash_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.baseline_car_crash_24));
        if (pendingIntent != null){
            builder.setContentIntent(pendingIntent);
        }
        Notification notification = builder.build();
        notificationManager.notify(id, notification);
    }

    public static String buildName(String firstName, String lastName) {
        return new StringBuilder(firstName).append(" ").append(lastName).toString();
    }

    //DECODE POLY
    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index=0,len=encoded.length();
        int lat=0,lng=0;
        while(index < len)
        {
            int b,shift=0,result=0;
            do{
                b=encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift+=5;

            }while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift +=5;
            }while(b >= 0x20);
            int dlng = ((result & 1)!=0 ? ~(result >> 1): (result >> 1));
            lng +=dlng;

            LatLng p = new LatLng((((double)lat / 1E5)),
                    (((double)lng/1E5)));
            poly.add(p);
        }
        return poly;
    }

    public static float getBearing(LatLng begin, LatLng end) {
        //You can copy this function by link at description
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    public static void setWelcomeMessage(TextView txtWelcome) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 1 && hour <= 12){
            txtWelcome.setText(new StringBuilder("Buổi sáng vui vẻ! Bạn muốn đi đâu?"));
        }
        else if (hour >= 13 && hour <= 17){
            txtWelcome.setText(new StringBuilder("Buổi chiều vui vẻ! Bạn muốn đi đâu?"));
        }
        else {
            txtWelcome.setText(new StringBuilder("Buổi tối vui vẻ! Bạn muốn đi đâu?"));
        }
    }
}