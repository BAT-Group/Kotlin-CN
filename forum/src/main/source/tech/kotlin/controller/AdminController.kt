package tech.kotlin.controller

import spark.Route
import tech.kotlin.model.domain.*
import tech.kotlin.model.request.*
import tech.kotlin.service.account.AccountService
import tech.kotlin.service.account.TokenService
import tech.kotlin.service.account.UserService
import tech.kotlin.service.article.ArticleService
import tech.kotlin.service.article.ReplyService
import tech.kotlin.service.article.TextService
import tech.kotlin.utils.exceptions.Err
import tech.kotlin.utils.exceptions.check

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object AdminController {

    val userState = Route { req, _ ->
        val userId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong();true }.toLong()

        val state = req.queryParams("state")
                .check(Err.PARAMETER) { s ->
                    arrayOf(Account.State.NORMAL, Account.State.BAN).any { it == s.toInt() }
                }.toInt()

        val owner = TokenService.checkToken(CheckTokenReq(req)).account
        owner.check(Err.UNAUTHORIZED) { it.role == Account.Role.ADMIN }

        AccountService.changeUserState(ChangeUserStateReq().apply {
            this.uid = userId
            this.state = state
        })

        return@Route ok()
    }

    val articleState = Route { req, _ ->
        val articleId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong();true }.toLong()

        val state = req.queryParams("state")
                .check(Err.PARAMETER) { s ->
                    arrayOf(Article.State.NORMAL, Article.State.BAN,
                            Article.State.DELETE, Article.State.FINE
                    ).any { it == s.toInt() }
                }

        val owner = TokenService.checkToken(CheckTokenReq(req)).account
        owner.check(Err.UNAUTHORIZED) { it.role == Account.Role.ADMIN }

        ArticleService.updateMeta(UpdateArticleReq().apply {
            this.id = articleId
            this.args = hashMapOf("state" to state)
        })

        return@Route ok()
    }

    val replyState = Route { req, _ ->
        val replyId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong();true }.toLong()

        val state = req.queryParams("state").check(Err.PARAMETER) { s ->
            arrayOf(Reply.State.NORMAL, Reply.State.BAN, Reply.State.DELETE).any { it == s.toInt() }
        }.toInt()

        val owner = TokenService.checkToken(CheckTokenReq(req)).account
        owner.check(Err.UNAUTHORIZED) { it.role == Account.Role.ADMIN }

        ReplyService.changeState(ChangeReplyStateReq().apply {
            this.replyId = replyId
            this.state = state
        })

        return@Route ok()
    }

    val getArticleList = Route { req, _ ->
        val offset = req.queryParams("offset")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val limit = req.queryParams("limit")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 20

        val owner = TokenService.checkToken(CheckTokenReq(req)).account
        owner.check(Err.UNAUTHORIZED) { it.role == Account.Role.ADMIN }

        val articles = ArticleService.getLatest(QueryLatestArticleReq().apply {
            this.offset = offset
            this.limit = limit
            this.state = "${Article.State.FINE},${Article.State.NORMAL},${Article.State.BAN},${Article.State.DELETE}"
        }).result

        val users = HashMap<Long, UserInfo>()
        if (articles.isNotEmpty()) {
            users.putAll(UserService.queryById(QueryUserReq().apply {
                this.id = ArrayList<Long>().apply {
                    addAll(articles.map { it.author })
                    addAll(articles.map { it.lastEditUID })
                }.distinctBy { it }
            }).info)
        }

        return@Route ok {
            it["articles"] = articles.map {
                hashMapOf(
                        "meta" to it,
                        "author" to (users[it.author] ?: UserInfo()),
                        "last_editor" to (users[it.lastEditUID] ?: UserInfo())
                )
            }
            it["next_offset"] = offset + articles.size
        }
    }
}