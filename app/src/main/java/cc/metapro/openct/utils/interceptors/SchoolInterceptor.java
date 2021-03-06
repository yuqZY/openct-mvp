package cc.metapro.openct.utils.interceptors;

/*
 *  Copyright 2016 - 2017 OpenCT open source class table
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

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import cc.metapro.openct.data.openctservice.QuotePreservingCookieJar;
import cc.metapro.openct.data.university.UniversityService;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class SchoolInterceptor implements Interceptor {

//    private static final String urlPattern = "((http|ftp|https)://)(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?";
    public static final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
    private RedirectObserver<String> mObserver;
    private HttpUrl mUrl;

    public SchoolInterceptor(String URL) {
        mUrl = HttpUrl.parse(URL);
    }

    public void setObserver(RedirectObserver<String> observer) {
        mObserver = observer;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request newRequest = request.newBuilder()
                .header("User-Agent", userAgent)
                .build();

        Response response = chain.proceed(newRequest);

        if (response.isRedirect()) {
            String location = response.headers().get("Location");
            location = mUrl.newBuilder(location).toString();
            if (mObserver != null) {
                mObserver.onRedirect(location);
            }
        }
        return response;
    }

    public UniversityService createSchoolService() {
        return new Retrofit.Builder()
                .baseUrl("http://openct.metapro.cc/")
                .client(new OkHttpClient.Builder()
                        .addNetworkInterceptor(this)
                        .followRedirects(true)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .cookieJar(new QuotePreservingCookieJar(new CookieManager() {{
                            setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                        }})).build()
                )
                .addConverterFactory(ScalarsConverterFactory.create())
                .build().create(UniversityService.class);
    }

    public interface RedirectObserver<T> {

        void onRedirect(T x);

    }
}
