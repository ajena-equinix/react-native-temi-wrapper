package com.temiwrapper;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.facebook.common.internal.Ints;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.Gson;
import com.robotemi.sdk.BatteryData;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.map.Layer;
import com.robotemi.sdk.map.MapDataModel;
import com.robotemi.sdk.map.MapImage;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.permission.Permission;
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener;
import com.robotemi.sdk.sequence.SequenceModel;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.robotemi.sdk.constants.Page.MAP_EDITOR;


public class Module extends ReactContextBaseJavaModule implements OnSequencePlayStatusChangedListener,OnGoToLocationStatusChangedListener,OnCurrentPositionChangedListener, LifecycleEventListener {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";

    private final ReactApplicationContext reactContext;

    private ExecutorService  executor= Executors.newSingleThreadExecutor();

    public Module(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
        Robot.getInstance().addOnSequencePlayStatusChangedListener(this);
        Robot.getInstance().addOnCurrentPositionChangedListener(this);
        reactContext.addLifecycleEventListener(this);

    }

    private static Robot robot = Robot.getInstance();

    @Override
    public String getName() {
        return "dora";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @ReactMethod
    public void exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    /*
    Get Battery info
    * */
    @ReactMethod
    public void getBatteryInfo(final Promise promise)
    {
        BatteryData batteryData = robot.getBatteryData();
        String charging ="N";
        WritableMap map = new WritableNativeMap();
        Log.d("isbattery Charging : ", ""+batteryData.isCharging());
        if(batteryData.isCharging())
        {
            charging ="Y";
        }
        map.putString("batteryPercent",String.valueOf(batteryData.getBatteryPercentage()));
        map.putString("batteryIsCharging",charging);
        promise.resolve(map);
    }

    /*
 Get Battery :is charging
 * */
    @ReactMethod
    public void getBatteryChargingInfo(final Promise promise) {
        boolean isCharging = robot.getBatteryData().isCharging();
        promise.resolve(isCharging);
    }


    @ReactMethod
    public void playSequence(String sequenceId) {
        callSequence(sequenceId);
    }

    private void callSequence(String sequenceId)
    {
        checkPermission();
        Log.d("id: ", sequenceId);
        int i=0;
        for (SequenceModel x : robot.getAllSequences()) {
            if (x.getId().equals(sequenceId)) {
                Log.d(" Sequence id present ", sequenceId);
                i=1;
                break;
            }
        }
        if(i==0){
            Log.d(" Sequence id  not prst ", sequenceId);
        }
        Log.d("Sequence id : ", sequenceId);
        if (sequenceId != "")
        {
            robot.playSequence(sequenceId);
        }
    }


    public void checkPermission()
    {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.SEQUENCE);
        robot.requestPermissions(permissions, 4);
    }

    @ReactMethod
    public void getSequenceData(final Promise promise)
    {
        try
        {
            checkPermission();
            List<SequenceModel> sequenceList = robot.getAllSequences();
            Log.d("Size ", "--"+sequenceList.size());
            // Log.d("Size1 ", "--"+sequenceList.toString());

            Gson g = new Gson();
            // Log.i("sequenceList",""+sequenceList);
            WritableArray array = new WritableNativeArray();
            for (SequenceModel co : sequenceList)
            {
                // Log.i("model",""+co);
                // Log.i("","");
                JSONObject jo = new JSONObject(g.toJson(co));
                //Log.i("jo",""+jo);
                WritableMap wm = convertJsonToMap(jo);
                // Log.i("wm",wm.toString());
                array.pushMap(wm);
            }
            //  Log.i("array",array.toString());
            promise.resolve(array);
        } catch (Exception e)
        {
            Log.d("Sequene error", "Error" + e);
            promise.reject(e);
        }
    }

    //added on 2/9/2022 by chris
    @ReactMethod
    public void requestLocationMap(final Promise promise) {
        try{

            Log.d("map Model ----", "Inside request Map Method");

            List<Permission> permissions = new ArrayList<>();
            permissions.add(Permission.MAP);
            robot.requestPermissions(permissions, 4);

            MapDataModel mapModel = robot.getMapData();
            MapImage k = mapModel.getMapImage();

            Log.d("map Model ----", "Load Map" + mapModel.getMapId());
            Log.d("map Model ----", "Load Map" + mapModel.getVirtualWalls());
            Log.d("Map Data", "Image Model ==>" + k.getData());
            Log.d("Map Data", "Image Model Size ==>" + k.getData().size());

            Log.d("map Model ----", "Printing getLocation Details" + mapModel.getLocations());
            Log.d("map Model ----", "Printing getLocation Size" + mapModel.getLocations().size());
            Gson g = new Gson();
            WritableArray array = new WritableNativeArray();
            for (Layer l1 : mapModel.getLocations()) {

                Log.d("Layer Location of Id :" + l1.getLayerId(), "  Description :" + l1.describeContents());
                Log.d("Layer Location of Id :" + l1.getLayerId(), "  Layer Category :" + l1.describeContents());
                Log.d("Layer Location of Id :" + l1.getLayerId(), "  Layer Creation UTC :" + l1.getLayerCreationUTC());
                Log.d("Layer Location of Id :" + l1.getLayerId(), "  Layer Thickness :" + l1.getLayerThickness());
                Log.d("Layer Location of Id :" + l1.getLayerId(), "  Layer Posses :" + l1.getLayerPoses());
                JSONObject JoLayer = new JSONObject(g.toJson(l1));

                Log.d("JsonObject MapLocations",JoLayer.toString());
                WritableMap wm = convertJsonToMap(JoLayer);
                Log.d("Writable  Map Locations",wm.toString());
                array.pushMap(wm);

            }
            Log.d("temi serial number",robot.getSerialNumber());

            Log.d("Array  Map Locations",array.toString());

            Log.d("map Model ----", "Printing Virtual wall Details" + mapModel.getVirtualWalls());
            Log.d("map Model ----", "Printing Virtual wall Details Size" + mapModel.getVirtualWalls().size());
            for (Layer l1 : mapModel.getVirtualWalls()) {
                Log.d("Virtual wall of Id :" + l1.getLayerId(), "  Description :" + l1.describeContents());
                Log.d("Virtual wall of Id :" + l1.getLayerId(), "  Layer Category :" + l1.describeContents());
                Log.d("Virtual wall of Id :" + l1.getLayerId(), "  Layer Creation UTC :" + l1.getLayerCreationUTC());
                Log.d("Virtual wall of Id :" + l1.getLayerId(), "  Layer Thickness :" + l1.getLayerThickness());
                Log.d("Virtual wall of Id :" + l1.getLayerId(), "  Layer Posses :" + l1.getLayerPoses());
            }
            ArrayList<Integer> ik = new ArrayList<Integer>();
            for (Integer t : k.getData()) {
                ik.add(Color.argb((int) (t * 2.55), 0, 0, 0));
            }

            int[] ret = new int[ik.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ik.get(i);
            }

            Log.d("Int arrayy ----", "ret[i] -->" + ret);

            Log.d("Int arrayy ----", "col -->" + k.getCols());
            Log.d("Int arrayy ----", "row -->" + k.getRows());

            Bitmap b = Bitmap.createBitmap(ret, k.getCols(), k.getRows(), Bitmap.Config.ALPHA_8);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //add support for jpg and more.
            b.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Log.d("encoded", "ALPHA_8--------->" + encoded);
            Log.d("encoded", "ALPHA_8 to Stirng ------------------>" + encoded.toString());


            Bitmap b2 = Bitmap.createBitmap(ret, k.getCols(), k.getRows(), Bitmap.Config.ARGB_8888);

            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            //add support for jpg and more.
            b2.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream2);
            byte[] byteArray2 = byteArrayOutputStream2.toByteArray();

            String encoded2 = Base64.encodeToString(byteArray2, Base64.DEFAULT);

            Log.d("encoded", "value ARGB_8888--------->" + encoded2+"-----END-------");
            Log.d("encoded", "value to ARGB_8888 ------------------>" + encoded2.toString());


            Log.d("map Model ----","Map zxzxzxModel "+mapModel);

            Log.d("END************","I am here at line 344" + array);
            promise.resolve(array);
        } catch (Exception e)
        {
            Log.d("Sequene error", "Error" + e);
            promise.reject(e);
        }
    }
//added on 2/9/2022 by chris

    @SuppressLint("LongLogTag")
    public void requestMap() {
    }


    @ReactMethod
    public void getSequenceList(Callback successCallback) throws JSONException
    {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.SEQUENCE);
        robot.requestPermissions(permissions, 4);

        List<SequenceModel> sequenceList = robot.getAllSequences();
        Log.d("Size ", "--" + robot.checkSelfPermission(Permission.SEQUENCE));

        if (robot.checkSelfPermission(Permission.SEQUENCE) == Permission.GRANTED) {
            robot.getInstance().speak(TtsRequest.create("Saved the " + sequenceList.size() + " location failed.", true));
        }
        Log.d("sequence ----", "" + sequenceList.get(0).component1());
        Log.d("sequence o ----", "" + sequenceList.get(0));
        Log.d("sequence ----", "" + sequenceList.get(0).component2());
        Log.d("sequence ----", "" + sequenceList.get(0).component3());
        Log.d("sequence ----", "" + sequenceList.get(0).component4());


        for (String t : sequenceList.get(0).component5()) {
            Log.d("++++++", "" + t);
        }

        robot.getInstance().speak(TtsRequest.create("Saved the " + sequenceList.size() + " location failed.", true));
        Gson g = new Gson();

        WritableArray array = new WritableNativeArray();
        for (SequenceModel co : sequenceList) {
            JSONObject jo = new JSONObject(g.toJson(co));
            jo.put("id", co.getId());
            jo.put("name", co.getName());
            jo.put("desc", co.getDescription());
            WritableMap wm = convertJsonToMap(jo);
            array.pushMap(wm);

        }

        successCallback.invoke(array);
    }

    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException
    {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext())
        {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    /**
     * for temi movements
     */
    @ReactMethod
    public void temiMovement(float x, float y) {
        robot.skidJoy(x, y);
    }

    @ReactMethod
    public void stopMovement() {
        robot.stopMovement();
    }

    @ReactMethod
    public void goToLocation(String Location) {
        robot.goTo(Location);
    }

    @ReactMethod
    public void getAllLocation(final Promise promise) {
        try {
            StringBuffer str1 = new StringBuffer();
            List<String> getAllLocation = new ArrayList<>();
            for (String str : robot.getLocations()) {
                str1.append(str);
                getAllLocation.add(str);
            }
            //Toast.makeText(getReactApplicationContext(), "location:"+str1.toString()+ " ",Toast.LENGTH_LONG).show();
            WritableArray promiseArray = Arguments.createArray();
            for (int i = 0; i < getAllLocation.size(); i++) {
                promiseArray.pushString(getAllLocation.get(i));
            }
            // Toast.makeText(getReactApplicationContext(), "Promise::"+promiseArray+ " ",Toast.LENGTH_LONG).show();
            promise.resolve(promiseArray);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    /**
     * This is an example of saving locations. 1
     **/
    @ReactMethod
    public void saveLocation(String location) {
        boolean result = robot.saveLocation(location);
        if (result) {
            robot.speak(TtsRequest.create("I've successfully saved the " + location + " location.", true));
        } else {
            robot.speak(TtsRequest.create("Saved the " + location + " location failed.", true));
        }
        // hideKeyboard();
    }

    /**
     * tiltAngle controls temi's head by specifying which angle you want
     * to tilt to and at which speed.
     */

    @ReactMethod
    public void tiltAngle(int degree, float angle) {
        robot.tiltAngle(degree, angle);//--
    }

    /**
     * turnBy allows for turning the robot around in place. You can specify
     * the amount of degrees to turn by and at which speed.
     */

    @ReactMethod
    public void turnBy(int degree, float speed) {
        robot.turnBy(degree, speed);
    }

    /**
     * tiltBy is used to tilt temi's head from its current position.
     */

    @ReactMethod
    public void tiltBy(int degree, float speed) {
        robot.tiltBy(degree, speed);
    }

    @ReactMethod
    public void getVolume(final Promise promise) {
        try {
            int volume = robot.getVolume();
            promise.resolve(volume);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void showTopBar()
    {
        robot.showTopBar();
    }

    @ReactMethod
    public void hideTopBar() {

        Log.i("Hide top bar", "" );
        robot.hideTopBar();

    }

    @ReactMethod
    public void goToPosition(float x, float y, float yaw, int tiltAngle) {
        Log.i("Inside Go To Position", "" );
        Position position = new Position(x,y,yaw,tiltAngle);
        robot.goToPosition(position);
    }

    @ReactMethod
    public void startPage() {
        Log.i("call MAP EDitor", "" );

        robot.startPage(MAP_EDITOR);
        Log.i("call MAP EDitor EnD", "" );
    }

    @ReactMethod
    public void getTemiNickName(final Promise promise) {
        try {
            String name = robot.getNickName();
            promise.resolve(name);
        } catch (Exception e) {
            promise.reject(e);
        }

    }

    @ReactMethod
    public void getTemiSerialNumber(final Promise promise) {
        try {
            promise.resolve(robot.getSerialNumber());

            // requestMap();

        } catch (Exception e) {
            promise.reject(e);
        }

    }

    @ReactMethod
    public void requestRobotMap(final Promise promise) {
        try{

            Log.d("map Model ----", "Inside Request RobotMap Method");

            List<Permission> permissions = new ArrayList<>();
            permissions.add(Permission.MAP);
            robot.requestPermissions(permissions, 4);

            MapDataModel mapModel = robot.getMapData();
            MapImage k = mapModel.getMapImage();

            Gson g = new Gson();

            ArrayList<Integer> ik = new ArrayList<Integer>();
            for (Integer t : k.getData()) {
                ik.add(Color.argb((int) (t * 2.55), 0, 0, 0));
            }

            int[] ret = new int[ik.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ik.get(i);
            }


            Bitmap b2 = Bitmap.createBitmap(ret, k.getCols(), k.getRows(), Bitmap.Config.ARGB_8888);

            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            //add support for jpg and more.
            b2.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream2);
            byte[] byteArray2 = byteArrayOutputStream2.toByteArray();

            String encoded2 = Base64.encodeToString(byteArray2, Base64.DEFAULT);

            Log.d("encoded", "value ARGB_8888--------->" + encoded2+"-----END-------");

            promise.resolve(encoded2);

        } catch (Exception e)
        {
            Log.d("Sequene error", "Error" + e);
            promise.reject(e);
        }
    }


    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int descriptionId, @NotNull String description) {
        Log.i("GoToStabolerplate", "status=" + status + ", descriptionId=" + location + ", description=" + description);
        //robot.speak(TtsRequest.create(description, false));
        WritableMap params = Arguments.createMap();
        String locStatus= "";
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                locStatus ="Starting";
                // robot.speak(TtsRequest.create("Starting", false));
                break;

            case OnGoToLocationStatusChangedListener.CALCULATING:
                locStatus ="Calculating";
                //  robot.speak(TtsRequest.create("Calculating", false));
                break;



            case OnGoToLocationStatusChangedListener.GOING:
                locStatus ="Going";
                //   robot.speak(TtsRequest.create("Going", false));
                break;

            case OnGoToLocationStatusChangedListener.COMPLETE:
                locStatus ="Completed";
                //  robot.speak(TtsRequest.create("Completed", false));
                break;

            case OnGoToLocationStatusChangedListener.ABORT:
                locStatus ="Cancelled";

                //    robot.speak(TtsRequest.create("Cancelled", false));
                break;



        }

        String statusValue = status+"_"+location;
        Log.i("passing event data ::",statusValue);
        params.putString("eventProperty",statusValue);
        // params.putString("location",location);

        sendEvent(reactContext, "locationStatusChanged", params);
    }

    @Override
    public void onSequencePlayStatusChanged(int status) {
        Log.i("Sequencelistener", "status=" + status  );
        String sequenceStat ="";
        WritableMap params = Arguments.createMap();
        if (status == OnSequencePlayStatusChangedListener.ERROR)
            sequenceStat ="ERROR";
        else if( status == OnSequencePlayStatusChangedListener.IDLE)
            sequenceStat ="COMPLETED";
        else if( status == OnSequencePlayStatusChangedListener.PLAYING)
            sequenceStat ="PLAYING";
        else if( status == OnSequencePlayStatusChangedListener.PREPARING)
            sequenceStat ="PREPARING";
        else
            sequenceStat ="VOID";
        // robot.speak(TtsRequest.create("Playing Sequence log"+ sequenceStat, false));

        Log.i
                ("Sequencelistener", "sequenceStatus=" + sequenceStat  );
        params.putString("eventProperty",sequenceStat);
        // params.putString("location",location);

        sendEvent(reactContext, "sequencePlayStatusChanged", params);
    }

    public void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    @Override
    public void onHostResume() {

    }
    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {

    }

    @Override
    public void onCurrentPositionChanged(@NotNull Position position) {
        // Log.i("CurrentPositionChanged", "position=" + position  );
        String positionStat ="";
        WritableMap params = Arguments.createMap();

        String statusValue =  String.valueOf(position.getX())+"_"+ String.valueOf(position.getY());
        // Log.i("passing position data :",statusValue);
        params.putString("eventProperty",statusValue);

        sendEvent(reactContext, "currentPositionChanged", params);
    }

}