package com.example.chatapp.activities;

import android.os.Bundle;

import com.example.chatapp.databinding.ActivityReportBinding;
import com.example.chatapp.utilities.Constants;

public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadData();
    }

    private void loadData() {
        String reason = getIntent().getStringExtra(Constants.KEY_RISK_LEVEL);
        String message = getIntent().getStringExtra(Constants.KEY_MESSAGE);
        binding.textReason.setText(reason);
        binding.textMessage.setText(message);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonSubmitReport.setOnClickListener(v -> {
            showToast("Report submitted successfully. Thank you for keeping the community safe.");
            finish();
        });
        binding.textSafetyResources.setOnClickListener(v -> {
            // TODO: Navigate to SafetyHub or external URL
            showToast("Opening Safety Resources...");
        });
    }
}
