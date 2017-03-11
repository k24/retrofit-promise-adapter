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
 * Created by k24 on 2017/02/16.
 */
public class PromiseCallAdapterFactory extends CallAdapter.Factory {
    private final Deferred.Factory deferredFactory;
    private final boolean isAsync;

    public static PromiseCallAdapterFactory create(Deferred.Factory deferredFactory) {
        return new PromiseCallAdapterFactory(deferredFactory, false);
    }

    public static PromiseCallAdapterFactory createAsync(Deferred.Factory deferredFactory) {
        return new PromiseCallAdapterFactory(deferredFactory, true);
    }

    private PromiseCallAdapterFactory(Deferred.Factory deferredFactory, boolean isAsync) {
        this.deferredFactory = deferredFactory;
        this.isAsync = isAsync;
    }

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
