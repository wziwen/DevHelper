package com.wziwen.devhelper.hosts;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by wen on 2016/9/25.
 */

public interface HostNetService {
    @GET
    Call<ResponseBody> downloadFile(@Url String fileUrl);
}
