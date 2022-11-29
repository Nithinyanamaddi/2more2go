package com.example.blog.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:Key=AAAASLQCUBg:APA91bGIKvb4uaERCa8yFPcTI9-aUnVEuYKVPL2xaJA2l5oL70SYpAEAmT6eXJKFPcsQu1XCiE-bQxaLE9H6xzeVjcSkgtU10rhoGKFzHjuNJDZ1eJKaApvloRdoNs7vQbKLoyNY1dTN"
            }
    )


    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
