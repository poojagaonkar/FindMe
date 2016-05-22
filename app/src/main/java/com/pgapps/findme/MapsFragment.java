package com.pgapps.findme;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pgapps.findme.Services.GPSTracker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MapsFragment extends Fragment implements OnMapReadyCallback ,LocationListener {

    private GoogleMap mMap;
    private int personIcon, busIcon, foodIcon, railIcon, atmIcon, hospitalIcon, pharmacyIcon, airportIcon, moviesIcon, fuelIcon;
    private LocationManager locMan;
    private Location lastLoc;
    private double lat, lng;
    private LatLng lastLatLng;
    private Marker userMarker;
    private View mView;
    private GPSTracker gps;
    private String placesSearchStr;
    private Marker[] placeMarkers;
    private final int MAX_PLACES = 20;
    private MarkerOptions[] places;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Nullable
    @Override

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_maps,container, false);
    }

    @Override
    @TargetApi(23)
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =  (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        placeMarkers = new Marker[MAX_PLACES];

        busIcon = R.mipmap.ic_directions_bus;
        foodIcon = R.mipmap.ic_local_dining;
        railIcon = R.mipmap.ic_directions_rail;
        atmIcon = R.mipmap.ic_local_atm;
        hospitalIcon = R.mipmap.ic_local_hospital;
        pharmacyIcon = R.mipmap.ic_local_pharmacy;
        airportIcon = R.mipmap.ic_flight_white;
        moviesIcon = R.mipmap.ic_local_movies;
        fuelIcon = R.mipmap.ic_ev_station;
        personIcon = R.mipmap.ic_person;

        locMan = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);

        gps = new GPSTracker(getActivity());

        // check if GPS enabled
        if(gps.canGetLocation()){

             lat = gps.getLatitude();
             lng = gps.getLongitude();


        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(getActivity()).addApi(AppIndex.API).build();
    }

    private void updatePlaces() {

        if(userMarker!=null) userMarker.remove();
        userMarker = mMap.addMarker(new MarkerOptions()
                .position(lastLatLng)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker())
                .snippet("Your last recorded location"));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng lastLatLng = new LatLng(lat,lng);
        if(userMarker!=null) userMarker.remove();
        userMarker =  mMap.addMarker(new MarkerOptions().position(lastLatLng).title("You are here"));


        mMap.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        String types = "food|hospital|airport|atm|bus_station|pharmacy|movie_theater|gas_station|train_station";
        try {
            types = URLEncoder.encode(types, "UTF-8");
        } catch (UnsupportedEncodingException e1) {

            e1.printStackTrace();
        }
       placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" +
                "json?location="+lat+","+lng+
                "&radius=1000&sensor=true" +
                "&types="+types+
                "&key=" + getActivity().getResources().getString(R.string.browser_key);
        new GetPlaces().execute(placesSearchStr);
        //locMan.requestLocationUpdates(android.LocationManager.NETWORK_PROVIDER, 30000, 100, this);


    }
    @Override
    public void onResume() {
        super.onResume();
       /* if(mMap!=null){
            locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,30000, 100, this);
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
        /*if(mMap!=null){
            locMan.removeUpdates(this);
        }*/
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.pgapps.findme/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.pgapps.findme/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("MyMapActivity", "location changed");
        onMapReady(mMap);
    }


    private class GetPlaces extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... placesURL) {

            String placeResult = null;
            StringBuilder placesBuilder = new StringBuilder();
            //process search parameter string(s)
            for (String placeSearchURL : placesURL) {
            //execute search
            HttpClient placesClient = new DefaultHttpClient();

                try {
                    //try to fetch the data
                    HttpGet placesGet = new HttpGet(placeSearchURL);
                    HttpResponse placesResponse = placesClient.execute(placesGet);
                    StatusLine placeSearchStatus = placesResponse.getStatusLine();
                    if (placeSearchStatus.getStatusCode() == 200) {
                    //we have an OK response
                        HttpEntity placesEntity = placesResponse.getEntity();
                        InputStream placesContent = placesEntity.getContent();
                        InputStreamReader placesInput = new InputStreamReader(placesContent);
                        BufferedReader placesReader = new BufferedReader(placesInput);
                        String lineIn;
                        while ((lineIn = placesReader.readLine()) != null) {
                            placesBuilder.append(lineIn);
                        }
                        placeResult = placesBuilder.toString();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
            return placeResult;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected void onPostExecute(String result) {
            //parse place data returned from Google Places
            //remove existing markers
            if (placeMarkers != null) {
                for (int pm = 0; pm < placeMarkers.length; pm++) {
                    if (placeMarkers[pm] != null)
                        placeMarkers[pm].remove();
                }
            }
            try {
                //parse JSON

                //create JSONObject, pass stinrg returned from doInBackground
                JSONObject resultObject = new JSONObject(result);
                //get "results" array
                JSONArray placesArray = resultObject.getJSONArray("results");
                //marker options for each place returned
                places = new MarkerOptions[placesArray.length()];
                //loop through places

                Log.d("test", "The placesArray length is " + placesArray.length() + "...............");

                for (int p = 0; p < placesArray.length(); p++) {
                    //parse each place
                    //if any values are missing we won't show the marker
                    boolean missingValue = false;
                    LatLng placeLL = null;
                    String placeName = "";
                    String vicinity = "";
                    int currIcon = personIcon;
                    try {
                        //attempt to retrieve place data values
                        missingValue = false;
                        //get place at this index
                        JSONObject placeObject = placesArray.getJSONObject(p);
                        //get location section
                        JSONObject loc = placeObject.getJSONObject("geometry")
                                .getJSONObject("location");
                        //read lat lng
                        placeLL = new LatLng(Double.valueOf(loc.getString("lat")),
                                Double.valueOf(loc.getString("lng")));
                        //get types
                        JSONArray types = placeObject.getJSONArray("types");
                        //loop through types
                        for (int t = 0; t < types.length(); t++) {
                            //what type is it
                            String thisType = types.get(t).toString();
                            //check for particular types - set icons
                            if(thisType.contains("food")){
                                currIcon = foodIcon;
                                break;
                            }
                            else if(thisType.contains("airport")){
                                currIcon = airportIcon;
                                break;
                            }
                            else if(thisType.contains("atm")){
                                currIcon = atmIcon;
                                break;
                            }
                            else if(thisType.contains("bus_station")){
                                currIcon = busIcon;
                                break;
                            }
                            else if(thisType.contains("hospital")){
                                currIcon = hospitalIcon;
                                break;
                            }
                            else if(thisType.contains("pharmacy")){
                                currIcon = pharmacyIcon;
                                break;
                            }
                            else if(thisType.contains("movie_theater")){
                                currIcon = moviesIcon;
                                break;
                            }
                            else if(thisType.contains("gas_station")){
                                currIcon = fuelIcon;
                                break;
                            }
                            else if(thisType.contains("train_station")){
                                currIcon = railIcon;
                                break;
                            }
                        }
                        //vicinity
                        vicinity = placeObject.getString("vicinity");
                        //name
                        placeName = placeObject.getString("name");
                    } catch (JSONException jse) {
                        Log.v("PLACES", "missing value");
                        missingValue = true;
                        jse.printStackTrace();
                    }
                    //if values missing we don't display
                    if (missingValue) places[p] = null;
                    else
                        places[p] = new MarkerOptions()
                                .position(placeLL)
                                .title(placeName)
                                .snippet(vicinity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (places != null && placeMarkers != null) {
                Log.d("test", "The placeMarkers length is " + placeMarkers.length + "...............");

                for (int p = 0; p < places.length && p < placeMarkers.length; p++) {
                    //will be null if a value was missing

                    if (places[p] != null) {

                        placeMarkers[p] = mMap.addMarker(places[p]);
                    }
                }
            }
        }
    }
}
