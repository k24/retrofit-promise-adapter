package com.github.k24.retrofit2.adapter.promise;

import com.github.k24.deferred.Deferred;
import com.github.k24.deferred.RxJava2DeferredFactory;
import io.reactivex.schedulers.Schedulers;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by k24 on 2017/03/02.
 */
public class PromiseCallAdapterFactoryTest {
    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    @Rule
    public final MockWebServer server = new MockWebServer();
    private final CallAdapter.Factory factory = PromiseCallAdapterFactory.create(new RxJava2DeferredFactory(Schedulers.io()));
    private Retrofit retrofit;

    @Before
    public void setUp() throws Exception {
        retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(new StringConverterFactory())
                .addCallAdapterFactory(factory)
                .build();
    }

    @Test
    public void responseType() throws Exception {
        assertThat(factory.get(BodyClasses.getTypeByName("string"), NO_ANNOTATIONS, retrofit).responseType())
                .isEqualTo(String.class);
        assertThat(factory.get(BodyClasses.getTypeByName("wildcard"), NO_ANNOTATIONS, retrofit).responseType())
                .isEqualTo(String.class);
        assertThat(factory.get(BodyClasses.getTypeByName("generic"), NO_ANNOTATIONS, retrofit).responseType())
                .isEqualTo(BodyClasses.getTypeByName("contentOfGeneric"));
        assertThat(factory.get(BodyClasses.getTypeByName("response"), NO_ANNOTATIONS, retrofit).responseType())
                .isEqualTo(String.class);
        assertThat(factory.get(BodyClasses.getTypeByName("responseWildcard"), NO_ANNOTATIONS, retrofit).responseType())
                .isEqualTo(String.class);
    }

    @Test
    public void nonPromiseTypeReturnsNull() throws Exception {
        assertThat(factory.get(String.class, NO_ANNOTATIONS, retrofit))
                .isNull();
    }

    @Test
    public void rawTypeThrows() throws Exception {
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                factory.get(BodyClasses.getTypeByName("rawType"), NO_ANNOTATIONS, retrofit);
            }
        }).hasMessage("Promise return type must be parameterized as Promise<Foo> or Promise<? extends Foo>");
    }

    @Test
    public void rawResponseTypeThrows() throws Exception {
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                factory.get(BodyClasses.getTypeByName("rawResponseType"), NO_ANNOTATIONS, retrofit);
            }
        }).hasMessage("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }

    private static class BodyClasses {
        public Deferred.Promise<String> string;
        public Deferred.Promise<? extends String> wildcard;
        public Deferred.Promise<List<String>> generic;
        public Deferred.Promise<Response<String>> response;
        public Deferred.Promise<Response<? extends String>> responseWildcard;

        public Deferred.Promise rawType;
        public Deferred.Promise<Response> rawResponseType;

        public List<String> contentOfGeneric;

        public static Type getTypeByName(String name) throws NoSuchFieldException {
            return BodyClasses.class.getField(name).getGenericType();
        }
    }
}