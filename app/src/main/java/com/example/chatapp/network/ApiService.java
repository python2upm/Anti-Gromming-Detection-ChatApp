package com.example.chatapp.network;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("v1/projects/{projectId}/messages:send")
    Call<String> sendMessage(
            @Path("projectId") String projectId,
            @HeaderMap HashMap<String, String> headers,
            @Body String messageBody
            );
}
