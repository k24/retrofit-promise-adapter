package com.github.k24.retrofit2.adapter.promise;

import com.github.k24.deferred.Deferred;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.HttpException;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by k24 on 2017/02/16.
 */
public class PromiseCallAdapterFactory extends CallAdapter.Factory {
    private final Deferred.Factory deferredFactory;

    public static PromiseCallAdapterFactory create(Deferred.Factory deferredFactory) {
        return new PromiseCallAdapterFactory(deferredFactory);
    }

    private PromiseCallAdapterFactory(Deferred.Factory deferredFactory) {
        this.deferredFactory = deferredFactory;
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
            return new BodyCallAdapter(innerType, deferredFactory);
        }

        if (!(innerType instanceof ParameterizedType)) {
            throw new IllegalStateException("Response must be parameterized"
                    + " as Response<Foo> or Response<? extends Foo>");
        }
        Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
        return new ResponseCallAdapter(responseType, deferredFactory);
    }

    private static class BodyCallAdapter<R> implements CallAdapter<R, Deferred.Promise<R>> {
        private final Type responseType;
        private final Deferred.Factory deferredFactory;

        private BodyCallAdapter(Type responseType, Deferred.Factory deferredFactory) {
            this.responseType = responseType;
            this.deferredFactory = deferredFactory;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Deferred.Promise<R> adapt(final Call<R> call) {
            final Deferred deferred = deferredFactory.deferred();
            return deferred.promise(new Deferred.PromiseCallback<R>() {
                @Override
                public void call(final Deferred.Result<R> result) {
                    call.enqueue(new Callback<R>() {
                        @Override
                        public void onResponse(Call<R> call, Response<R> response) {
                            if (response.isSuccessful()) {
                                result.resolve(response.body());
                            } else {
                                result.reject(new HttpException(response));
                            }
                        }

                        @Override
                        public void onFailure(Call<R> call, Throwable throwable) {
                            result.reject(throwable);
                        }
                    });
                }
            });
        }
    }

    private static class ResponseCallAdapter<R> implements CallAdapter<R, Deferred.Promise<Response<R>>> {
        private final Type responseType;
        private final Deferred.Factory deferredFactory;

        private ResponseCallAdapter(Type responseType, Deferred.Factory deferredFactory) {
            this.responseType = responseType;
            this.deferredFactory = deferredFactory;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Deferred.Promise<Response<R>> adapt(final Call<R> call) {
            final Deferred deferred = deferredFactory.deferred();
            return deferred.promise(new Deferred.PromiseCallback<Response<R>>() {
                @Override
                public void call(final Deferred.Result<Response<R>> result) {
                    call.enqueue(new Callback<R>() {
                        @Override
                        public void onResponse(Call<R> call, Response<R> response) {
                            result.resolve(response);
                        }

                        @Override
                        public void onFailure(Call<R> call, Throwable throwable) {
                            result.reject(throwable);
                        }
                    });
                }
            });
        }
    }
}
