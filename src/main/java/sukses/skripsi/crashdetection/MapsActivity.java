package sukses.skripsi.crashdetection;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import com.google.firebase.database.Query;

import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.firebase.database.ValueEventListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivity extends FragmentActivity  implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, SensorEventListener {
    private Button btnLogout;
    private TextView txvSpeed,txvResultan,txvHasil,txvName;
    LocationRequest mLocationRequest;
    private String setLongitude;
    private String setLatitude;
    private String userName;
    private SensorManager sensorManager;
    Sensor accelerometer;


    private ArrayList<Integer> speedList = new ArrayList<>();
    private List<Integer> speedSubList;
    private List<Double> atSubList;
    private int speedTimer;
    private double sdSpeeds;
    private double difATt;
    private String uId;

    private ArrayList<Double>resultan = new ArrayList<>();

    private static final int PERMISSION_SEND_SMS = 1;

    private ArrayList<String>phoneNumber = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        txvName = findViewById(R.id.name);
        txvHasil = findViewById(R.id.status);
        btnLogout =  findViewById(R.id.btnLogout);
        txvResultan = findViewById(R.id.resultan);
        txvSpeed = findViewById(R.id.speed);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        }

        DatabaseReference mDbContact;
        mDbContact = FirebaseDatabase.getInstance().getReference("contact").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        Query qDbContact = mDbContact.orderByChild("NoHp");
        qDbContact.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                phoneNumber.clear();
                    for(DataSnapshot phoneNumberSnapshot : dataSnapshot.getChildren()) {
                        Users users = phoneNumberSnapshot.getValue(Users.class);
                        String number = users.getNoHp();
                        phoneNumber.add(number);
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference mUsersName;
        mUsersName = FirebaseDatabase.getInstance().getReference("users");
        Query qName = mUsersName.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        qName.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("Name").getValue(String.class);
                txvName.setText("Welcome :" +name);
                setUserName(name);
                uId = dataSnapshot.getKey();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        startLocationUpdates();

        //sensor accelerometer
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MapsActivity.this, accelerometer,15000);

        //timer for get last 10 seconds of speed
        CountUpTimer ct = new CountUpTimer(7200000) {
            @Override
            public void onTick(int second) {
                speedList.add(getSpeedTimer());
                speedSubList = speedList.subList(Math.max(speedList.size() - 10,0),speedList.size());
                double sdSpeed = standarDeviation(speedSubList);
                setSdSpeeds(sdSpeed);
            }
        };
        ct.start();
    }

    //send sms
    private void send(String phoneNumber){
            String name = getUserName();
            String location = " Mengalami Kecelakaan di : ";
            String longitude = getSetLongitude();
            String latitude = getSetLatitude();
            String longUrl = "http://www.google.com/maps/place/"+latitude+","+longitude;
            Uri path = Uri.parse(longUrl);
            String message = name+location+path;

            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(DELIVERED), 0);

            //---Saat SMS terkirim
            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS sent",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Toast.makeText(getBaseContext(), "Generic failure",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Toast.makeText(getBaseContext(), "No service",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Toast.makeText(getBaseContext(), "Null PDU",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Toast.makeText(getBaseContext(), "Radio off",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(SENT));

            //---when the SMS has been delivered---
            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS delivered",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getBaseContext(), "SMS not delivered",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(DELIVERED));

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    private void startLocationUpdates() {
        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1);
        mLocationRequest.setFastestInterval(1);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }
    public void onLocationChanged(Location location) {
        String slati = String.valueOf(Double.toString(location.getLatitude()));
        String slongit = String.valueOf(Double.toString(location.getLongitude()));

        setSetLatitude(slati);
        setSetLongitude(slongit);

        //untuk speed
        int speed = (int)((location.getSpeed()*3600)/1000);
        txvSpeed.setText(speed + " km/h");
        setSpeedTimer(speed);
    }

    public static double standarDeviation(List<Integer> speed){
        double sum = 0.0;
        double standardDeviation = 0.0;
        int length = speed.size();
        for(double num : speed){
            sum += num;
        }

        double mean = sum/length;

        for(double num : speed){
            standardDeviation += Math.pow(num-mean,2);
        }
        return Math.sqrt(standardDeviation/length);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (checkPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                return;
            }
            googleMap.setMyLocationEnabled(true);
        }
    }
    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double Accx = sensorEvent.values[0];
        double Accy = sensorEvent.values[1];
        double Accz = sensorEvent.values[2];

        double kuadratX = Accx * Accx;
        double kuadratY = Accy * Accy;
        double kuadratZ = Accz * Accz;
        double rAT = Math.sqrt(kuadratX+kuadratY+kuadratZ);
        DecimalFormat dat = new DecimalFormat("#.##");
        double ATt = Double.parseDouble(dat.format(rAT));

        resultan.add(ATt);

        atSubList = resultan.subList(Math.max(resultan.size() - 1000,0),resultan.size());
        setDifATt(Collections.max(atSubList) - Collections.min(atSubList));

        String AT = String.valueOf(ATt);
        txvResultan.setText("Resultan :" +AT);

        accidentProbability();
    }

    public void accidentProbability(){
        double h1 = 0.5; //hipotesis terjadi kecelakaan tanpa memandang evidence apapun
        double h2 = 0.5; //hipotesis terjadi tidak kecelakaan tanpa memandang evidence apapun
        double e1h1 = 0.03; //Probabilitas terjadi kecelakaan jika Standar deviasi kecepatan dari 2,7 sampai 3,743141877 (2,7 <=x<=3,743141877)
        double e1h2 = 0.10; //Probabilitas tidak terjadi kecelakaan jika Standar deviasi kecepatan dari 2,7 sampai 3,743141877 (2,7 <=x<=3,743141877)
        double e2h1 = 0.96; //Probabilitas terjadi kecelakaan jika Standar deviasi kecepatan lebih dari 3,743141877
        double e2h2 = 0.04; //Probabilitas tidak terjadi kecelakaan jika Standar deviasi kecepatan lebih dari 3,743141877
        double e3h1 = 0.10; //Probabilitas terjadi kecelakaan jika Standar deviasi kecepatan kurang dari 2,7
        double e3h2 = 0.90; //Probabilitas tidak terjadi kecelakaan jika Standar deviasi kecepatan kurang dari 2,7
        double e4h1 = 0.33; //Probabilitas terjadi kecelakaan jika Selisih max dan min resultan akselerometer dari 21,89 sampai 26,07 (21,89 <=x<=26,07)
        double e4h2 = 0.33; //Probabilitas tidak terjadi kecelakaan jika Selisih max dan min resultan akselerometer dari 21,89 sampai 26,07 (21,89 <=x<=26,07)
        double e5h1 = 0.66; //Probabilitas terjadi kecelakaan jika Selisih max dan min resultan akselerometer lebih dari 26,07
        double e5h2 = 0.34; //Probabilitas tidak terjadi kecelakaan jika Selish max dan min resultan akselerometer lebih dari 26,07
        double e6h1 = 0.10; //Probabilitas terjadi kecelakaan jika Selisih max dan min resultan akselerometer kurang dari 21,89
        double e6h2 = 0.90; //Probabilitas tidak terjadi kecelakaan jika Selisih max dan min resultan akselerometer kurang dari 21,89
        double countH1;
        double countH2;
        double resultH1; //terjadi kecelakaan
        double resultH2; //tidak terjadi kecelakaan
        String P1 = "Terjadi Kecelakaan";
        String P2 = "Tidak Terjadi Kecelakaan";

        double stdSpeed = getSdSpeeds();
        double difAT = getDifATt();

        if(stdSpeed >= 2.75 && stdSpeed <=3.74 && difAT >=21.89 && difAT <= 26.07){ //e1 & e4
            countH1 = e1h1*e4h1*h1;
            countH2 = e1h2*e4h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed >= 2.75 && stdSpeed <=3.74 && difAT > 26.07){ //e1 & e5
            countH1 = e1h1*e5h1*h1;
            countH2 = e1h2*e5h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed >= 2.75 && stdSpeed <=3.74 && difAT < 21.89){ //e1 & e6
            countH1 = e1h1*e6h1*h1;
            countH2 = e1h2*e6h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed > 3.74 && difAT >=21.89 && difAT <= 26.07){ //e2 & e4
            countH1 = e2h1*e4h1*h1;
            countH2 = e2h2*e4h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed > 3.74 && difAT > 26.07){ //e2 & e5
            countH1 = e2h1*e5h1*h1;
            countH2 = e2h2*e5h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed > 3.74 && difAT < 21.89){ //e2 & e6
            countH1 = e2h1*e6h1*h1;
            countH2 = e2h2*e6h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed < 2.75 && difAT >=21.89 && difAT <= 26.07){ //e3 & e4
            countH1 = e3h1*e4h1*h1;
            countH2 = e3h2*e4h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed < 2.75 && difAT > 26.07){ //e3 & e5
            countH1 = e3h1*e5h1*h1;
            countH2 = e3h2*e5h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(stdSpeed < 2.75 && difAT < 21.89){ //e3 & e6
            countH1 = e3h1*e6h1*h1;
            countH2 = e3h2*e6h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT >=21.89 && difAT <= 26.07 && stdSpeed >= 2.75 && stdSpeed <=3.74){ //e4 & e1
            countH1 = e4h1*e1h1*h1;
            countH2 = e4h2*e1h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT >=21.89 && difAT <= 26.07 && stdSpeed > 3.74){ //e4 &e2
            countH1 = e4h1*e2h1*h1;
            countH2 = e4h2*e2h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT >=21.89 && difAT <= 26.07 && stdSpeed < 2.75){ //e4 & e3
            countH1 = e4h1*e3h1*h1;
            countH2 = e4h2*e3h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT > 26.07 && stdSpeed >= 2.75 && stdSpeed <=3.74){ //e5 & e1
            countH1 = e5h1*e1h1*h1;
            countH2 = e5h2*e1h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT > 26.07 && stdSpeed > 3.74){ //e5 & e2
            countH1 = e5h1*e2h1*h1;
            countH2 = e5h2*e2h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT > 26.07 && stdSpeed < 2.75){ //e5 & e3
            countH1 = e5h1*e3h1*h1;
            countH2 = e5h2*e3h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT < 21.89 && stdSpeed >= 2.75 && stdSpeed <=3.74){ //e6 & e1
            countH1 = e6h1*e1h1*h1;
            countH2 = e6h2*e1h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT < 21.89 && stdSpeed > 3.74){ //e6 & e2
            countH1 = e6h1*e2h1*h1;
            countH2 = e6h2*e2h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }else if(difAT < 21.89 && stdSpeed < 2.75){ //e6 & e3
            countH1 = e6h1*e3h1*h1;
            countH2 = e6h2*e3h2*h2;
            resultH1 = countH1/(countH1+countH2);
            resultH2 = countH2/(countH1+countH2);

            if(resultH1 > resultH2){
                txvHasil.setText(P1);
                for(int i=0; i<phoneNumber.size(); i++ ) {
                    send(phoneNumber.get(i));
                }
                phoneNumber.clear();
            }else if(resultH2 > resultH1){
                txvHasil.setText(P2);
            }

        }

    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public String getSetLongitude() {
        return setLongitude;
    }

    public void setSetLongitude(String setLongitude) {
        this.setLongitude = setLongitude;
    }

    public String getSetLatitude() {
        return setLatitude;
    }

    public void setSetLatitude(String setLatitude) {
        this.setLatitude = setLatitude;
    }

    public int getSpeedTimer() {
        return speedTimer;
    }

    public void setSpeedTimer(int speedTimer) {
        this.speedTimer = speedTimer;
    }

    public double getSdSpeeds() {
        return sdSpeeds;
    }

    public void setSdSpeeds(double sdSpeeds) {
        this.sdSpeeds = sdSpeeds;
    }

    public double getDifATt() {
        return difATt;
    }

    public void setDifATt(double difATt) {
        this.difATt = difATt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }
}
