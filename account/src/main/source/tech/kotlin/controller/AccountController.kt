package tech.kotlin.controller

import spark.Route
import tech.kotlin.common.rpc.Serv
import tech.kotlin.common.utils.*
import tech.kotlin.service.account.*
import tech.kotlin.service.account.req.*
import tech.kotlin.service.account.resp.GithubCheckTokenReq
import tech.kotlin.service.article.req.EmailCheckTokenReq
import tech.kotlin.service.domain.Account
import tech.kotlin.service.domain.Device
import tech.kotlin.service.domain.UserInfo
import java.net.URLDecoder

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object AccountController {

    val accountApi by Serv.bind(AccountApi::class)
    val sessionApi by Serv.bind(SessionApi::class)
    val userApi by Serv.bind(UserApi::class)
    val githubApi by Serv.bind(GithubApi::class)
    val emailApi by Serv.bind(EmailApi::class)

    val login = Route { req, _ ->
        val githubToken = req.queryParams("github_token")

        val loginResp = accountApi.loginWithName(LoginReq().apply {
            this.device = tryExec(Err.PARAMETER, "无效的设备信息") { Device(req) }

            this.loginName = req.queryParams("login_name")
                    .check(Err.PARAMETER, "无效的用户名") { !it.isNullOrBlank() }

            this.password = req.queryParams("password")
                    .check(Err.PARAMETER, "无效的密码") { !it.isNullOrBlank() }

            if (!githubToken.isNullOrBlank()) {
                this.githubUser = githubApi.checkToken(GithubCheckTokenReq().apply {
                    this.token = githubToken
                    this.device = Device(req)
                }).info
            }
        })

        return@Route ok {
            it["uid"] = loginResp.userInfo.uid
            it["token"] = loginResp.token
            it["username"] = loginResp.userInfo.username
            it["email"] = loginResp.userInfo.email
            it["is_email_validate"] = loginResp.userInfo.emailState == UserInfo.EmailState.VERIFIED
            it["role"] = loginResp.account.role
        }
    }

    val register = Route { req, _ ->
        val username = req.queryParams("username")
                .check(Err.PARAMETER, "无效的用户名") { !it.isNullOrBlank() && it.trim().length >= 2 }
        val password = req.queryParams("password")
                .check(Err.PARAMETER, "无效的密码") { !it.isNullOrBlank() && it.length >= 8 }
        val email = req.queryParams("email")
                .check(Err.PARAMETER, "无效的邮箱") {
                    !it.isNullOrBlank() && it.matches(Regex(
                            "^\\s*\\w+(?:\\.?[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$"
                    ))
                }

        val githubToken = req.queryParams("github_token")

        //创建账号
        val createResp = accountApi.create(CreateAccountReq().apply {
            this.username = username
            this.password = password
            this.email = email
            this.device = tryExec(Err.PARAMETER, "无效的设备信息") { Device(req) }
            if (!githubToken.isNullOrBlank()) {
                this.githubUser = githubApi.checkToken(GithubCheckTokenReq().apply {
                    this.token = githubToken
                    this.device = Device(req)
                }).info
            }
        })

        //修改头像
        val logo = req.queryParams("logo") ?: ""
        if (!logo.isNullOrBlank()) {
            userApi.updateById(UpdateUserReq().apply {
                this.id = createResp.account.id
                this.args = strDict {
                    if (!logo.isNullOrBlank()) this["logo"] = logo
                }
            })
        }

        return@Route ok {
            it["uid"] = createResp.account.id
            it["token"] = createResp.token
        }
    }

    val activateEmail = Route { req, _ ->
        val token = URLDecoder.decode(req.queryParams("token"), "UTF-8")
        val resp = emailApi.checkToken(EmailCheckTokenReq().apply { this.token = token })
        userApi.activateEmail(ActivateEmailReq().apply { this.uid = resp.uid ;this.email = resp.email})
        return@Route ok()
    }

    val getUserInfo = Route { req, _ ->
        val uid = req.params(":uid")
                .check(Err.PARAMETER, "uid错误") { it.toLong(); true }
                .toLong()

        val owner = sessionApi.checkToken(CheckTokenReq(req)).account
        owner.check(Err.UNAUTHORIZED) { it.role == Account.Role.ADMIN || it.id == uid }

        val queryUser = userApi.queryById(QueryUserReq().apply { id = arrayListOf(uid) })
        val info = queryUser.info[uid] ?: abort(Err.USER_NOT_EXISTS)
        val account = queryUser.account[uid] ?: abort(Err.SYSTEM)

        return@Route ok {
            it["username"] = info.username
            it["email"] = info.email
            it["is_email_validate"] = (info.emailState == UserInfo.EmailState.VERIFIED)
            it["last_login"] = account.lastLogin
            it["state"] = account.state
            it["role"] = account.role
            it["create_time"] = account.createTime
        }
    }

    val alterPassword = Route { req, _ ->
        val uid = req.params(":uid")
                .check(Err.PARAMETER, "uid错误") { it.toLong();true }
                .toLong()

        val password = req.queryParams("password")
                .check(Err.PARAMETER, "密码长度过短") { !it.isNullOrBlank() && it.length >= 8 }

        password.chars().forEach { it ->
            if ('a'.toInt() <= it && it <= 'z'.toInt()) return@forEach
            if ('A'.toInt() <= it && it <= 'Z'.toInt()) return@forEach
            if ('0'.toInt() <= it && it <= '9'.toInt()) return@forEach
            abort(Err.PARAMETER, "密码格式有误")
        }

        sessionApi.checkToken(CheckTokenReq(req)).account
                .check(Err.UNAUTHORIZED) { it.id == uid || it.role == Account.Role.ADMIN }

        accountApi.updatePassword(UpdatePasswordReq().apply {
            this.id = uid
            this.password = password
        })

        return@Route ok()
    }

    val updateUserInfo = Route { req, _ ->
        val uid = req.params(":uid")
                .check(Err.PARAMETER, "uid错误") { it.toLong();true }
                .toLong()

        val username = req.queryParams("username") ?: ""
        val email = req.queryParams("email") ?: ""
        val logo = req.queryParams("logo") ?: ""

        if (username.isNullOrBlank() && email.isNullOrBlank() && logo.isNullOrBlank())
            abort(Err.PARAMETER)

        sessionApi.checkToken(CheckTokenReq(req)).account
                .check(Err.UNAUTHORIZED) { it.id == uid || it.role == Account.Role.ADMIN }

        userApi.updateById(UpdateUserReq().apply {
            this.id = uid
            this.args = strDict {
                if (!username.isNullOrBlank()) this["username"] = username
                if (!email.isNullOrBlank()) this["email"] = email
                if (!logo.isNullOrBlank()) this["logo"] = logo
            }
        })
        return@Route ok()
    }

}