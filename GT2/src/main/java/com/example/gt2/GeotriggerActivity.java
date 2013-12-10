package com.example.gt2;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.geotrigger.GeotriggerApiClient;
import com.esri.android.geotrigger.GeotriggerApiListener;
import com.esri.android.geotrigger.GeotriggerBroadcastReceiver;
import com.esri.android.geotrigger.GeotriggerService;
import com.esri.android.geotrigger.TriggerBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.gdb.GdbFeature;
import com.esri.core.gdb.GdbFeatureTable;
import com.esri.core.gdb.Geodatabase;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.table.TableException;
import com.esri.core.tasks.gdb.GenerateGeodatabaseParameters;
import com.esri.core.tasks.gdb.GeodatabaseStatusCallback;
import com.esri.core.tasks.gdb.GeodatabaseStatusInfo;
import com.esri.core.tasks.gdb.GeodatabaseTask;
import com.esri.core.tasks.gdb.SyncGeodatabaseParameters;
import com.esri.core.tasks.gdb.SyncModel;

public class GeotriggerActivity extends Activity implements
        GeotriggerBroadcastReceiver.LocationUpdateListener,
        GeotriggerBroadcastReceiver.DeviceReadyListener {
    private static final String TAG = "GeotriggerActivity";
    private static final int PLAY_SERVICES_REQUEST_CODE = 1;

    // Create a new application at https://developers.arcgis.com/en/applications
    private static final String AGO_CLIENT_ID = "5wWb5MBvsA7KTc8S";

    // The project number from https://code.google.com/apis/console
    private static final String GCM_SENDER_ID = "25538359709";

    // The GeotriggerBroadcastReceiver receives intents from the
    // GeotriggerService, calling any listeners implemented in your class.
    private GeotriggerBroadcastReceiver mGeotriggerBroadcastReceiver;

    private boolean mShouldCreateTrigger;

    private MapView mMapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView)findViewById(R.id.map);
        // Add dynamic layer to MapView
        //mMapView.addLayer(new ArcGISTiledMapServiceLayer("" +"http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer"));
        mGeotriggerBroadcastReceiver = new GeotriggerBroadcastReceiver();
        mShouldCreateTrigger = false;




        JSONObject params = new JSONObject();
        try {
            params.put("addTags", "Campus");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating device update parameters.", e);
        }

        GeotriggerApiClient.runRequest(this, "device/update", params, new GeotriggerApiListener() {
            @Override
            public void onSuccess(JSONObject data) {
                Log.d(TAG, "Device updated: " + data.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, "Failed to update device.", error);
            }
        });

        LocationService locationService = mMapView.getLocationService();
        locationService.setSymbol(new SimpleMarkerSymbol(Color.BLUE, 15, SimpleMarkerSymbol.STYLE.CIRCLE)); //setSymbol(new PictureMarkerSymbol(getResources().getDrawable(R.drawable.ic_maps_indicator_current_position_anim3)));
        locationService.setAccuracyCircleOn(false);
        locationService.setAutoPan(false);
        locationService.start();
    }

    @Override
    public void onStart() {
        super.onStart();

        GeotriggerHelper.startGeotriggerService(this, PLAY_SERVICES_REQUEST_CODE,
                AGO_CLIENT_ID, GCM_SENDER_ID, GeotriggerService.TRACKING_PROFILE_FINE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the receiver. Activity will no longer respond to
        // GeotriggerService intents. Tracking and push notification handling
        // will continue in the background.
        unregisterReceiver(mGeotriggerBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the receiver. The default intent filter listens for all
        // intents that the receiver can handle. If you need to handle events
        // while the app is in the background, you must register the receiver
        // in the manifest.
        // See: http://esri.github.io/geotrigger-docs/android/handling-events/
        registerReceiver(mGeotriggerBroadcastReceiver,
                GeotriggerBroadcastReceiver.getDefaultIntentFilter());

    }

    @Override
    public void onDeviceReady() {
        // Called when the device has registered with ArcGIS Online and is ready
        // to make requests to the Geotrigger Service API.
        Toast.makeText(this, "Device Registered!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Device registered!");
    }

    @Override
    public void onLocationUpdate(Location loc, boolean isOnDemand) {
        // Called with the GeotriggerService obtains a new location update from
        // Android's native location services. The isOnDemand parameter lets you
        // determine if this location update was a result of calling
        // GeotriggerService.requestOnDemandUpdate()

//        Point p = new Point(loc.getLatitude(),loc.getLongitude());
//        Point point = (Point) GeometryEngine.project(p,
//                SpatialReference.create(4326),
//                MapView.getSpatialReference());




        Toast.makeText(this, "Location Update Received!" + GeotriggerService.getDeviceId(this),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, String.format("Location update received: (%f, %f)",
                loc.getLatitude(), loc.getLongitude()));

        // Create the trigger if we haven't done so already.
        if (mShouldCreateTrigger) {
            // Set create trigger flag here so that we don't create multiple
            // triggers if we get a few initial updates in rapid succession.
            mShouldCreateTrigger = false;

            // The TriggerBuilder helps build JSON parameters for use with the
            // 'trigger/create' API route.
            JSONObject params = new TriggerBuilder()
                    .setTags(GeotriggerService.getDeviceDefaultTag(this))
                    .setGeo(loc, 100)
                    .setDirection(TriggerBuilder.DIRECTION_LEAVE)
                    .setNotificationText("You left the trigger!")
                    .build();

            // Send the request to the Geotrigger API.
            GeotriggerApiClient.runRequest(this, "trigger/create", params,
                    new GeotriggerApiListener() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Toast.makeText(GeotriggerActivity.this, "Trigger created!",
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Trigger Created");
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            Log.d(TAG, "Error creating trigger!", e);
                            // It didn't work, so we need to try again
                            mShouldCreateTrigger = true;
                        }
                    });
        }


//        //Push notification
//        JSONObject params2 = new JSONObject();
//        try {
//            params2.put("text", "Push notifications are working!");
//            params2.put("url", "http://developers.arcgis.com");
//        } catch (JSONException e) {
//            Log.e(TAG, "Error creating device/notify params", e);
//        }
//
//        GeotriggerApiClient.runRequest(this, "device/notify", params2, new GeotriggerApiListener() {
//            @Override
//            public void onSuccess(JSONObject json) {
//                Log.i(TAG, "device/notify success: " + json);
//            }
//            @Override
//            public void onFailure(Throwable error) {
//                Log.e(TAG, "device/notify failure", error);
//            }
//        });


    }
}