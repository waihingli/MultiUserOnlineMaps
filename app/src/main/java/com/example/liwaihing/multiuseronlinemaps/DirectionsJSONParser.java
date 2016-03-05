package com.example.liwaihing.multiuseronlinemaps;

import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by WaiHing on 23/2/2016.
 */
public class DirectionsJSONParser {
    private JSONObject disTimeOb;
    private JSONObject distanceOb;

    public List<List<HashMap<String,String>>> parse(JSONObject jObject){
        List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
        JSONArray routeArray, legArray, stepArray;

        try {
            routeArray = jObject.getJSONArray("routes");
            for(int i=0;i<routeArray.length();i++){
                legArray = ( (JSONObject)routeArray.get(i)).getJSONArray("legs");
                disTimeOb = legArray.getJSONObject(0);
                distanceOb = disTimeOb.getJSONObject("distance");
                List path = new ArrayList<>();
                for(int j=0;j<legArray.length();j++){
                    stepArray = ( (JSONObject)legArray.get(j)).getJSONArray("steps");
                    for(int k=0;k<stepArray.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)stepArray.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);
                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString((list.get(l)).latitude));
                            hm.put("lng", Double.toString((list.get(l)).longitude));
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
        }

        return routes;
    }

    public String getDistanceMeters() {
        String s = "";
        try {
            s = distanceOb.getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return s;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do{
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
