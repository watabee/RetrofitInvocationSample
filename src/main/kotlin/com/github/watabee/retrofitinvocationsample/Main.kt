package com.github.watabee.retrofitinvocationsample

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// TODO: Set your username and Github access token.
private const val username = "watabee"
private const val githubAccessToken = ""

val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor)
    .build()

val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://api.github.com")
    .client(okHttpClient)
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()

val githubService: GithubService = retrofit.create(GithubService::class.java)

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositories = githubService.getRepositories(username).blockingGet()
        val authenticatedRepositories = githubService.getAuthenticatedRepositories().blockingGet()

        println("repositories.size = ${repositories.size}")
        println("authenticatedRepositories.size = ${authenticatedRepositories.size}")
    }
}

object AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val accessToken = invocation?.method()?.getAnnotation(AccessToken::class.java) ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .addHeader("Authorization", "Bearer ${accessToken.token}")
                .build()
        )
    }
}

interface GithubService {

    @GET("/users/{username}/repos")
    fun getRepositories(@Path("username") username: String): Single<List<Repository>>

    @AccessToken(githubAccessToken)
    @GET("/user/repos")
    fun getAuthenticatedRepositories(): Single<List<Repository>>
}

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AccessToken(val token: String)

data class Repository(
    val fullName: String,
    @SerializedName("private") val isPrivate: Boolean
)