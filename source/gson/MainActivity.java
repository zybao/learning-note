package com.oab.gsondemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv)
    TextView tv;
    @BindView(R.id.tv2)
    TextView tv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        try {
            String s = "{ \"age\" : 6, \"name\" : null }";
            User user = new User();
            user.name = null;
            Log.d("bzy", new Gson().toJson(user));
            User u = GsonUtils.getInstance().fromJson(s, User.class);

            Log.d("bzy", "out: " + u.name);
        } catch (Exception e) {
            Log.d("bzy", "e: " + e.getMessage());
        }
    }
}
