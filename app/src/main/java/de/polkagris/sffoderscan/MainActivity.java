package de.polkagris.sffoderscan;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private Button scanBtn, searchManuallyBtn;
    private TextView contentTxt, dbResultTxt;
    private String scanContent, dbResult;
    private String tag = "MYTAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = findViewById(R.id.scan_button);
        searchManuallyBtn = findViewById(R.id.search_manually_button);
        contentTxt = findViewById(R.id.scan_content);
        dbResultTxt = findViewById(R.id.database_result);
        if(scanContent != null) {
            contentTxt.setText(scanContent);
        } else {
            Log.d(tag, "scanContent är null");
        }
        if(dbResult != null) {
            dbResultTxt.setText(dbResult);
        } else {
            Log.d(tag, "dbResult är null");
        }
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
        //super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(scanningResult != null) {
            // We have a result
            String scanContent = scanningResult.getContents();
            this.scanContent = scanContent;
            contentTxt.setText("Scannad kod: " + scanContent);

            sendSearchRequest(scanContent);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Streckkod hittades ej i databas");
        builder.setMessage("Streckkod " + scanContent + " hittades ej. Kontrollera att koden scannats korrekt. Skriv in namnet på varan för att spara i databas");

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
        builder.setMessage("Skriv in siffrorna i streckkoden, utan mellanslag eller andra tecken, för att söka efter ett foder");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(params);
        builder.setView(input);

        builder.setPositiveButton("Sök", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendSearchRequest(input.getText().toString());
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
        Requests.sendSearchRequest(this, barcode, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("null")) {
                    showSaveDialog();
                } else {
                    dbResultTxt.setText(response);
                    addInventoryButtons();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    private void addInventoryButtons() {
        Button addToInventoryBtn = new Button(this);
        Button removeFromInventoryBtn = new Button(this);
        addToInventoryBtn.setText("Lägg till i lager");
        removeFromInventoryBtn.setText("Ta från lager");
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        addToInventoryBtn.setLayoutParams(params);
        removeFromInventoryBtn.setLayoutParams(params);

        LinearLayout linearLayout = findViewById(R.id.main_linear_layout);
        linearLayout.addView(addToInventoryBtn);
        linearLayout.addView(removeFromInventoryBtn);
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
                            int newInventory = response.getInt("newInventory");
                            Toast.makeText(MainActivity.this, "Ny lagerstatus: " + newInventory, Toast.LENGTH_LONG).show();
                        } catch(Exception e) {
                            Log.d(tag, "Error when parsing JSON object: " + e);
                        }
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

}
