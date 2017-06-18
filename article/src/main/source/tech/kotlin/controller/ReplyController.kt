package tech.kotlin.controller

import spark.Route
import tech.kotlin.common.rpc.Serv
import tech.kotlin.common.utils.dict
import tech.kotlin.service.domain.Account
import tech.kotlin.service.domain.Reply
import tech.kotlin.service.domain.TextContent
import tech.kotlin.service.domain.UserInfo
import tech.kotlin.common.utils.ok
import tech.kotlin.service.ServDef
import tech.kotlin.service.account.SessionApi
import tech.kotlin.service.account.UserApi
import tech.kotlin.service.article.ReplyApi
import tech.kotlin.service.article.TextApi
import tech.kotlin.service.Err
import tech.kotlin.common.utils.abort
import tech.kotlin.common.utils.check
import tech.kotlin.service.account.req.CheckTokenReq
import tech.kotlin.service.account.req.QueryUserReq
import tech.kotlin.service.article.req.*

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object ReplyController {

    val sessionApi by Serv.bind(SessionApi::class, ServDef.ACCOUNT)
    val userApi by Serv.bind(UserApi::class, ServDef.ACCOUNT)

    val replyApi by Serv.bind(ReplyApi::class)
    val textApi by Serv.bind(TextApi::class)

    val createReply = Route { req, _ ->
        val articleId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong(); true }
                .toLong()

        val content = req.queryParams("content")
                .check(Err.PARAMETER, "评论内容为空") { !it.isNullOrBlank() && it.trim().length >= 10 }

        val aliasId = req.queryParams("alias_id")
                ?.check(Err.PARAMETER, "非法的关联id") { it.toLong();true }
                ?.toLong()
                ?: 0

        val owner = sessionApi.checkToken(CheckTokenReq(req)).account

        val createResp = replyApi.create(CreateArticleReplyReq().apply {
            this.articleId = articleId
            this.ownerUID = owner.id
            this.content = content
            this.aliasId = aliasId
        })

        return@Route ok { it["id"] = createResp.replyId }
    }

    val delReply = Route { req, _ ->
        val replyId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong();true }.toLong()

        val owner = sessionApi.checkToken(CheckTokenReq(req)).account
        val reply = replyApi.getReplyById(QueryReplyByIdReq().apply {
            this.id = arrayListOf(replyId)
        }).result[replyId] ?: abort(Err.REPLY_NOT_EXISTS)

        if (reply.ownerUID == owner.id) {
            replyApi.changeState(ChangeReplyStateReq().apply {
                this.replyId = replyId
                this.state = Reply.State.DELETE
            })
        } else {
            abort(Err.UNAUTHORIZED)
        }
        return@Route ok()
    }

    val queryReply = Route { req, _ ->
        val articleId = req.params(":id")
                .check(Err.PARAMETER) { it.toLong();true }.toLong()

        val offset = req.queryParams("offset")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val limit = req.queryParams("limit")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 20

        val reply = replyApi.getReplyByArticle(QueryReplyByArticleReq().apply {
            this.articleId = articleId
            this.offset = offset
            this.limit = limit
        }).result

        val users = HashMap<Long, UserInfo>()
        val contents = HashMap<Long, TextContent>()
        if (reply.isNotEmpty()) {
            users.putAll(userApi.queryById(QueryUserReq().apply {
                this.id = reply.map { it.ownerUID }.toList()
            }).info)
            contents.putAll(textApi.getById(QueryTextReq().apply {
                this.id = reply.map { it.contentId }.toList()
            }).result)
        }

        //只有管理员才能看到封禁和删除的文章内容
        var isUserAdmin = false
        try {
            val account = sessionApi.checkToken(CheckTokenReq(req)).account
            isUserAdmin = account.role == Account.Role.ADMIN
        } catch (ignore: Throwable) {
        }

        return@Route ok {
            it["reply"] = reply.map {
                dict {
                    this["meta"] = it
                    this["user"] = users[it.ownerUID] ?: UserInfo()
                    this["content"] =
                            if (isUserAdmin || it.state == Reply.State.NORMAL)
                                contents[it.contentId] ?: TextContent()
                            else
                                TextContent()
                }
            }
            it["next_offset"] = offset + reply.size
        }
    }

    val queryReplyCount = Route { req, _ ->
        val queryId = req.queryParams("id")
                ?.check(Err.PARAMETER) { it.split(',').map { it.toLong() };true }
                ?.split(',')
                ?.map { it.toLong() }
                ?: listOf(0L)

        val result = replyApi.getReplyCountByArticle(QueryReplyCountByArticleReq().apply {
            this.id = queryId
        })
        return@Route ok {
            it["data"] = result.result
        }
    }

}