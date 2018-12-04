package de.polkagris.sffoderscan;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class Requests {
    private final static int timeoutMs = 20000;
    public static void sendSaveRequest(Context context, String name, String barcode, Response.Listener<JSONObject> onResult, Response.ErrorListener onError) {
        RequestQueue queue = Volley.newRequestQueue(context);
        JSONObject object = new JSONObject();
        JSONArray history = new JSONArray();
        JSONObject historyObject = new JSONObject();
        try {
            object.put("name", name);
            object.put("barcode", barcode);
            object.put("inventory", 0);
            long now = System.currentTimeMillis();
            historyObject.put("date", now);
            historyObject.put("change", 0);
            history.put(historyObject);
            object.put("history", history);
        } catch(Exception e) {
            //Log.d(tag, "Error when creating JSON object" + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Urls.POST_SAVE_NEW, object, onResult, onError);
        request.setRetryPolicy(new DefaultRetryPolicy(timeoutMs, 1, 1));
        queue.add(request);
    }

    public static void sendSearchRequest(Context context, String barcode, Response.Listener<JSONObject> onResult, Response.ErrorListener onError) {
        RequestQueue queue = Volley.newRequestQueue(context);
        Log.d("MYTAG", "scanContent: " + barcode);
        String url = Urls.GET_FIND_BARCODE + barcode;
        Log.d("MYTAG","url: " + url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, Urls.GET_FIND_BARCODE + barcode, new JSONObject(), onResult, onError);
        request.setRetryPolicy(new DefaultRetryPolicy(timeoutMs, 1, 1));
        queue.add(request);
    }

    public static void sendChangeInventoryRequest(Context context, int change, String barcode, Response.Listener<JSONObject> onResult, Response.ErrorListener onError) {
        RequestQueue queue = Volley.newRequestQueue(context);
        JSONObject object = new JSONObject();
        try {
            object.put("barcode", barcode);
            object.put("change", change);
        } catch (Exception e) {
            Log.d("MYTAG", "Error when creating JSON object: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Urls.POST_CHANGE_INVENTORY, object, onResult, onError);
        request.setRetryPolicy(new DefaultRetryPolicy(timeoutMs, 1, 1));
        queue.add(request);
    }

}