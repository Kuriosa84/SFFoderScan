package de.polkagris.sffoderscan;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.Html;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private Button scanBtn, searchManuallyBtn, addToInventoryBtn, removeFromInventoryBtn;
    private TextView contentTxt, dbResultTxt, inventoryTxt;
    private LinearLayout registerButtons;
    private String scanContent;
    private String tag = "MYTAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerButtons = findViewById(R.id.register_buttons);
        scanBtn = findViewById(R.id.scan_button);
        searchManuallyBtn = findViewById(R.id.search_manually_button);
        contentTxt = findViewById(R.id.scan_content);
        dbResultTxt = findViewById(R.id.database_result);
        inventoryTxt = findViewById(R.id.inventory);
        addToInventoryBtn = findViewById(R.id.add_to_inventory);
        removeFromInventoryBtn = findViewById(R.id.remove_from_inventory);
        scanBtn.setOnClickListener(this);
        searchManuallyBtn.setOnClickListener(this);
        Log.d(tag, "Är i onCreate!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(tag, "Är i onResume!");
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.scan_button) {
            // Scan
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            scanIntegrator.initiateScan();
        } else if(view.getId() == R.id.search_manually_button) {
            showSearchManuallyDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(scanningResult != null) {
            // We have a result
            String scanContent = scanningResult.getContents();
            this.scanContent = scanContent;
            contentTxt.setText("Scannad kod: " + scanContent);

            sendSearchRequest(scanContent);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Ingen data från scanning", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Streckkod hittades ej i databas");
        builder.setMessage("Streckkod " + scanContent + " hittades ej. Kontrollera att koden scannats/skrivits korrekt. Skriv in namnet på varan för att spara det i databasen:");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(params);
        builder.setView(input);

        builder.setPositiveButton("Spara", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Requests.sendSaveRequest(MainActivity.this, input.getText().toString(), scanContent, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dbResultTxt.setText("Fodret sparat. Scanna koden igen för att ange lagerstatus.");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
            }
        });
        builder.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });
        builder.create();
        builder.show();
    }

    private void showSearchManuallyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Sök manuellt");
        builder.setMessage("Skriv in siffrorna i streckkoden, utan mellanslag eller andra tecken, för att söka efter ett foder:");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(params);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Sök", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scanContent = input.getText().toString();
                sendSearchRequest(scanContent);
            }
        });
        builder.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });
        builder.create();
        builder.show();
    }

    private void sendSearchRequest(String barcode) {
        dbResultTxt.setText("Söker...");
        Requests.sendSearchRequest(this, barcode, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(tag,"Fick svar från databasen");
                Log.d(tag, response.toString());
                String name = "Namn";
                try {
                    name = response.getString("name");
                } catch (Exception e) {
                    Log.d(tag, "Fel vid hämtning av name: " + e);
                }
                if(name.equals("null")) {
                    showSaveDialog();
                } else {
                    showInventoryData(response);
                    showInventoryButtons();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(tag, "Error: " + error);
            }
        });
    }

    private void showInventoryButtons() {
        addToInventoryBtn.setVisibility(View.VISIBLE);
        removeFromInventoryBtn.setVisibility(View.VISIBLE);

        addToInventoryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeInventoryDialog(true);
            }
        });
        removeFromInventoryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeInventoryDialog(false);
            }
        });
    }

    private void showChangeInventoryDialog(final boolean isAdding) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(isAdding) {
            builder.setTitle("Lägg till förpackningar i lagret");
            builder.setMessage("Ange antal förpackningar du lägger till i lagret:");
        } else {
            builder.setTitle("Ta förpackningar från lagret");
            builder.setMessage("Ange antal förpackningar du tar från lagret:");
        }

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(params);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int change = Integer.parseInt(input.getText().toString());
                if(!isAdding) {
                    change *= -1;
                }
                Log.d(tag, "change: " + change);
                Requests.sendChangeInventoryRequest(MainActivity.this, change, scanContent, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            showInventoryData(response);
                            Toast.makeText(MainActivity.this, "Lagerstatus uppdaterad", Toast.LENGTH_LONG).show();
                        } catch(Exception e) {
                            Log.d(tag, "Error when parsing JSON object: " + e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(tag, "Error when sending request: " + error);
                    }
                });
            }
        });
        builder.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });
        builder.create();
        builder.show();
    }

    private static String convertDate(long dateInMilliseconds) {
        String dateFormat = "EEE dd MMM yyyy hh:mm";
        return DateFormat.format(dateFormat, dateInMilliseconds).toString();
    }

    private void showInventoryData(JSONObject response) {
        String name = "Namn";
        //String barcode = "01234567";
        int inventory = 0;
        String history = "";
        try {
            name = response.getString("name");
            //barcode = response.getString("barcode");
            inventory = response.getInt("inventory");
            JSONArray historyJson = response.getJSONArray("history");
            for(int i=historyJson.length()-1; i>=0; i--) {
                int change = historyJson.getJSONObject(i).getInt("change");
                String changeSigned = change > 0 ? "+" + change : "" + change;
                history += convertDate(historyJson.getJSONObject(i).getLong("date")) + "   " + changeSigned + "\n";
            }
        } catch (Exception e) {
            Log.d(tag, "Fel vid parsning av JSON-objekt.");
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        registerButtons.setLayoutParams(params);

        dbResultTxt.setVisibility(View.VISIBLE);
        inventoryTxt.setVisibility(View.VISIBLE);
        String htmlStr = "<b>" + name + "</b><br>" + inventory + " i lager";
        dbResultTxt.setText(Html.fromHtml(htmlStr));
        inventoryTxt.setText("Lagerhistorik:\n\n" + history);
    }
}
