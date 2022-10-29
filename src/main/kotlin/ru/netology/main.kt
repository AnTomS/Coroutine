package ru.netology

import Post
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.dto.Comment
import ru.netology.dto.PostWithCommentWithAuthor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val BASE_URL = "http://127.0.0.1:9999"
private val gson = Gson()
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    })
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()


suspend fun <T> makePost(url: String, typeToken: TypeToken<T>): T =
    suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(client::newCall)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        continuation.resume(gson.fromJson(response.body?.string(), typeToken.type))
                    } catch (e: JsonParseException) {
                        continuation.resumeWithException(e)
                    }

                }

            }
            )

    }

suspend fun getPost(): List<Post> =
    makePost("$BASE_URL/api/slow/posts", object : TypeToken<List<Post>>(){})

suspend fun getComment(postId : Long): List<Comment> =
    makePost("$BASE_URL/api/slow/posts/$postId/comments/", object : TypeToken<List<Comment>>(){})

fun main() {
    runBlocking {
        val post = getPost()
        val result = post.map {
            PostWithCommentWithAuthor(it,getComment(it.id))
        }
        println(result)
    }
}





