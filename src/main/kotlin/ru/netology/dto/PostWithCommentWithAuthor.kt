package ru.netology.dto

import Post

data class PostWithCommentWithAuthor (
    val post: Post,
    val author: Author,
    val comments: List<CommentWithAuthor>,
        )