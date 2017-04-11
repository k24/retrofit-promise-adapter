/*
 * Copyright 2017 k24
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.k24.retrofit2.converter.success;

import com.github.k24.retrofit2.adapter.promise.StringConverterFactory;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.http.POST;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by k24 on 2017/04/11.
 */
public class SuccessConverterFactoryTest {

    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    @Rule
    public final MockWebServer server = new MockWebServer();
    private final Converter.Factory factory = SuccessConverterFactory.create();
    private Retrofit retrofit;
    private Service service;

    @Before
    public void setUp() throws Exception {
        retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(factory)
                .addConverterFactory(new StringConverterFactory())
                .build();
        service = retrofit.create(Service.class);
    }

    @Test
    public void responseType() throws Exception {
        assertThat(factory.responseBodyConverter(Success.class, NO_ANNOTATIONS, retrofit).convert(responseBodyEmpty()))
                .isSameAs(Success.SUCCESS);
        assertThat(factory.responseBodyConverter(Void.class, NO_ANNOTATIONS, retrofit))
                .isNull();
    }

    @Test
    public void callThenResponse() throws Exception {
        server.enqueue(new MockResponse().setBody(""));
        assertThat(service.postAsSuccess().execute().body())
                .isSameAs(Success.SUCCESS);

        server.enqueue(new MockResponse().setBody(""));
        assertThat(service.postAsVoid().execute().body())
                .isNull();
    }

    private static ResponseBody responseBodyEmpty() {
        return ResponseBody.create(MediaType.parse("application/json"), "");
    }

    interface Service {
        @POST("/")
        Call<Success> postAsSuccess();

        @POST("/")
        Call<Void> postAsVoid();
    }
}