package com.example.paquetdriver;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

// classes needed to launch navigation UI
import android.view.View;
import android.widget.Button;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;

public class Main2Activity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "Main2Activity";
    private NavigationMapRoute navigationMapRoute;
    // variables needed to initialize navigation
    private Button button;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    @Nullable List<Map<String, Object>> orders;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main2);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        orders = new ArrayList<Map<String, Object>>();

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        InitNavBar();
    }

        private void InitNavBar(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        final View headerView = navigationView.getHeaderView(0);
        TextView header_text = headerView.findViewById(R.id.header_text);
        header_text.setText(firebaseAuth.getCurrentUser().getDisplayName());
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
//                        //menuItem.setChecked(true);

                        int id = menuItem.getItemId();
                        switch(id)
                        {
                            case R.id.nav_help:
                                Intent i = new Intent(Main2Activity.this, Help.class);
                                startActivity(i);
                                break;
                            case R.id.nav_new_orders:
                                i = new Intent(Main2Activity.this, NewOrders.class);
                                startActivity(i);
                                break;

                            case R.id.nav_ongoing_orders:
                                i = new Intent(Main2Activity.this, OngoingOrders.class);
                                startActivity(i);
                                break;

                            case R.id.nav_settings:
                                i = new Intent(Main2Activity.this, Settings.class);
                                startActivity(i);
                                break;
                            default:
                                return true;
                        }
                        // close drawer when item is tapped
                        drawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });

        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        Menu m = navView.getMenu();
        for (int i=0;i<m.size();i++) {
            MenuItem mi = m.getItem(i);

            //for aapplying a font to subMenu ...
            SubMenu subMenu = mi.getSubMenu();
            if (subMenu!=null && subMenu.size() >0 ) {
                for (int j=0; j <subMenu.size();j++) {
                    MenuItem subMenuItem = subMenu.getItem(j);
                    applyFontToMenuItem(subMenuItem);
                }
            }

            //the method we have create in activity
            applyFontToMenuItem(mi);
        }
    }

        private void applyFontToMenuItem(MenuItem mi) {
        Typeface font = ResourcesCompat.getFont(getApplicationContext(), R.font.opensans_light);
        SpannableString mNewTitle = new SpannableString(mi.getTitle());
        mNewTitle.setSpan(new CustomTypefaceSpan("" , font), 0 , mNewTitle.length(),  Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mi.setTitle(mNewTitle);
    }


    private void GetAllTBPO(@Nullable Style style){
        CollectionReference root = db.collection("AppRoot");

        //TODO:get user id and put in place of orders
        DocumentReference dr = root.document("ToBePickedupOrders");
        CollectionReference ordersCollection = dr.collection("AllOrders");

        ordersCollection.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                Log.d(TAG, document.getId() + " => " + data);
                                orders.add(data);
                            }

                            Toast.makeText(Main2Activity.this, String.valueOf(orders.size()), Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        GetAnOrder(style);
                    }
                });


    }

    private void GetAnOrder(@Nullable Style style){
            Toast.makeText(Main2Activity.this, "INIT!", Toast.LENGTH_LONG).show();
        List<Location> ordersLocation =  new ArrayList<Location>();
        List<Float> distances = new ArrayList<Float>();
        Location userLoc = new Location("");

        userLoc = enableLocationComponent(mapboxMap.getStyle());

        for(Map<String, Object> order: orders){
            Location loc = new Location("");
            loc.setLatitude(Double.valueOf(order.get("pickup_lat").toString()));
            loc.setLongitude(Double.valueOf(order.get("pickup_lon").toString()));
            ordersLocation.add(loc);
            distances.add(loc.distanceTo(userLoc));
        }
        Toast.makeText(Main2Activity.this, "Distance = " + distances.get(0).toString(), Toast.LENGTH_LONG).show();
        float a, b;
        for (int i = 0; i < distances.size(); i++){
            for (int j = 0; j < distances.size()-i-1; j++){

                if (distances.get(j) > distances.get(j+1)){
                        float temp = distances.get(j);
                        distances.set(j, distances.get(j+1));
                        distances.set(j+1, temp);

                        Map<String, Object> tempM = orders.get(j);
                     orders.set(j, orders.get(j+1));
                     orders.set(j+1, tempM);
                }

            }
        }

        Point o = Point.fromLngLat(userLoc.getLongitude(), userLoc.getLatitude());
        Point d = Point.fromLngLat(Double.valueOf(orders.get(0).get("pickup_lon").toString()),Double.valueOf(orders.get(0).get("pickup_lat").toString()));
        //Point d = Point.fromLngLat(o.longitude() + 1,o.latitude()+9);
        Log.d(TAG, o.toJson());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(d));
        }

        getRoute(o, d, style);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                mapboxMap.addOnMapClickListener(Main2Activity.this);
//                button = findViewById(R.id.startbutton);
//                button.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        boolean simulateRoute = true;
//                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
//                                .directionsRoute(currentRoute)
//                                .shouldSimulateRoute(simulateRoute)
//                                .build();
//                        // Call this method with Context from within an Activity
//                        NavigationLauncher.startNavigation(Main2Activity.this, options);
//                    }
//                });
                GetAllTBPO(style);


            }
        });
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

//        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
//        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
//                locationComponent.getLastKnownLocation().getLatitude());
//
//        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
//        if (source != null) {
//            source.setGeoJson(Feature.fromGeometry(destinationPoint));
//        }
//
//        getRoute(originPoint, destinationPoint);
//        button.setEnabled(true);
//        button.setBackgroundResource(R.color.mapboxBlue);
        return true;
    }

    private void getRoute(Point origin, Point destination, @Nullable Style style) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                        Toast.makeText(Main2Activity.this, "Rouute = " + destination.toJson(), Toast.LENGTH_LONG).show();
//                        style.addImage("marker-icon-id",
//                                BitmapFactory.decodeResource(
//                                        Main2Activity.this.getResources(), R.drawable.mapbox_marker_icon_default));
//
//                        GeoJsonSource geoJsonSource = new GeoJsonSource("source-id", Feature.fromGeometry(
//                                destination));
//                        style.addSource(geoJsonSource);
//
//                        SymbolLayer symbolLayer = new SymbolLayer("layer-id", "source-id");
//                        symbolLayer.withProperties(
//                                PropertyFactory.iconImage("marker-icon-id")
//                        );
//                        style.addLayer(symbolLayer);
//                                        boolean simulateRoute = true;
//                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
//                        .directionsRoute(currentRoute)
//                        .shouldSimulateRoute(simulateRoute)
//                        .build();
//                        // Call this method with Context from within an Activity
//                        NavigationLauncher.startNavigation(Main2Activity.this, options);
                        boolean simulateRoute = true;
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(simulateRoute)
                                .build();
                        // Call this method with Context from within an Activity
                        NavigationLauncher.startNavigation(Main2Activity.this, options);

                        //navigationMapRoute.addProgressChangeListener();
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private Location enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            return locationComponent.getLastKnownLocation();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
//
//public class Main2Activity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener  {
//
//    private DrawerLayout drawerLayout;
//    private FirebaseAuth firebaseAuth;
//    private FirebaseFirestore db;
//    private MapView mapView;
//    private MapboxMap mapboxMap;
//    private PermissionsManager permissionsManager;
//    private LocationEngine locationEngine;
////    private LocationLayerPlugin locationLayerPlugin;
//    private Location originLocation;
//    private com.mapbox.services.android.navigation.ui.v5.NavigationView nv;
//
//    private AutoCompleteTextView pickup_autocomplete, drop_autocomplete;
//
//    private static final String MARKER_SOURCE = "markers-source";
//    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
//    private static final String MARKER_IMAGE = "pickup_marker";
//
//    private static final String TAG = "Main2Activity";
//
//
//    private Point origin, destination;
//    private Style style;
//
//    //For route
//    private static final String ROUTE_LAYER_ID = "route-layer-id";
//    private static final String ROUTE_SOURCE_ID = "route-source-id";
//    private static final String ICON_LAYER_ID = "icon-layer-id";
//    private static final String ICON_SOURCE_ID = "icon-source-id";
//    private static final String RED_PIN_ICON_ID = "red-pin-icon-id";
//    private DirectionsRoute currentRoute;
//    private MapboxDirections client;
//    MapboxNavigation navigation;
//
//    //Order Related
//    private static final String TO_BE_PICKED_UP= "To Be Picked Up!";
//    private ConstraintLayout order_init;
//    private EditText order_quantity;
//    List<Map<String, Object>> orders;
//
//    LocationComponent userLocation;
//    DirectionsRoute route;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Mapbox.getInstance(this, "pk.eyJ1IjoicmVka2F5IiwiYSI6ImNqN2lpb2xjeTF0MTgzMm5wamY2NXJ2emcifQ.yHlWsKDNfEd-7vKTysjjqQ");
//        navigation = new MapboxNavigation(this, "pk.eyJ1IjoicmVka2F5IiwiYSI6ImNqN2lpb2xjeTF0MTgzMm5wamY2NXJ2emcifQ.yHlWsKDNfEd-7vKTysjjqQ");
//        setContentView(R.layout.activity_main2);
//        drawerLayout = findViewById(R.id.drawer_layout);
//        order_init = (ConstraintLayout) findViewById(R.id.order_init);
//        order_quantity = (EditText) findViewById(R.id.quantity);
//
//
//        // Initialize Firebase Auth
//        firebaseAuth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//        orders = new ArrayList<Map<String, Object>>();
//
//        InitNavBar();
//
//        mapView = findViewById(R.id.mapView);
//        mapView.onCreate(savedInstanceState);
//
//        InitMapView();
//
//        InitPickUpAC();
//        InitDropAC();
//
//        GetAllTBPO();
//
//    }
//
//    public static void hideKeyboard(Activity activity) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
//        View view = activity.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
//        if (view == null) {
//            view = new View(activity);
//        }
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//    }
//
//    private void GetAllTBPO(){
//        CollectionReference root = db.collection("AppRoot");
//
//        //TODO:get user id and put in place of orders
//        DocumentReference dr = root.document("ToBePickedupOrders");
//        CollectionReference ordersCollection = dr.collection("AllOrders");
//
//        ordersCollection.get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.d(TAG, document.getId() + " => " + document.getData());
//                                orders.add(document.getData());
//                            }
//
//                            Toast.makeText(Main2Activity.this, String.valueOf(orders.size()), Toast.LENGTH_LONG).show();
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                        GetAnOrder();
//                    }
//                });
//
//
//    }
//
//    private void GetAnOrder(){
//        List<Location> ordersLocation =  new ArrayList<Location>();
//        List<Float> distances = new ArrayList<Float>();
//        Location userLoc = new Location("");
//        userLoc = enableLocationComponent(style);
//
//        for(Map<String, Object> order: orders){
//            Location loc = new Location("");
//            loc.setLatitude(Double.valueOf(order.get("pickup_lat").toString()));
//            loc.setLongitude(Double.valueOf(order.get("pickup_lon").toString()));
//            ordersLocation.add(loc);
//            distances.add(loc.distanceTo(userLoc));
//        }
//        Toast.makeText(Main2Activity.this, "Distance = " + distances.get(0).toString(), Toast.LENGTH_LONG).show();
//        float a, b;
//        for (int i = 0; i < distances.size(); i++){
//            for (int j = 0; j < distances.size()-i-1; j++){
//
//                if (distances.get(j) > distances.get(j+1)){
//                        float temp = distances.get(j);
//                        distances.set(j, distances.get(j+1));
//                        distances.set(j+1, temp);
//
//                        Map<String, Object> tempM = orders.get(j);
//                     orders.set(j, orders.get(j+1));
//                     orders.set(j+1, tempM);
//                }
//
//            }
//        }
//
//        Point o = Point.fromLngLat(userLoc.getLatitude(), userLoc.getLongitude());
//        //Point d = Point.fromLngLat(Double.valueOf(orders.get(0).get("pickup_lat").toString()),Double.valueOf(orders.get(0).get("pickup_lon").toString()));
//        Point d = Point.fromLngLat(o.longitude() + 1,o.latitude()+9);
//        Log.d(TAG, o.toJson());
//
//        NavigationRoute.builder(this)
//                .accessToken("pk.eyJ1IjoicmVka2F5IiwiYSI6ImNqN2lpb2xjeTF0MTgzMm5wamY2NXJ2emcifQ.yHlWsKDNfEd-7vKTysjjqQ")
//                .origin(o)
//                .destination(d)
//                .build()
//                .getRoute(new Callback<DirectionsResponse>() {
//                    @Override
//                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                        if (response.body() == null || response.body().routes().size() < 1) {
//                            return;
//                        }
//                        Toast.makeText(Main2Activity.this, "Response = " + response.body().routes().get(0), Toast.LENGTH_LONG).show();
//                    }
//
//                    @Override
//                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//                        Toast.makeText(Main2Activity.this, "onFailure = " + t.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                });
////
////        DirectionsRoute route = ...
////
////        boolean simulateRoute = true;
////
////        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
////                .directionsRoute(route)
////                .shouldSimulateRoute(simulateRoute)
////                .build();
////        NavigationLauncher.startNavigation(this, options);
//
//    }
//
//    private void InitNavBar(){
//        //        Toolbar toolbar = findViewById(R.id.toolbar);
////        setSupportActionBar(toolbar);
////        ActionBar actionbar = getSupportActionBar();
////        actionbar.setDisplayHomeAsUpEnabled(true);
////        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
//
//        NavigationView navigationView = findViewById(R.id.nav_view);
//        final View headerView = navigationView.getHeaderView(0);
//        TextView header_text = headerView.findViewById(R.id.header_text);
//        header_text.setText(firebaseAuth.getCurrentUser().getDisplayName());
//        navigationView.setNavigationItemSelectedListener(
//                new NavigationView.OnNavigationItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem menuItem) {
//                        // set item as selected to persist highlight
////                        //menuItem.setChecked(true);
//
//                        int id = menuItem.getItemId();
//                        switch(id)
//                        {
//                            case R.id.nav_help:
//                                WebView myWebView = new WebView(Main2Activity.this);
//                                setContentView(myWebView);
//                                myWebView.loadUrl("https://paquetsupport.000webhostapp.com/");
//                                break;
//                            default:
//                                return true;
//                        }
//                        // close drawer when item is tapped
//                        drawerLayout.closeDrawers();
//
//                        // Add code here to update the UI based on the item selected
//                        // For example, swap UI fragments here
//
//                        return true;
//                    }
//                });
//
//        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
//        Menu m = navView.getMenu();
//        for (int i=0;i<m.size();i++) {
//            MenuItem mi = m.getItem(i);
//
//            //for aapplying a font to subMenu ...
//            SubMenu subMenu = mi.getSubMenu();
//            if (subMenu!=null && subMenu.size() >0 ) {
//                for (int j=0; j <subMenu.size();j++) {
//                    MenuItem subMenuItem = subMenu.getItem(j);
//                    applyFontToMenuItem(subMenuItem);
//                }
//            }
//
//            //the method we have create in activity
//            applyFontToMenuItem(mi);
//        }
//    }
//
//    private void InitMapView(){
//        //Mapbox shit
////        mapView.getMapAsync(new OnMapReadyCallback() {
////            @Override
////            public void onMapReady(@NonNull MapboxMap mapboxMap) {
////                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
////                    @Override
////                    public void onStyleLoaded(@NonNull Style style) {
////                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
////                        // Add the marker image to map
////                        style.addImage("marker-icon-id",
////                                BitmapFactory.decodeResource(
////                                        Main2Activity.this.getResources(), R.drawable.mapbox_marker_icon_default));
////
////                        GeoJsonSource geoJsonSource = new GeoJsonSource("source-id", Feature.fromGeometry(
////                                Point.fromLngLat(72.837506, 19.109941)));
////                        style.addSource(geoJsonSource);
////
////                        SymbolLayer symbolLayer = new SymbolLayer("layer-id", "source-id");
////                        symbolLayer.withProperties(
////                                PropertyFactory.iconImage("marker-icon-id")
////                        );
////                        style.addLayer(symbolLayer);
////                    }
////                });
////            }
////        });
//        mapView.getMapAsync(this);
//    }
//
//    private void InitPickUpAC(){
//        final GeocoderAdapter adapter = new GeocoderAdapter(this);
//        pickup_autocomplete = (AutoCompleteTextView) findViewById(R.id.pickup_auto_comp);
//        pickup_autocomplete.setLines(1);
//        pickup_autocomplete.setAdapter(adapter);
//        pickup_autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                CarmenFeature result = adapter.getItem(position);
//                pickup_autocomplete.setText(result.text());
//                //updateMap(result.prope, result.getLongitude());
//                Toast.makeText(Main2Activity.this, "result: " + result.center().toString(), Toast.LENGTH_LONG).show();
//                IconFactory iconFactory = IconFactory.getInstance(Main2Activity.this);
//                Icon icon = iconFactory.fromResource(R.drawable.pickup_marker);
//                mapboxMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(result.center().latitude(), result.center().longitude()))
//                        .icon(icon)).setTitle("Pick-up!");
//                origin = result.center();
//            }
//        });
//    }
//
//    private void InitDropAC(){
//        final GeocoderAdapter adapter = new GeocoderAdapter(this);
//        drop_autocomplete = (AutoCompleteTextView) findViewById(R.id.drop_auto_comp);
//        drop_autocomplete.setLines(1);
//        drop_autocomplete.setAdapter(adapter);
//        drop_autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                CarmenFeature result = adapter.getItem(position);
//                drop_autocomplete.setText(result.text());
//                //updateMap(result.prope, result.getLongitude());
//                Toast.makeText(Main2Activity.this, "result: " + result.center().toString(), Toast.LENGTH_LONG).show();
//                IconFactory iconFactory = IconFactory.getInstance(Main2Activity.this);
//                Icon icon = iconFactory.fromResource(R.drawable.pickup_marker);
//                mapboxMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(result.center().latitude(), result.center().longitude()))
//                        .icon(icon)).setTitle("Drop!");
//                destination = result.center();
//                //InitRoute(origin, destination);
//                initSource(style);
//
//                initLayers(style);
//
//// Get the directions route from the Mapbox Directions API
//                getRoute(style, origin, destination);
//                hideKeyboard(Main2Activity.this);
//                order_init.setVisibility(View.VISIBLE);
//
//
//            }
//        });
//    }
//
//    public void ConfirmOrder(View view){
//        float order_quan = Float.parseFloat(order_quantity.getText().toString());
//        String order_name = "Order" + order_quan, order_desc = "Orderdesc" + order_quan;
//        CollectionReference root = db.collection("AppRoot");
//
//        //TODO:get user id and put in place of orders
//        DocumentReference dr = root.document("User");
//
//        CollectionReference userIDCollection = dr.collection(Objects.requireNonNull(firebaseAuth.getUid()));
//        dr = userIDCollection.document("Orders");
//        Toast.makeText(Main2Activity.this, dr.getId().toString(), Toast.LENGTH_LONG).show();
//        Map<String, Object> order = new HashMap<>();
//        order.put("name", order_name);
//        order.put("desc", order_desc);
//        order.put("quantity", String.valueOf(order_quan));
//        order.put("pickup_lat", origin.latitude());
//        order.put("pickup_lon", origin.longitude());
//        order.put("drop_lat", destination.latitude());
//        order.put("drop_lon", destination.longitude());
//        order.put("status", TO_BE_PICKED_UP);
//        order.put("driver", "None");
//        order.put("price", "10");
//        order.put("payment_method", "Paytm");
//
//        dr.collection("AllOrders").add(order)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Toast.makeText(Main2Activity.this,"Order Confirmed" + documentReference.getId(), Toast.LENGTH_LONG).show();
//                        //tv.setText(documentReference.getId());
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(Main2Activity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
//
//
//    }
//
//    private void InitRoute(Point origin, Point destination){
//        NavigationRoute.builder(getApplicationContext())
//                .accessToken("pk.eyJ1IjoicmVka2F5IiwiYSI6ImNqN2lpb2xjeTF0MTgzMm5wamY2NXJ2emcifQ.yHlWsKDNfEd-7vKTysjjqQ")
//                .origin(origin)
//                .destination(destination)
//                .build()
//                .getRoute(new Callback<DirectionsResponse>() {
//                    @Override
//                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//
//                    }
//
//                    @Override
//                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//
//                    }
//                });
//    }
//
//    /**
//     * Add the route and marker sources to the map
//     */
//    private void initSource(@NonNull Style loadedMapStyle) {
//        if(loadedMapStyle.getSource(ROUTE_SOURCE_ID) == null){
//            loadedMapStyle.addSource(new GeoJsonSource(ROUTE_SOURCE_ID,
//                    FeatureCollection.fromFeatures(new Feature[] {})));
//        }
//
//
//        GeoJsonSource iconGeoJsonSource = new GeoJsonSource(ICON_SOURCE_ID, FeatureCollection.fromFeatures(new Feature[] {
//                Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
//                Feature.fromGeometry(Point.fromLngLat(destination.longitude(), destination.latitude()))}));
//        if(loadedMapStyle.getSource(ROUTE_SOURCE_ID) == null)
//            loadedMapStyle.addSource(iconGeoJsonSource);
//    }
//
//    /**
//     * Add the route and maker icon layers to the map
//     */
//    private void initLayers(@NonNull Style loadedMapStyle) {
//        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
//
//// Add the LineLayer to the map. This layer will display the directions route.
//        routeLayer.setProperties(
//                lineCap(Property.LINE_CAP_ROUND),
//                lineJoin(Property.LINE_JOIN_ROUND),
//                lineWidth(5f),
//                lineColor(getResources().getColor(R.color.colorPrimary))
//        );
//        if(loadedMapStyle.getLayer(ROUTE_LAYER_ID) == null)
//            loadedMapStyle.addLayer(routeLayer);
//
//// Add the red marker icon image to the map
//        if(loadedMapStyle.getImage(MARKER_IMAGE) == null){
//            loadedMapStyle.addImage(MARKER_IMAGE, BitmapUtils.getBitmapFromDrawable(
//                    getResources().getDrawable(R.drawable.pickup_marker)));
//        }
//
//// Add the red marker icon SymbolLayer to the map
//        if(loadedMapStyle.getLayer(ICON_LAYER_ID) == null){
//            loadedMapStyle.addLayer(new SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
//                    iconImage(RED_PIN_ICON_ID),
//                    iconIgnorePlacement(true),
//                    iconIgnorePlacement(true),
//                    iconOffset(new Float[] {0f, -4f})));
//        }
//    }
//
//    /**
//     * Make a request to the Mapbox Directions API. Once successful, pass the route to the
//     * route layer.
//     *
//     * @param origin      the starting point of the route
//     * @param destination the desired finish point of the route
//     */
//    private void getRoute(@NonNull final Style style, Point origin, Point destination) {
//
//        client = MapboxDirections.builder()
//                .origin(origin)
//                .destination(destination)
//                .overview(DirectionsCriteria.OVERVIEW_FULL)
//                .profile(DirectionsCriteria.PROFILE_DRIVING)
//                .accessToken(getString(R.string.access_token))
//                .build();
//
//        client.enqueueCall(new Callback<DirectionsResponse>() {
//            @Override
//            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                System.out.println(call.request().url().toString());
//
//// You can get the generic HTTP info about the response
//                Timber.d("Response code: " + response.code());
//                if (response.body() == null) {
//                    Timber.e("No routes found, make sure you set the right user and access token.");
//                    return;
//                } else if (response.body().routes().size() < 1) {
//                    Timber.e("No routes found");
//                    return;
//                }
//
//// Get the directions route
//                currentRoute = response.body().routes().get(0);
//
//// Make a toast which displays the route's distance
//                Toast.makeText(Main2Activity.this, currentRoute.distance().toString(), Toast.LENGTH_SHORT).show();
//
//                if (style.isFullyLoaded()) {
//// Retrieve and update the source designated for showing the directions route
//                    GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
//
//// Create a LineString with the directions route's geometry and
//// reset the GeoJSON source for the route LineLayer source
//                    if (source != null) {
//                        Timber.d("onResponse: source != null");
//                        source.setGeoJson(FeatureCollection.fromFeature(
//                                Feature.fromGeometry(LineString.fromPolyline(currentRoute.geometry(), PRECISION_6))));
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
//                Timber.e("Error: " + throwable.getMessage());
//                Toast.makeText(Main2Activity.this, "Error: " + throwable.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//
//    }
//
//    public void HelpWebView(){
//        WebView myWebView = new WebView(this);
//        setContentView(myWebView);
//        myWebView.loadUrl("https://paquetsupport.000webhostapp.com/");
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                drawerLayout.openDrawer(GravityCompat.START);
//                return true;
//
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    public void openDrawer(View view){
//        drawerLayout.openDrawer(GravityCompat.START);
//    }
//
//    private void applyFontToMenuItem(MenuItem mi) {
//        Typeface font = ResourcesCompat.getFont(getApplicationContext(), R.font.opensans_light);
//        SpannableString mNewTitle = new SpannableString(mi.getTitle());
//        mNewTitle.setSpan(new CustomTypefaceSpan("" , font), 0 , mNewTitle.length(),  Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//        mi.setTitle(mNewTitle);
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        mapView.onStart();
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        mapView.onResume();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        mapView.onPause();
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        mapView.onStop();
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mapView.onLowMemory();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mapView.onDestroy();
//        navigation.onDestroy();
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        mapView.onSaveInstanceState(outState);
//    }
//
//    @Override
//    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
//        Main2Activity.this.mapboxMap = mapboxMap;
//
//        mapboxMap.setStyle(Style.DARK,
//                new Style.OnStyleLoaded() {
//                    @Override
//                    public void onStyleLoaded(@NonNull Style _style) {
//                        enableLocationComponent(_style);
//
//                        style = _style;
////                        style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
////                                Main2Activity.this.getResources(), R.drawable.pickup_marker));
////                        addMarkers(style);
//                    }
//                });
//    }
//
//    private void addMarkers(@NonNull Style loadedMapStyle) {
//        List<Feature> features = new ArrayList<>();
//        features.add(Feature.fromGeometry(Point.fromLngLat(72.8365, 19.1157)));
//
//        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
//
//        loadedMapStyle.addSource(new GeoJsonSource(MARKER_SOURCE, FeatureCollection.fromFeatures(features)));
//
//        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
//        loadedMapStyle.addLayer(new SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
//                .withProperties(
//                        PropertyFactory.iconAllowOverlap(true),
//                        PropertyFactory.iconIgnorePlacement(true),
//                        PropertyFactory.iconImage(MARKER_IMAGE),
//// Adjust the second number of the Float array based on the height of your marker image.
//// This is because the bottom of the marker should be anchored to the coordinate point, rather
//// than the middle of the marker being the anchor point on the map.
//                        PropertyFactory.iconOffset(new Float[] {0f, -52f})
//                ));
//    }
//
//    @SuppressWarnings( {"MissingPermission"})
//    private Location enableLocationComponent(@NonNull Style loadedMapStyle) {
//// Check if permissions are enabled and if not request
//        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//
//// Get an instance of the component
//            LocationComponent locationComponent = mapboxMap.getLocationComponent();
//
//// Activate with options
//            locationComponent.activateLocationComponent(this, loadedMapStyle);
//
//// Enable to make component visible
//            locationComponent.setLocationComponentEnabled(true);
//
//// Set the component's camera mode
//            locationComponent.setCameraMode(CameraMode.TRACKING);
//
//// Set the component's render mode
//            locationComponent.setRenderMode(RenderMode.COMPASS);
//            userLocation = locationComponent;
//            return locationComponent.getLastKnownLocation();
////            IconFactory iconFactory = IconFactory.getInstance(Main2Activity.this);
////            Icon icon = iconFactory.fromResource(R.drawable.pickup_marker);
////            mapboxMap.addMarker(new MarkerOptions()
////                    .position(new LatLng(locationComponent.getLastKnownLocation().getLatitude(), locationComponent.getLastKnownLocation().getLatitude()))).setTitle("Pick-up!");
////            Toast.makeText(Main2Activity.this,"Your location: " + locationComponent.getLastKnownLocation().getLatitude() + " _ " +  + locationComponent.getLastKnownLocation().getLatitude(), Toast.LENGTH_LONG).show();
//
//        } else {
//            permissionsManager = new PermissionsManager(this);
//            permissionsManager.requestLocationPermissions(this);
//
//        }
//        return null;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
//
//    @Override
//    public void onExplanationNeeded(List<String> permissionsToExplain) {
//        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
//    }
//
//    @Override
//    public void onPermissionResult(boolean granted) {
//        if (granted) {
//            mapboxMap.getStyle(new Style.OnStyleLoaded() {
//                @Override
//                public void onStyleLoaded(@NonNull Style style) {
//                    enableLocationComponent(style);
//                }
//            });
//        } else {
//            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
//            finish();
//        }
//    }
//
//
//}
//
