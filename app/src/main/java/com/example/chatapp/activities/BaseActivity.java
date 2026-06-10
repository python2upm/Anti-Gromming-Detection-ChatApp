package com.example.chatapp.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Toast;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;

    protected void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId != null) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
        }
    }

    protected void applyEdgeToEdge(View rootView) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (documentReference != null) {
            documentReference.update(Constants.KEY_AVAILABILITY, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (documentReference != null) {
            documentReference.update(Constants.KEY_AVAILABILITY, 1);
        }
    }


}
