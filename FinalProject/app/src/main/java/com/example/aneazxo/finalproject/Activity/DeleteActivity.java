package com.example.aneazxo.finalproject.Activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.R;

import java.util.ArrayList;

public class DeleteActivity extends AppCompatActivity {

    private final int DELETE = 3;
    private ListView list;
    private Button toMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        list =  (ListView) findViewById(R.id.pointList);
        toMain = (Button) findViewById(R.id.backBtn);

        DataModel model = new DataModel(DeleteActivity.this);
        ArrayList<String> valuesList = model.selectAllTarget();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(DeleteActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, valuesList);

        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Delete Method Here
                // ListView Clicked item value
                String  itemValue    = (String) list.getItemAtPosition(position);

                Intent intent = new Intent(DeleteActivity.this, ConfirmActivity.class);
                intent.putExtra("des", itemValue);
                intent.putExtra("from", DELETE);
                startActivity(intent);
                finish();
            }

        });

        toMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DeleteActivity.this, MainActivity.class);
                intent.putExtra("finished", "");
                intent.putExtra("from", DeleteActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DeleteActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", DeleteActivity.class);
        startActivity(intent);
        finish();
    }
}
