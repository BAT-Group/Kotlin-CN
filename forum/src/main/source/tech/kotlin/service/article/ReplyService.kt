package tech.kotlin.service.article

import com.github.pagehelper.PageHelper
import com.relops.snowflake.Snowflake
import tech.kotlin.dao.article.ArticleDao
import tech.kotlin.dao.article.ReplyDao
import tech.kotlin.model.domain.Reply
import tech.kotlin.model.request.*
import tech.kotlin.model.response.CreateReplyResp
import tech.kotlin.model.response.EmptyResp
import tech.kotlin.model.response.QueryReplyByArticleResp
import tech.kotlin.model.response.QueryReplyByIdResp
import tech.kotlin.utils.exceptions.Err
import tech.kotlin.utils.exceptions.abort
import tech.kotlin.utils.mysql.Mysql


/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object ReplyService {

    fun create(req: CreateArticleReplyReq): CreateReplyResp {
        if (req.aliasId != 0L) {
            Mysql.read {
                val reply = ReplyDao.getById(it, req.aliasId) ?:
                        abort(Err.REPLY_NOT_EXISTS, "关联评论不存在")

                if (reply.aliasId != req.aliasId)
                    abort(Err.REPLY_NOT_EXISTS, "关联评论不存在")

                ArticleDao.getById(it, req.articleId, useCache = true, updateCache = true) ?:
                        abort(Err.ARTICLE_NOT_EXISTS)
            }
        }

        val replyId = Snowflake(0).next()
        val contentId = TextService.createContent(CreateTextContentReq().apply {
            this.serializeId = "reply:$replyId"
            this.content = req.content
        }).id

        val reply = Reply().apply {
            this.id = replyId
            this.replyPoolId = "article:${req.articleId}"
            this.ownerUID = req.ownerUID
            this.createTime = System.currentTimeMillis()
            this.state = Reply.State.NORMAL
            this.contentId = contentId
            this.aliasId = req.aliasId
        }
        Mysql.write { ReplyDao.create(it, reply) }
        return CreateReplyResp().apply {
            this.replyId = reply.id
            this.contentId = contentId
        }
    }

    fun changeState(req: ChangeReplyStateReq): EmptyResp {
        Mysql.write { ReplyDao.update(it, req.replyId, args = hashMapOf("state" to "${req.state}")) }
        return EmptyResp()
    }

    fun getReplyById(req: QueryReplyByIdReq): QueryReplyByIdResp {
        val result = HashMap<Long, Reply>()
        Mysql.read { session ->
            req.id.forEach {
                val reply = ReplyDao.getById(session, it) ?: return@forEach
                result[it] = reply
            }
        }
        return QueryReplyByIdResp().apply { this.result = result }
    }

    fun getReplyByArticle(req: QueryReplyByArticleReq): QueryReplyByArticleResp {
        val result = Mysql.read {
            PageHelper.startPage<Reply>(req.offset + 1, req.limit
            ).doSelectPageInfo<Reply> {
                ReplyDao.getByPool(it, "article:${req.articleId}")
            }.list
        }
        return QueryReplyByArticleResp().apply {
            this.result = result
        }
    }
}




