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

package com.github.k24.retrofit2.adapter.promise;

import com.github.k24.deferred.Deferred;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * CallAdapterFactory with Promise.
 * <p>
 * Created by k24 on 2017/02/16.
 */
@SuppressWarnings("WeakerAccess")
public class PromiseCallAdapterFactory extends CallAdapter.Factory {
    private final Deferred.Factory deferredFactory;
    private final boolean isAsync;

    /**
     * Create sync factory.
     *
     * @param deferredFactory for Promise.
     * @return Factory for calling with sync.
     */
    public static PromiseCallAdapterFactory create(Deferred.Factory deferredFactory) {
        return new PromiseCallAdapterFactory(deferredFactory, false);
    }

    /**
     * Create async factory.
     *
     * @param deferredFactory for Promise.
     * @return Factory for calling with async.
     */
    public static PromiseCallAdapterFactory createAsync(Deferred.Factory deferredFactory) {
        return new PromiseCallAdapterFactory(deferredFactory, true);
    }

    private PromiseCallAdapterFactory(Deferred.Factory deferredFactory, boolean isAsync) {
        this.deferredFactory = deferredFactory;
        this.isAsync = isAsync;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Deferred.Promise.class) return null;

        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException("Promise return type must be parameterized"
                    + " as Promise<Foo> or Promise<? extends Foo>");
        }
        Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

        if (getRawType(innerType) != Response.class) {
            return new BodyCallAdapter(innerType, deferredFactory, isAsync);
        }

        if (!(innerType instanceof ParameterizedType)) {
            throw new IllegalStateException("Response must be parameterized"
                    + " as Response<Foo> or Response<? extends Foo>");
        }
        Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
        return new ResponseCallAdapter(responseType, deferredFactory, isAsync);
    }

    private static class BodyCallAdapter<R> implements CallAdapter<R, Deferred.Promise<R>> {
        private final Type responseType;
        private final Deferred.Factory deferredFactory;
        private final boolean isAsync;

        private BodyCallAdapter(Type responseType, Deferred.Factory deferredFactory, boolean isAsync) {
            this.responseType = responseType;
            this.deferredFactory = deferredFactory;
            this.isAsync = isAsync;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Deferred.Promise<R> adapt(Call<R> call) {
            final Deferred deferred = deferredFactory.deferred();
            final Call<R> clonedCall = call.clone();
            if (isAsync) {
                final Deferred.DeferredPromise<R> promise = deferred.promise();
                clonedCall.enqueue(new Callback<R>() {
                    @Override
                    public void onResponse(Call<R> call, Response<R> response) {
                        promise.resolve(response.body());
                    }

                    @Override
                    public void onFailure(Call<R> call, Throwable throwable) {
                        promise.reject(throwable);
                    }
                });
                return promise;
            } else {
                try {
                    Response<R> response = clonedCall.execute();
                    return deferred.resolved(response.body());
                } catch (Exception e) {
                    return deferred.rejected(e);
                }
            }
        }
    }

    private static class ResponseCallAdapter<R> implements CallAdapter<R, Deferred.Promise<Response<R>>> {
        private final Type responseType;
        private final Deferred.Factory deferredFactory;
        private final boolean isAsync;

        private ResponseCallAdapter(Type responseType, Deferred.Factory deferredFactory, boolean isAsync) {
            this.responseType = responseType;
            this.deferredFactory = deferredFactory;
            this.isAsync = isAsync;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Deferred.Promise<Response<R>> adapt(Call<R> call) {
            final Deferred deferred = deferredFactory.deferred();
            final Call<R> clonedCall = call.clone();
            if (isAsync) {
                final Deferred.DeferredPromise<Response<R>> promise = deferred.promise();
                clonedCall.enqueue(new Callback<R>() {
                    @Override
                    public void onResponse(Call<R> call, Response<R> response) {
                        promise.resolve(response);
                    }

                    @Override
                    public void onFailure(Call<R> call, Throwable throwable) {
                        promise.reject(throwable);

                    }
                });
                return promise;
            } else {
                try {
                    Response<R> response = clonedCall.execute();
                    return deferred.resolved(response);
                } catch (Exception e) {
                    return deferred.rejected(e);
                }
            }
        }
    }
}
