package com.viasofts.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
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
import com.o3dr.services.android.lib.util.MathUtils;
import com.viasofts.mygcs.activities.helpers.BluetoothDevicesActivity;
import com.viasofts.mygcs.utils.TLogUtils;
import com.viasofts.mygcs.utils.prefs.DroidPlannerPrefs;

import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.jetbrains.annotations.NotNull;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private Button lockButton;
    private Button lockOnButton;
    private Button lockOffButton;

    private Button geoTypeButton;
    private Button clearButton;

    // 미션 거리 선택 버튼
    private Button distanceButton;
    private Button upDistanceButton;
    private Button downDistanceButton;


    // 비행 폭 선택 버튼
    private Button widthButton;
    private Button upWidthButton;
    private Button downWidthButton;

    // 미션 유형 선택 버튼
    private Button missionButton;
    private Button missionABButton;
    private Button missionCancelButton;

    private Spinner modeSelector;


    private ArrayList<String> messageArr = new ArrayList<>(); // 메시지 담을 배열

    private Boolean flag = false;
    private Boolean altitudeFlag = false;
    private Boolean mapFlag = false;
    private Boolean lockFlag = false;
    private Boolean geoFlag = false;
    private Boolean missionTypeFlag = false;
    private Boolean distanceFlag = false;
    private Boolean widthFlag = false;

    private int count = 0;
    private Marker droneMarker = new Marker();
    private Marker lineMarker = new Marker();

    private double droneAltitude = 5.5;
    private String altitudeText = "";
    private int ABDistance = 50;
    private String distanceText = "";
    private double flightWidth = 5.5;
    private String widthText = "";

    private Marker guideMarker = new Marker();
    private LatLng guideLatLng;
    private int guideCount = 0;

    private PolylineOverlay polyline = new PolylineOverlay();

    private NaverMap mNaverMap;

    // test_mission
    private Marker iconA = new Marker();
    private Marker iconB = new Marker();
    private ArrayList<LatLng> missionArr = new ArrayList<>(); // 미션에 사용할 배열
    private Boolean flagAB = false; // AB 모드 전환 플래그
    private PolylineOverlay lineAB = new PolylineOverlay(); // A와 B를 선택하면 그려지는 라인
    private int countA = 0;
    private int countB = 0;

    private Button missionStartButton;
    private int missionCount = 0;
    private Mission mission;


    //추가 9/30
    private Button returnButton;


    private ArrayList<Marker> polygonMarker = new ArrayList<>();
    private Boolean flagPolygon = false;
    private PolygonOverlay polygon = new PolygonOverlay();
    private ArrayList<LatLng> polygonLatLng = new ArrayList<>();
    private int markerCount = 0;
    private ArrayList<Double> compareDegree = new ArrayList<>();

    private ArrayList<LatLng> realPolygonLatLng = new ArrayList<>();

    // test
    private DroidPlannerPrefs mPrefs;
    private static final long EVENTS_DISPATCHING_PERIOD = 200L;

    Button mBtnSetConnectionType;

    Handler mainHandler;

    private Boolean checkReturnHome = false;
    private Marker homeMarker = new Marker();
    private LatLong homePositionG;
    private LatLng homePositionN;


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
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);


        // Spinner modeSelector
        this.modeSelector = (Spinner) findViewById(R.id.modeSelector);
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


        // 버튼 클릭 시, 드론과 연결
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == false) {
                    button.setText("Disconnect");
                    ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
                    drone.connect(retrieveConnectionParameters());
                    // drone.connect(connectionParams);
                    flag = true;
                } else {
                    button.setText("Connect");
                    drone.disconnect();
                    flag = false;

                    polyline.setMap(null);
                    guideLatLngArr.clear();
                    count = 0;

                }
            }
        });

        // 고도 상승 및 하강 버튼 구현
        altitudeButton = (Button) findViewById(R.id.altitudeButton);
        upAltitude = (Button) findViewById(R.id.upAltitudeButton);
        downAltitude = (Button) findViewById(R.id.downAltitudeButton);

        altitudeText = droneAltitude + "m\n이륙고도";
        altitudeButton.setText(altitudeText);

        altitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (altitudeFlag == true) {
                    upAltitude.setVisibility(View.VISIBLE);
                    downAltitude.setVisibility(View.VISIBLE);
                    altitudeFlag = false;
                } else {
                    upAltitude.setVisibility(View.INVISIBLE);
                    downAltitude.setVisibility(View.INVISIBLE);
                    altitudeFlag = true;
                }
            }
        });

        upAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (droneAltitude < 10) {
                    droneAltitude += 0.5;
                    altitudeText = droneAltitude + "m\n이륙고도";
                    altitudeButton.setText(altitudeText);
                }
            }
        });

        downAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (droneAltitude > 3) {
                    droneAltitude -= 0.5;
                    altitudeText = droneAltitude + "m\n이륙고도";
                    altitudeButton.setText(altitudeText);
                    // test 메시지 창
                    // alertUser("0.5m 감소!");
                }
            }
        });


        recyclerTest();

        // test
        SharedPreferences sharedPref = getPreferences(getApplicationContext().MODE_PRIVATE);
        SharedPrefManager.getInstance().init(sharedPref);
        mPrefs = DroidPlannerPrefs.getInstance(getApplicationContext());

        mBtnSetConnectionType = findViewById(R.id.button_set_conn_type);
        final String[] types = getResources().getStringArray(R.array.connection_type);
        mBtnSetConnectionType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setTitle("SET CONNECTION TYPE")
                        .setItems(R.array.connection_type, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String value = types[id];
                                SharedPrefManager.save(SharedPrefManager.KEY_CONNECTION, value);
                                mBtnSetConnectionType.setText(value);
                            }
                        });

                final AlertDialog alert = builder.create();
                alert.show();
            }
        });

        String connectionType = SharedPrefManager.read(SharedPrefManager.KEY_CONNECTION);
        if (connectionType.equals("")) {
            connectionType = types[0];
        }

        mBtnSetConnectionType.setText(connectionType);

        // test
        this.modeSelector = (Spinner) findViewById(R.id.modeSelector);

        final ArrayList<String> modeList = new ArrayList<>();


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

    private void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
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
                checkSoloState();
                break;


            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
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
                if (guideCount > 0) {
                    if (checkGoal() == true) {
                        guideCount = 0;
                        changeToLoitor();
                        alertUser("도착 완료");
                        guideMarker.setMap(null);
                    }
                }
                State vehicleState = this.drone.getAttribute(AttributeType.STATE);
                VehicleMode vehicleMode = vehicleState.getVehicleMode();
                // 리턴 버튼 테스트 필요
                if (vehicleMode == vehicleMode.COPTER_GUIDED && checkReturnHome) {
                    if (checkBackHome() == true) {
                        changeToLand(); // land
                        alertUser("도착 완료");
                        checkReturnHome = false;
                        homeMarker.setMap(null);
                        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
                        LatLong vehiclePosition = droneGps.getPosition();
                    }
                }
                break;

            case AttributeEvent.HOME_UPDATED:
//                updateDistanceFromHome();
                break;
            case AttributeEvent.MISSION_SENT:
                break;

            case AttributeEvent.MISSION_ITEM_REACHED:
                alertUser("미션 비행 완료");
                missionCount = 0;
                missionStartButton.setText("임무전송");
                break;
            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
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
        //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        messageArr.add(String.format("★ " + message));
        recyclerTest();
    }

    public void recyclerTest() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        SimpleTextAdapter adapter = new SimpleTextAdapter(messageArr);
        recyclerView.setAdapter(adapter);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }


    @Override
    public void onMapReady(@NonNull @org.jetbrains.annotations.NotNull final NaverMap naverMap) {
        mNaverMap = naverMap;

        naverMap.setMapType(NaverMap.MapType.Satellite);

        //gps로 내 위치 설정하기
        naverMap.setLocationSource(locationSource);
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 줌 버튼 제거
        final UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setCompassEnabled(false);

        mapButton = (Button) findViewById(R.id.mapTypeButton);
        normalMap = (Button) findViewById(R.id.normalMapButton);
        geoMap = (Button) findViewById(R.id.geoMapButton);
        satelliteMap = (Button) findViewById(R.id.satelliteMapButton);

        // 지도 유형 변경하기
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFlag == true) {
                    normalMap.setVisibility(View.VISIBLE);
                    geoMap.setVisibility(View.VISIBLE);
                    satelliteMap.setVisibility(View.VISIBLE);
                    mapFlag = false;
                } else {
                    normalMap.setVisibility(View.INVISIBLE);
                    geoMap.setVisibility(View.INVISIBLE);
                    satelliteMap.setVisibility(View.INVISIBLE);
                    mapFlag = true;
                }
            }
        });

        normalMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNaverMap.setMapType(NaverMap.MapType.Basic);
                normalMap.setBackgroundResource(R.drawable.btn_on);
                geoMap.setBackgroundResource(R.drawable.btn_off);
                satelliteMap.setBackgroundResource(R.drawable.btn_off);
                mapButton.setText("일반지도");
            }
        });


        geoMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNaverMap.setMapType(NaverMap.MapType.Terrain);
                normalMap.setBackgroundResource(R.drawable.btn_off);
                geoMap.setBackgroundResource(R.drawable.btn_on);
                satelliteMap.setBackgroundResource(R.drawable.btn_off);
                mapButton.setText("지형도");
            }
        });

        satelliteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNaverMap.setMapType(NaverMap.MapType.Satellite);
                normalMap.setBackgroundResource(R.drawable.btn_off);
                geoMap.setBackgroundResource(R.drawable.btn_off);
                satelliteMap.setBackgroundResource(R.drawable.btn_on);
                mapButton.setText("위성지도");

            }
        });

        // 멥 이동 잠금 버튼 기능
        lockButton = (Button) findViewById(R.id.lockTypeButton);
        lockOnButton = (Button) findViewById(R.id.lockOnButton);
        lockOffButton = (Button) findViewById(R.id.lockOffButton);


        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lockFlag == true) {
                    lockOnButton.setVisibility(View.VISIBLE);
                    lockOffButton.setVisibility(View.VISIBLE);
                    lockFlag = false;
                } else {
                    lockOnButton.setVisibility(View.INVISIBLE);
                    lockOffButton.setVisibility(View.INVISIBLE);
                    lockFlag = true;
                }
            }
        });

        // 잠금 버튼 누를 시 드론 좌표로 카메라 이동 필요
        lockOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiSettings.setScrollGesturesEnabled(false);
                lockOnButton.setBackgroundResource(R.drawable.btn_on);
                lockOffButton.setBackgroundResource(R.drawable.btn_off);
                lockButton.setText("맵 잠금");
            }
        });

        lockOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiSettings.setScrollGesturesEnabled(true);
                lockOnButton.setBackgroundResource(R.drawable.btn_off);
                lockOffButton.setBackgroundResource(R.drawable.btn_on);
                lockButton.setText("맵 이동");
            }
        });

        // 지적도 온오프 버튼
        geoTypeButton = (Button) findViewById(R.id.geoTypeButton);

        geoTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (geoFlag == true) {
                    geoTypeButton.setBackgroundResource(R.drawable.btn_off);
                    geoTypeButton.setText("지적도\nOff");
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    geoFlag = false;
                } else {
                    geoTypeButton.setBackgroundResource(R.drawable.btn_on);
                    geoTypeButton.setText("지적도\nOn");
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    geoFlag = true;
                }
            }
        });

        // 화면 지우기 버튼
        clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                polyline.setMap(null);
                guideLatLngArr.clear();
                count = 0;

                guideMarker.setMap(null);
                homeMarker.setMap(null);

                countA = 0;
                countB = 0;
                missionArr.clear();
                iconA.setMap(null);
                iconB.setMap(null);

                lineAB.setMap(null);

                polygon.setMap(null);

                for (int i = 0; i < polygonMarker.size(); i++) {
                    polygonMarker.get(i).setMap(null);
                }
                polygonMarker.clear();
                polygonLatLng.clear();
                realPolygonLatLng.clear();
                markerCount = 0;
                // 임무 초기화 필요
            }
        });

        // 미션 거리 선택
        distanceButton = (Button) findViewById(R.id.distanceButton);
        upDistanceButton = (Button) findViewById(R.id.upDistanceButton);
        downDistanceButton = (Button) findViewById(R.id.downDistanceButton);

        distanceText = ABDistance + "m\nAB거리";
        distanceButton.setText(distanceText);

        distanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (distanceFlag == true) {
                    upDistanceButton.setVisibility(View.VISIBLE);
                    downDistanceButton.setVisibility(View.VISIBLE);
                    distanceFlag = false;
                } else {
                    upDistanceButton.setVisibility(View.INVISIBLE);
                    downDistanceButton.setVisibility(View.INVISIBLE);
                    distanceFlag = true;
                }
            }
        });

        upDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 제한 조건 필요할 듯
                ABDistance = ABDistance + 10;
                distanceText = ABDistance + "m\nAB거리";
                distanceButton.setText(distanceText);
            }
        });

        downDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ABDistance > 10) {
                    ABDistance = ABDistance - 10;
                    distanceText = ABDistance + "m\nAB거리";
                    distanceButton.setText(distanceText);
                }
            }
        });

        // 비행 폭 선택
        widthButton = (Button) findViewById(R.id.flightWidthButton);
        upWidthButton = (Button) findViewById(R.id.upWidthButton);
        downWidthButton = (Button) findViewById(R.id.downWidthButton);

        widthText = flightWidth + "m\n비행폭";
        widthButton.setText(widthText);

        widthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (widthFlag == true) {
                    upWidthButton.setVisibility(View.VISIBLE);
                    downWidthButton.setVisibility(View.VISIBLE);
                    widthFlag = false;
                } else {
                    upWidthButton.setVisibility(View.INVISIBLE);
                    downWidthButton.setVisibility(View.INVISIBLE);
                    widthFlag = true;
                }
            }
        });

        upWidthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flightWidth < 10) {
                    flightWidth = flightWidth + 0.5;
                    widthText = flightWidth + "m\n비행폭";
                    widthButton.setText(widthText);
                }
            }
        });

        downWidthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flightWidth > 1) {
                    flightWidth = flightWidth - 0.5;
                    widthText = flightWidth + "m\n비행폭";
                    widthButton.setText(widthText);
                }
            }
        });

        // 미션 유형 선택
        missionButton = (Button) findViewById(R.id.missionButton);
        missionABButton = (Button) findViewById(R.id.missionABButton);
        missionCancelButton = (Button) findViewById(R.id.missionCancelButton);
        missionStartButton = (Button) findViewById(R.id.missionStartButton);
        returnButton = (Button) findViewById(R.id.returnButton);

        missionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (missionTypeFlag == true) {
                    missionABButton.setVisibility(View.VISIBLE);
                    missionCancelButton.setVisibility(View.VISIBLE);
                    missionTypeFlag = false;
                } else {
                    missionABButton.setVisibility(View.INVISIBLE);
                    missionCancelButton.setVisibility(View.INVISIBLE);
                    missionTypeFlag = true;
                }
            }
        });

        missionABButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                missionButton.setText("AB");
                flagAB = true;
                flagPolygon = false;
                missionStartButton.setVisibility(View.VISIBLE);
                returnButton.setVisibility(View.VISIBLE);
            }
        });

        missionCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                missionButton.setText("임무");
                flagAB = false;
                flagPolygon = false;
                missionStartButton.setVisibility(View.INVISIBLE);
                returnButton.setVisibility(View.INVISIBLE);
            }
        });

        // AB 라인 미션
        mNaverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull @NotNull PointF pointF, @NonNull @NotNull LatLng latLng) {
                if (flagAB == true) {
                    if (countA == 0) {
                        missionArr.add(latLng);
                        iconA.setPosition(latLng);
                        iconA.setIcon(OverlayImage.fromResource(R.drawable.icon_a));
                        iconA.setWidth(25);
                        iconA.setHeight(25);
                        iconA.setMap(mNaverMap);
                        countA++;
                    } else if (countA == 1 && countB == 0) {
                        missionArr.add(latLng);
                        iconB.setPosition(latLng);
                        iconB.setIcon(OverlayImage.fromResource(R.drawable.icon_b));
                        iconB.setWidth(25);
                        iconB.setHeight(25);
                        iconB.setMap(mNaverMap);
                        countB++;
                        // 추가 구현 필요 (폴리라인 그리기)
                        // 밑에 구간 계속 사용하도록 만들기 메소드로 구현할 것, 그리고 distance 값 바꾸기
                        getNextCoords();
                        lineAB.setCoords(missionArr);
                        lineAB.setColor(Color.YELLOW);
                        lineAB.setMap(mNaverMap);
                        // test 이 밑부터는 버튼을 클릭할 시 작동하는 방법으로 가야됨
                        missionLineSetting();
                    }
                }
            }
        });

        // 가이드 모드 비행
        mNaverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull @NotNull PointF pointF, @NonNull @NotNull LatLng latLng) {
                guideLatLng = latLng;

                State checkVehicleState = drone.getAttribute(AttributeType.STATE);
                VehicleMode checkVehicleMode = checkVehicleState.getVehicleMode();

                // 가이드 모드인 경우 위치만 변경해서 이동시킴
                if (checkVehicleMode == VehicleMode.COPTER_GUIDED) {
                    guideMarker.setMap(null);
                    guideMarker.setIconTintColor(Color.YELLOW);
                    guideMarker.setPosition(guideLatLng);
                    guideMarker.setWidth(30);
                    guideMarker.setHeight(30);
                    guideMarker.setMap(mNaverMap);

                    goToSelectedPlace();

                } else {
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

    // 버튼 클릭 시 텍스트 변경이 안되는 점 개션 필요
    public void missionButtonClick(View view) {
        missionStartButton = findViewById(R.id.missionStartButton);
        if (flagAB == true) {
            if (missionCount == 0) {
                missionStartButton.setText("임무설정");
                missionCount++;
                missionLineSetting();

            } else if (missionCount == 1) {
                missionStartButton.setText("임무시작");
                missionCount++;
                missionStartSetting();
            } else if (missionCount == 2) {
                missionStartButton.setText("임무종료");
                missionPauseSetting();
            }
        }
    }

    public void missionLineSetting() {
        mission = new Mission();
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);

        for (int i = 0; i < missionArr.size(); i++) {
            Waypoint waypoint = new Waypoint();
            waypoint.setCoordinate(new LatLongAlt(missionArr.get(i).latitude, missionArr.get(i).longitude, droneAltitude.getAltitude()));
            waypoint.setDelay(1);
            mission.addMissionItem(i, waypoint);
        }
        MissionApi.getApi(this.drone).setMission(mission, true);
    }

    public void missionStartSetting() {
        MissionApi.getApi(this.drone).startMission(true, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("임무를 시작합니다!");
            }

            @Override
            public void onError(int executionError) {
                alertUser("임무 시작을 종료합니다.");
                missionCount = 1;
            }

            @Override
            public void onTimeout() {
                alertUser("시간 초과로 임무를 종료합니다.");
                missionCount = 1;
            }
        });
    }

    public void missionPauseSetting() {
        MissionApi.getApi(this.drone).pauseMission(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("임무를 멈춥니다.");
                missionCount = 1;
                changeToLoitor();
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error");
                missionCount = 2;
            }

            @Override
            public void onTimeout() {
                alertUser("time out");
                missionCount = 2;
            }
        });
    }

    public void getNextCoords() {
        MathUtils mathUtils = new MathUtils();

        int missionCount = (int) (ABDistance / flightWidth) * 2 + 2;
        double lineDistance = mathUtils.getDistance2D(new LatLong(missionArr.get(0).latitude, missionArr.get(0).longitude), new LatLong(missionArr.get(1).latitude, missionArr.get(1).longitude));

        for (int i = 2; i < missionCount; i++) {
            if (i % 4 == 2 || i % 4 == 3) {
                if (i % 4 == 2) {
                    LatLong latLong = mathUtils.newCoordFromBearingAndDistance(new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude),
                            mathUtils.getHeadingFromCoordinates(new LatLong(missionArr.get(i - 2).latitude, missionArr.get(i - 2).longitude),
                                    new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude)) + 90, flightWidth);
                    missionArr.add(i, new LatLng(latLong.getLatitude(), latLong.getLongitude()));
                } else if (i % 4 == 3) {
                    LatLong latLong = mathUtils.newCoordFromBearingAndDistance(new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude),
                            mathUtils.getHeadingFromCoordinates(new LatLong(missionArr.get(i - 2).latitude, missionArr.get(i - 2).longitude),
                                    new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude)) + 90, lineDistance);
                    missionArr.add(i, new LatLng(latLong.getLatitude(), latLong.getLongitude()));
                }
            } else if (i % 4 == 0 || i % 4 == 1) {
                if (i % 4 == 0) {
                    LatLong latLong = mathUtils.newCoordFromBearingAndDistance(new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude),
                            mathUtils.getHeadingFromCoordinates(new LatLong(missionArr.get(i - 2).latitude, missionArr.get(i - 2).longitude),
                                    new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude)) - 90, flightWidth);
                    missionArr.add(i, new LatLng(latLong.getLatitude(), latLong.getLongitude()));
                } else if (i % 4 == 1) {
                    LatLong latLong = mathUtils.newCoordFromBearingAndDistance(new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude),
                            mathUtils.getHeadingFromCoordinates(new LatLong(missionArr.get(i - 2).latitude, missionArr.get(i - 2).longitude),
                                    new LatLong(missionArr.get(i - 1).latitude, missionArr.get(i - 1).longitude)) - 90, lineDistance);
                    missionArr.add(i, new LatLng(latLong.getLatitude(), latLong.getLongitude()));
                }
            }
        }
    }

    // 롱클릭된 좌표로 드론 이동시키기
    private void goToSelectedPlace() {

        ControlApi.getApi(this.drone).goTo(new LatLong(guideMarker.getPosition().latitude, guideMarker.getPosition().longitude), true, new AbstractCommandListener() {
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

    // 리턴 버튼 관련 메소드
    public void guidedHomeBtn(View view) {
        State checkVehicleState = drone.getAttribute(AttributeType.STATE);
        VehicleMode checkVehicleMode = checkVehicleState.getVehicleMode();

        if (checkVehicleMode == VehicleMode.COPTER_GUIDED) {
            homeMarker.setMap(null);
            homeMarker.setIconTintColor(Color.YELLOW);
            homeMarker.setPosition(homePositionN);
            homeMarker.setWidth(30);
            homeMarker.setHeight(30);
            homeMarker.setMap(mNaverMap);
            goHomePoint();

        } else {
            AlertDialog.Builder guidedDialog = new AlertDialog.Builder(MainActivity.this);
            guidedDialog.setTitle(null);
            guidedDialog.setMessage("현재고도를 유지하며 목표지점까지 기체가 이동합니다.");
            guidedDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    homeMarker.setMap(null);
                    homeMarker.setIconTintColor(Color.YELLOW);
                    homeMarker.setPosition(homePositionN);
                    homeMarker.setWidth(30);
                    homeMarker.setHeight(30);
                    homeMarker.setMap(mNaverMap);

                    changeToGuided();
                    goHomePoint();
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


    //홈포지션으로 드론 이동
    private void goHomePoint() {
        ControlApi.getApi(this.drone).goTo(homePositionG, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("목적지로 이동합니다.");
                checkReturnHome = true;
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

    //홈포지션에 도착했는지 확인
    public boolean checkBackHome() {
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();
        LatLng vehicleLatLng = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
        return homePositionN.distanceTo(vehicleLatLng) <= 1;
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

    // Stabilize mode
    private void changeToStabilize() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_STABILIZE, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("수동 모드로 변경합니다.");
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

    // alt-hold mode
    private void changeToAltHold() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_ALT_HOLD, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("alt-hold 모드로 변경합니다.");
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

    // auto mode
    private void changeToAuto() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("auto 모드로 변경합니다.");
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

    // land mode
    private void changeToLand() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("착륙 모드로 변경합니다.");
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
        //  GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        //  LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        Gps dronePosition = this.drone.getAttribute(AttributeType.GPS);
        LatLong position = dronePosition.getPosition();
        LatLng coords = new LatLng(position.getLatitude(), position.getLongitude());
        return coords.distanceTo(guideLatLng) <= 1;
        // return target.distanceTo(guideLatLng) <= 1; // 확실하지 않음
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
            public void onSuccess() {
                alertUser("Vehicle armed successfully.");
            }

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
    }

    // 비행모드 확인
    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        TextView vehicleModeTextView = (TextView) findViewById(R.id.vehicleModeLabelWriteTextView);
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        //VehicleMode vehicleMode = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleMode == VehicleMode.COPTER_STABILIZE) {
            vehicleModeTextView.setText("STABILIZE");
        } else if (vehicleMode == VehicleMode.COPTER_LAND) {
            vehicleModeTextView.setText("LAND");
        } else if (vehicleMode == VehicleMode.COPTER_LOITER) {
            vehicleModeTextView.setText("LOITER");
        } else if (vehicleMode == vehicleMode.COPTER_GUIDED) {
            vehicleModeTextView.setText("GUIDED");
        } else if (vehicleMode == vehicleMode.COPTER_ALT_HOLD) {
            vehicleModeTextView.setText("ALT-HOLD");
        } else if (vehicleMode == vehicleMode.COPTER_AUTO) {
            vehicleModeTextView.setText("AUTO");
        } else {
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
        if (attitude.getYaw() < 0) {
            yawTextView.setText(String.format("%3.1f", 360 + attitude.getYaw()) + "deg");
        } else {
            yawTextView.setText(String.format("%3.1f", attitude.getYaw()) + "deg");
        }
    }

    // GPS 위성 개수 확인
    protected void updateGpsCount() {
        TextView gpsCountTextView = (TextView) findViewById(R.id.gpsCountValueTextView);
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        gpsCountTextView.setText(String.format("%d", droneGps.getSatellitesCount()));
    }

    // GPS 위치 표시 <- 위성이 잡혀야 표시가 됨
    protected void updateGpsPosition() {
        Gps dronePosition = this.drone.getAttribute(AttributeType.GPS);
        LatLong position = dronePosition.getPosition();
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double droneYaw = 0.0;
        if (attitude.getYaw() < 0) {
            droneYaw = 360 + attitude.getYaw();
        } else {
            droneYaw = attitude.getYaw();
        }

        if (count > 0) {
            droneMarker.setMap(null);
            lineMarker.setMap(null);
        }

        // 방향 표시선 그리기
        lineMarker.setIcon(OverlayImage.fromResource(R.drawable.green_dash_line));
        lineMarker.setPosition(new LatLng(position.getLatitude(), position.getLongitude()));
        lineMarker.setAngle((float) droneYaw);
        lineMarker.setHeight(150);
        lineMarker.setMap(mNaverMap);

        // 드론 아이콘 그리기
        droneMarker.setIcon(OverlayImage.fromResource(R.drawable.drone_icon));
        droneMarker.setHeight(35);
        droneMarker.setWidth(35);
        droneMarker.setPosition(new LatLng(position.getLatitude(), position.getLongitude()));
        droneMarker.setAngle((float) droneYaw);
        droneMarker.setMap(mNaverMap);


        // 비행경로 폴리라인 그리기
        Collections.addAll(guideLatLngArr, new LatLng(position.getLatitude(), position.getLongitude()));
        polyline.setCoords(guideLatLngArr);
        polyline.setWidth(5);
        polyline.setColor(Color.WHITE);
        polyline.setCapType(PolylineOverlay.LineCap.Round);
        polyline.setJoinType(PolylineOverlay.LineJoin.Round);
        polyline.setMap(mNaverMap);

        count++;

        homePositionG = new LatLong(guideLatLngArr.get(0).latitude, guideLatLngArr.get(0).longitude);
        homePositionN = guideLatLngArr.get(0);
    }

    private ConnectionParameter retrieveConnectionParameters() {
        final @ConnectionType.Type int connectionType = mPrefs.getConnectionParameterType();

        // Generate the uri for logging the tlog data for this session.
        Uri tlogLoggingUri = TLogUtils.getTLogLoggingUri(getApplicationContext(),
                connectionType, System.currentTimeMillis());

        int idx = getConnectionTypeIdx();

        ConnectionParameter connParams;
        //switch (connectionType) {
        //switch (ConnectionType.TYPE_BLUETOOTH) {
        switch (idx) {
            case ConnectionType.TYPE_USB:
                connParams = ConnectionParameter.newUsbConnection(mPrefs.getUsbBaudRate(),
                        tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
                break;

            case ConnectionType.TYPE_UDP:
                if (mPrefs.isUdpPingEnabled()) {
                    connParams = ConnectionParameter.newUdpWithPingConnection(
                            mPrefs.getUdpServerPort(),
                            mPrefs.getUdpPingReceiverIp(),
                            mPrefs.getUdpPingReceiverPort(),
                            "Hello".getBytes(),
                            ConnectionType.DEFAULT_UDP_PING_PERIOD,
                            tlogLoggingUri,
                            EVENTS_DISPATCHING_PERIOD);
                } else {
                    connParams = ConnectionParameter.newUdpConnection(mPrefs.getUdpServerPort(),
                            tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
                }
                break;

            case ConnectionType.TYPE_TCP:
                connParams = ConnectionParameter.newTcpConnection(mPrefs.getTcpServerIp(),
                        mPrefs.getTcpServerPort(), tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
                break;

            case ConnectionType.TYPE_BLUETOOTH:
                String btAddress = mPrefs.getBluetoothDeviceAddress();

                if (TextUtils.isEmpty(btAddress)) {
                    connParams = null;
                    startActivity(new Intent(getApplicationContext(), BluetoothDevicesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                } else {
                    connParams = ConnectionParameter.newBluetoothConnection(btAddress,
                            tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
                }
                break;

            default:
                Log.e("myLog", "Unrecognized connection type: " + connectionType);
                connParams = null;
                break;
        }
        return connParams;
    }

    public int getConnectionTypeIdx() {
        final String[] types = getResources().getStringArray(R.array.connection_type);
        String strConn = mBtnSetConnectionType.getText().toString();

        int idx = 3;
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(strConn)) {
                return i;
            }
        }
        return idx;
    }
}