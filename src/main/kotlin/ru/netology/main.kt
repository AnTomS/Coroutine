package ru.netology

import Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.dto.Author
import ru.netology.dto.Comment
import ru.netology.dto.CommentWithAuthor
import ru.netology.dto.PostWithCommentWithAuthor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext


private const val BASE_URL = "http:///127.0.0.1:9999"
private val gson = Gson()
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    })
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()


suspend fun <T> makeCall(url: String, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        Request.Builder()
            .url(url)
            .build()
            .let(client::newCall)
            .execute()
            .let { response ->
                gson.fromJson(response.body?.string(), typeToken.type)
            }
    }

suspend fun getPost(): List<Post> =
    makeCall("$BASE_URL/api/slow/posts", object : TypeToken<List<Post>>() {})

suspend fun getComments(postId: Long): List<Comment> =
    makeCall("$BASE_URL/api/slow/posts/$postId/comments", object : TypeToken<List<Comment>>() {})

suspend fun getAuthor(id: Long): Author =
    makeCall("$BASE_URL/api/slow/authors$id", object : TypeToken<Author>() {})


fun main() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {
                val posts = getPost()
                    .map { post ->
                        async {
                            val author = getAuthor(post.authorId)
                            val comments = getComments(post.id)
                                .map { comment ->
                                    async {
                                        CommentWithAuthor(comment, getAuthor(comment.authorId))
                                    }
                                }.awaitAll()
                            PostWithCommentWithAuthor(
                                post, author, comments
                            )
                        }
                    }.awaitAll()
                println(posts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    Thread.sleep(30_000L)

}



