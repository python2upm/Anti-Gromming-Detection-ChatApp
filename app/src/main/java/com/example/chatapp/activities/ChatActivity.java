package com.example.chatapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.network.ApiClient;
import com.example.chatapp.network.ApiService;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.GroomingDetector;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.pipeline.Expression;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();

    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();

    }
    private Bitmap getBitmapFromEncodedString(String encodedImage){
        if(encodedImage != null){
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }else{
            return null;
        }

    }

    private void sendMessage() {
        String messageText = binding.inputMessage.getText().toString();
        GroomingDetector.DetectionResult result = GroomingDetector.analyze(messageText);

        if (result.riskLevel == GroomingDetector.RiskLevel.HIGH) {
            showHighRiskAlert(result);
        } else if (result.riskLevel == GroomingDetector.RiskLevel.MEDIUM) {
            showMediumRiskWarning(result);
        } else {
            performSendMessage(messageText, false, 0, null);
        }
    }

    private void showMediumRiskWarning(GroomingDetector.DetectionResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Safety Warning")
                .setMessage("This message contains language that could be associated with grooming behavior. Are you sure you want to send it?\n\nDetected: " + result.reason)
                .setPositiveButton("Send Anyway", (dialog, which) -> performSendMessage(binding.inputMessage.getText().toString(), true, result.score, result.riskLevel.name()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHighRiskAlert(GroomingDetector.DetectionResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Security Alert")
                .setMessage("This message has been blocked because it contains high-risk grooming patterns. This incident will be logged for your safety.")
                .setPositiveButton("Report & Get Help", (dialog, which) -> {
                    logHighRiskIncident(result);
                    android.content.Intent intent = new android.content.Intent(getApplicationContext(), ReportActivity.class);
                    intent.putExtra(Constants.KEY_RISK_LEVEL, result.reason);
                    intent.putExtra(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                    startActivity(intent);
                })
                .setNegativeButton("Close", (dialog, which) -> logHighRiskIncident(result))
                .show();
    }

    private void logHighRiskIncident(GroomingDetector.DetectionResult result) {
        HashMap<String, Object> incident = new HashMap<>();
        incident.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        incident.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        incident.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        incident.put(Constants.KEY_TIMESTAMP, new Date());
        incident.put(Constants.KEY_IS_FLAGGED, true);
        incident.put(Constants.KEY_RISK_SCORE, result.score);
        incident.put(Constants.KEY_RISK_LEVEL, result.riskLevel.name());
        database.collection("flagged_incidents").add(incident);
        binding.inputMessage.setText(null);
    }

    private void performSendMessage(String messageText, boolean isFlagged, int riskScore, String riskLevel) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, messageText);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.KEY_IS_FLAGGED, isFlagged);
        if (isFlagged) {
            message.put(Constants.KEY_RISK_SCORE, riskScore);
            message.put(Constants.KEY_RISK_LEVEL, riskLevel);
        }

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversionId != null) {
            updateConversion(messageText);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, messageText);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, messageText);

                JSONObject notificationMessage = new JSONObject();
                notificationMessage.put(Constants.REMOTE_MSG_DATA, data);
                notificationMessage.put(Constants.REMOTE_MSG_TOKEN, receiverUser.token);

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_MESSAGE, notificationMessage);

                sendNotification(body.toString());
            } catch (Exception exception) {
                showToast(exception.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

// -----------------------------------------------------------------------
// Replace the existing sendNotification() method in ChatActivity.java
// with this version. Everything else in the file stays the same.
// -----------------------------------------------------------------------

    private void sendNotification(String messageBody) {
        Constants.getRemoteMsgHeaders(getApplicationContext(), new Constants.HeadersCallback() {
            @Override
            public void onHeaders(HashMap<String, String> headers) {
                // getRemoteMsgHeaders runs on a background thread, so hop back
                // to the main thread before touching Retrofit / UI.
                runOnUiThread(() ->
                        ApiClient.getClient().create(ApiService.class)
                                .sendMessage(headers, messageBody)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(@NonNull Call<String> call,
                                                           @NonNull Response<String> response) {
                                        if (response.isSuccessful()) {
                                            try {
                                                if (response.body() != null) {
                                                    JSONObject responseJson = new JSONObject(response.body());
                                                    if (responseJson.has("name")) {
                                                        showToast("Notification sent successfully");
                                                    } else if (responseJson.has("error")) {
                                                        showToast("Error: " + responseJson
                                                                .getJSONObject("error")
                                                                .getString("message"));
                                                    }
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            showToast("Error " + response.code());
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<String> call,
                                                          @NonNull Throwable t) {
                                        showToast(t.getMessage());
                                    }
                                })
                );
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> showToast("Token error: " + e.getMessage()));
            }
        });
    }
    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this,(value, error) -> {
            if(error != null){
                return;
            }
            if(value != null){
                if(value.getLong(Constants.KEY_AVAILABILITY)!=null){
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if(receiverUser.image == null){
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                }
            }
            if(isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else{
                binding.textAvailability.setVisibility(View.GONE);
            }

        });

    }

    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }

            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId == null){
            checkForConversion();
        }
    };

    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){

        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void updateConversion(String message){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void addConversion(HashMap<String, Object> conversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}