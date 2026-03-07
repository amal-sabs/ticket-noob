package com.example.ticketnoob.util;

import android.app.Activity;
import android.content.Intent;

import com.example.ticketnoob.R;
import com.example.ticketnoob.ui.activities.EventListActivity;
import com.example.ticketnoob.ui.activities.MyReservationsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavHelper {

    public static void setup(Activity activity, BottomNavigationView bottomNav, int currentItemId, String userId) {
        bottomNav.setSelectedItemId(currentItemId);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) return false;

            Intent intent;
            if (id == R.id.nav_events) {
                intent = new Intent(activity, EventListActivity.class);
            } else {
                intent = new Intent(activity, MyReservationsActivity.class);
            }
            intent.putExtra("userId", userId);
            // Avoid stacking activities
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            return true;
        });
    }
}
