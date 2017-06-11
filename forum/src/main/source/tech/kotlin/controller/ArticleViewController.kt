package tech.kotlin.controller

import spark.Route
import tech.kotlin.common.utils.Props
import tech.kotlin.common.utils.dict
import tech.kotlin.common.utils.str
import tech.kotlin.model.domain.Article
import tech.kotlin.model.domain.Category
import tech.kotlin.model.domain.UserInfo
import tech.kotlin.model.request.QueryLatestArticleReq
import tech.kotlin.model.request.QueryUserReq
import tech.kotlin.ok
import tech.kotlin.service.account.Users
import tech.kotlin.service.article.Articles
import tech.kotlin.utils.Err
import tech.kotlin.utils.check
import java.util.*


/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object ArticleViewController {

    val properties = Props.loads("project.properties")
    val cgiHost = properties str "cgi.host"

    val getList = Route { req, _ ->
        val offset = req.queryParams("offset")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val limit = req.queryParams("limit")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 20

        val articles = Articles.getLatest(QueryLatestArticleReq().apply {
            this.offset = offset
            this.limit = limit
        }).result

        val users = HashMap<Long, UserInfo>()
        if (articles.isNotEmpty()) {
            users.putAll(Users.queryById(QueryUserReq().apply {
                this.id = ArrayList<Long>().apply {
                    addAll(articles.map { it.author })
                    addAll(articles.map { it.lastEditUID })
                }.distinctBy { it }
            }).info)
        }

        return@Route ok {
            it["articles"] = articles.map {
                dict {
                    this["meta"] = it
                    this["author"] = users[it.author] ?: UserInfo()
                    this["last_editor"] = users[it.lastEditUID] ?: UserInfo()
                }
            }
            it["next_offset"] = offset + articles.size
        }
    }

    val getByCategory = Route { req, _ ->
        val category = req.params(":id")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val offset = req.queryParams("offset")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val limit = req.queryParams("limit")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 20

        val articles = Articles.getLatest(QueryLatestArticleReq().apply {
            this.offset = offset
            this.limit = limit
            this.category = "$category"

        }).result

        val users = HashMap<Long, UserInfo>()
        if (articles.isNotEmpty()) {
            users.putAll(Users.queryById(QueryUserReq().apply {
                this.id = ArrayList<Long>().apply {
                    addAll(articles.map { it.author })
                    addAll(articles.map { it.lastEditUID })
                }.distinctBy { it }
            }).info)
        }

        return@Route ok {
            it["articles"] = articles.map {
                dict {
                    this["meta"] = it
                    this["author"] = users[it.author] ?: UserInfo()
                    this["last_editor"] = users[it.lastEditUID] ?: UserInfo()
                }
            }
            it["next_offset"] = offset + articles.size
        }
    }

    val getFine = Route { req, _ ->
        val offset = req.queryParams("offset")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 0

        val limit = req.queryParams("limit")
                ?.apply { check(Err.PARAMETER) { it.toInt();true } }
                ?.toInt()
                ?: 20

        val articles = Articles.getLatest(QueryLatestArticleReq().apply {
            this.offset = offset
            this.limit = limit
            this.state = "${Article.State.FINE}"
        }).result

        val users = HashMap<Long, UserInfo>()
        if (articles.isNotEmpty()) {
            users.putAll(Users.queryById(QueryUserReq().apply {
                this.id = ArrayList<Long>().apply {
                    addAll(articles.map { it.author })
                    addAll(articles.map { it.lastEditUID })
                }.distinctBy { it }
            }).info)
        }

        return@Route ok {
            it["articles"] = articles.map {
                dict {
                    this["meta"] = it
                    this["author"] = users[it.author] ?: UserInfo()
                    this["last_editor"] = users[it.lastEditUID] ?: UserInfo()
                }
            }
            it["next_offset"] = offset + articles.size
        }
    }

    val getCategory = Route { _, _ -> ok { it["category"] = Category.values().map { it.value } } }

}