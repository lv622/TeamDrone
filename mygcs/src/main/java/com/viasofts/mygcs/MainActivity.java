package com.viasofts.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.jetbrains.annotations.NotNull;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    // gcs 위치 설정
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    // 드론 설정
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;


    private Button button;
    private Button altitudeButton;
    private Button upAltitude;
    private Button downAltitude;
    private ArrayList<LatLng> guideLatLngArr = new ArrayList<>(); // 비행경로를 그릴 좌표계 배열
    private Button mapButton;
    private Button normalMap;
    private Button geoMap;
    private Button satelliteMap;


    private Boolean flag = false;
    private Boolean altitudeFlag = false;
    private Boolean mapFlag = false;
    // test_start
    private int count = 0;
    private  Marker droneMarker = new Marker();
    // test_end
    private double droneAltitude = 5.5;
    private String altitudeText = "";
    //private Spinner modeSelector;

    private Marker guideMarker = new Marker();
    private LatLng guideLatLng;
    private int guideCount = 0;

    private PolylineOverlay polyline = new PolylineOverlay();

    private NaverMap mNaverMap;



    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 타이틀 바 제거 및 가로 모드
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);


        // 네이버 맵 띄우기
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);


        // 버튼 클릭 시, 드론과 연결
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == false) {
                    button.setText("Disconnect");
                    ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
                    drone.connect(connectionParams);
                    flag = true;
                    //test
                    //updateVehicleMode();
                    Log.d("test", drone.getConnectionParameter().toString());
                } else {
                    button.setText("Connect");
                    drone.disconnect();
                    flag = false;
                    // 추가 부분
                    count = 0;
                }
            }
        });


        // 고도 상승 및 하강 버튼 구현
        altitudeButton = (Button) findViewById(R.id.altitudeButton);
        upAltitude = (Button) findViewById(R.id.upAltitudeButton);
        downAltitude = (Button) findViewById(R.id.downAltitudeButton);

        altitudeText = droneAltitude + "m 이륙고도";
        altitudeButton.setText(altitudeText);

        altitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (altitudeFlag == false) {
                    upAltitude.setVisibility(View.VISIBLE);
                    downAltitude.setVisibility(View.VISIBLE);
                    altitudeFlag = true;
                } else {
                    upAltitude.setVisibility(View.INVISIBLE);
                    downAltitude.setVisibility(View.INVISIBLE);
                    altitudeFlag = false;
                }
            }
        });

        upAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(droneAltitude < 10) {
                    droneAltitude += 0.5;
                    altitudeText = droneAltitude + "m 이륙고도";
                    altitudeButton.setText(altitudeText);
                }
            }
        });

        downAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(droneAltitude > 3) {
                    droneAltitude -= 0.5;
                    altitudeText = droneAltitude + "m 이륙고도";
                    altitudeButton.setText(altitudeText);
                }
            }
        });

        mapButton = (Button) findViewById(R.id.mapTypeButton);
        normalMap = (Button) findViewById(R.id.normalMapButton);
        geoMap = (Button) findViewById(R.id.geoMapButton);
        satelliteMap = (Button) findViewById(R.id.satelliteMapButton);

        // 여기까지
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mapFlag == false) {
                    normalMap.setVisibility(View.VISIBLE);
                }
            }
        });

/*
        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
 */

        // gcs 위치 받아오기
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        mainHandler = new Handler(getApplicationContext().getMainLooper());
    }


    //권한 받아오기
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);


    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        //updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateArmButton();
               // updateConnectedButton(this.drone.isConnected());
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                checkSoloState();

                break;


            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateArmButton();
              //  updateConnectedButton(this.drone.isConnected());
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
/*
                updateArmButton();
 */
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
/*
                    updateVehicleModesForType(this.droneType);
 */
                }
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.GPS_COUNT:
                updateGpsCount();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.GPS_POSITION:
                updateGpsPosition();
                if(guideCount > 0) {
                    if(checkGoal() == true) {
                        guideCount = 0;
                        changeToLoitor();
                    }
                }
                break;

            case AttributeEvent.HOME_UPDATED:
//                updateDistanceFromHome();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }


    @Override
    public void onMapReady(@NonNull @org.jetbrains.annotations.NotNull NaverMap naverMap) {
        mNaverMap = naverMap;

        naverMap.setMapType(NaverMap.MapType.Satellite);

        //gps로 내 위치 설정하기
        naverMap.setLocationSource(locationSource);
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);




        mNaverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull @NotNull PointF pointF, @NonNull @NotNull LatLng latLng) {


                guideLatLng = latLng;

                State checkVehicleState = drone.getAttribute(AttributeType.STATE);
                VehicleMode checkVehicleMode = checkVehicleState.getVehicleMode();

                // 가이드 모드인 경우 위치만 변경해서 이동시킴
                if(checkVehicleMode == VehicleMode.COPTER_GUIDED)
                {
                    guideMarker.setMap(null);
                    guideMarker.setIconTintColor(Color.YELLOW);
                    guideMarker.setPosition(guideLatLng);
                    guideMarker.setWidth(30);
                    guideMarker.setHeight(30);
                    guideMarker.setMap(mNaverMap);

                    goToSelectedPlace();

                }
                else {
                    AlertDialog.Builder guidedDialog = new AlertDialog.Builder(MainActivity.this);
                    guidedDialog.setTitle(null);
                    guidedDialog.setMessage("현재고도를 유지하며 목표지점까지 기체가 이동합니다.");
                    guidedDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            guideMarker.setMap(null);
                            guideMarker.setIconTintColor(Color.YELLOW);
                            guideMarker.setPosition(guideLatLng);
                            guideMarker.setWidth(30);
                            guideMarker.setHeight(30);
                            guideMarker.setMap(mNaverMap);


                            changeToGuided();
                            goToSelectedPlace();

                        }
                    });
                    guidedDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    guidedDialog.show();
                }

            }
        });

    }



    // 롱클릭된 좌표로 드론 이동시키기
    private void goToSelectedPlace() {

        ControlApi.getApi(this.drone).goTo(new LatLong(guideMarker.getPosition().latitude, guideMarker.getPosition().longitude),
                true, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지로 이동합니다.");
                        guideCount++;
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("목적지로 이동할 수 없습니다.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("시간이 초과되어 취소합니다.");
                    }
                });

    }

    // 가이드 모드로 변경하기
    private void changeToGuided() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("가이드 모드로 변경합니다.");
            }
            @Override
            public void onError(int executionError) {
                alertUser("모드 변경을 할 수 없습니다.");
            }
            @Override
            public void onTimeout() {
                alertUser("시간이 초과되어 취소합니다.");
            }
        });
    }

    // 로이터 모드로 변경하기
    private void changeToLoitor() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("로이터 모드로 변경합니다.");
            }
            @Override
            public void onError(int executionError) {
                alertUser("모드 변경을 할 수 없습니다.");
            }
            @Override
            public void onTimeout() {
                alertUser("시간이 초과되어 취소합니다.");
            }
        });
    }


    // 목적지 도착한 것인지 확인하기
    private boolean checkGoal() {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        return target.distanceTo(guideLatLng) <= 1; // 확실하지 않음
    }

    // 모터 가동 입력 시, 모터 가동 구현
    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {

            AlertDialog.Builder takeOffDialog = new AlertDialog.Builder(MainActivity.this);
            takeOffDialog.setTitle(null);
            takeOffDialog.setMessage("지정한 이륙고도까지 기체가 상승합니다.\n안전거리를 유지하세요.");
            takeOffDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    takeOff();
                }
            });
            takeOffDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            takeOffDialog.show();
            // Take off
//            ControlApi.getApi(this.drone).takeoff(droneAltitude, new AbstractCommandListener() {
//
//                @Override
//                public void onSuccess() {
//                    alertUser("Taking off...");
//
//                }
//
//                @Override
//                public void onError(int i) {
//                    alertUser("Unable to take off.");
//                }
//
//                @Override
//                public void onTimeout() {
//                    alertUser("Unable to take off.");
//                }
//            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed

            AlertDialog.Builder armingDialog = new AlertDialog.Builder(MainActivity.this);
            armingDialog.setTitle(null);
            armingDialog.setMessage("모터를 가동합니다.\n모터가 고속으로 회전합니다.");
            armingDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        Arming();
                }
            });
            armingDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                }
            });

            armingDialog.show();

        }
    }


    public void takeOff() {
        ControlApi.getApi(this.drone).takeoff(droneAltitude, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");

                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
        });
    }


    // 시동 걸기
    public void Arming() {
        VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
    }

    // UI Update!

    // 모터 가동 버튼 구현
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.armButton);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }


    // 전압 확인
    protected void updateVoltage() {
        TextView voltageTextView = (TextView) findViewById(R.id.batteryValueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        voltageTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");
        Log.d("test_vol", String.format("%3.1f", droneBattery.getBatteryVoltage()));
    }


    // 비행모드 확인
    protected void updateVehicleMode() {
        TextView vehicleModeTextView = (TextView) findViewById(R.id.vehicleModeLabelWriteTextView);
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        //VehicleMode vehicleMode = this.drone.getAttribute(AttributeType.STATE);



        if(vehicleMode == VehicleMode.COPTER_STABILIZE)
        {
            vehicleModeTextView.setText("STABILIZE");
        }
        else if(vehicleMode == VehicleMode.COPTER_LAND)
        {
            vehicleModeTextView.setText("LAND");
        }
        else if(vehicleMode == VehicleMode.COPTER_LOITER)
        {
            vehicleModeTextView.setText("LOITER");
        }
        else if(vehicleMode == vehicleMode.COPTER_GUIDED)
        {
            vehicleModeTextView.setText("GUIDED");
        }
        else if(vehicleMode == vehicleMode.COPTER_ALT_HOLD)
        {
            vehicleModeTextView.setText("ALT-HOLD");
        }
        else{
            vehicleModeTextView.setText("UNKNOWN");
        }

    }


    // 고도 확인
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    // 속도 확인
    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    // YAW 확인

    protected void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(attitude.getYaw() < 0)
        {
            yawTextView.setText(String.format("%3.1f", 360 + attitude.getYaw()) + "deg");
        }
        else {
            yawTextView.setText(String.format("%3.1f", attitude.getYaw()) + "deg");
        }
    }

    // GPS 위성 개수 확인
    protected void updateGpsCount() {
        TextView gpsCountTextView = (TextView) findViewById(R.id.gpsCountValueTextView);
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        gpsCountTextView.setText(String.format("%d", droneGps.getSatellitesCount()));
        Log.d("test_gps",String.format("%d", droneGps.getSatellitesCount()) );
    }

    // GPS 위치 표시 <- 위성이 잡혀야 표시가 됨
    protected void updateGpsPosition() {
        Gps dronePosition = this.drone.getAttribute(AttributeType.GPS);
        LatLong position = dronePosition.getPosition();
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double droneYaw = 0.0;
        if(attitude.getYaw() < 0)
        {
            droneYaw = 360 + attitude.getYaw();
        }
        else {
            droneYaw = attitude.getYaw();
        }


        Log.d("test_position", String.format("위도: %f, 경도: %f", position.getLatitude(), position.getLongitude()));

        if(count > 0)
        {
            droneMarker.setMap(null);
        }

        droneMarker.setIcon(OverlayImage.fromResource(R.drawable.drone_icon)); // 지시 점선 넣을 필요가 있음
        droneMarker.setHeight(35);
        droneMarker.setWidth(35);
        droneMarker.setPosition(new LatLng(position.getLatitude(), position.getLongitude()));
        droneMarker.setAngle((float)droneYaw);
        droneMarker.setMap(mNaverMap);

        // 비행경로 폴리라인 그리기
        guideLatLngArr.add(count, new LatLng(position.getLatitude(), position.getLongitude()));
        polyline.setCoords(guideLatLngArr);
        polyline.setWidth(10);
        polyline.setColor(Color.WHITE);
        polyline.setCapType(PolylineOverlay.LineCap.Round);
        polyline.setJoinType(PolylineOverlay.LineJoin.Round);
        polyline.setMap(mNaverMap);

        count++;
    }


}
