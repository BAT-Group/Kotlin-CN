package tech.kotlin.controller

import spark.Route
import tech.kotlin.model.domain.Account
import tech.kotlin.model.domain.Reply
import tech.kotlin.model.domain.TextContent
import tech.kotlin.model.domain.UserInfo
import tech.kotlin.model.request.*
import tech.kotlin.service.account.TokenService
import tech.kotlin.service.account.UserService
import tech.kotlin.service.article.ReplyService
import tech.kotlin.service.article.TextService
import tech.kotlin.utils.exceptions.Err
import tech.kotlin.utils.exceptions.abort
import tech.kotlin.utils.exceptions.check
import tech.kotlin.utils.serialize.dict

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object ReplyController {

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

        val owner = TokenService.checkToken(CheckTokenReq(req)).account

        val createResp = ReplyService.create(CreateArticleReplyReq().apply {
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

        val owner = TokenService.checkToken(CheckTokenReq(req)).account
        val reply = ReplyService.getReplyById(QueryReplyByIdReq().apply {
            this.id = arrayListOf(replyId)
        }).result[replyId] ?: abort(Err.REPLY_NOT_EXISTS)

        if (reply.ownerUID == owner.id) {
            ReplyService.changeState(ChangeReplyStateReq().apply {
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

        val reply = ReplyService.getReplyByArticle(QueryReplyByArticleReq().apply {
            this.articleId = articleId
            this.offset = offset
            this.limit = limit
        }).result

        val users = HashMap<Long, UserInfo>()
        val contents = HashMap<Long, TextContent>()
        if (reply.isNotEmpty()) {
            users.putAll(UserService.queryById(QueryUserReq().apply {
                this.id = reply.map { it.ownerUID }.toList()
            }).info)
            contents.putAll(TextService.getById(QueryTextReq().apply {
                this.id = reply.map { it.contentId }.toList()
            }).result)
        }

        //只有管理员才能看到封禁和删除的文章内容
        var isUserAdmin = false
        try {
            val account = TokenService.checkToken(CheckTokenReq(req)).account
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

        val result = ReplyService.getReplyCountByArticle(QueryReplyCountByArticleReq().apply {
            this.id = queryId
        })
        return@Route ok {
            it["data"] = result.result
        }
    }

}