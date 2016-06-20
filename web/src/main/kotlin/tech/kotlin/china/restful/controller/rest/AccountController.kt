package tech.kotlin.china.restful.controller.rest

import com.github.pagehelper.PageHelper
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import tech.kotlin.china.restful.database.AccountMapper
import tech.kotlin.china.restful.database.get
import tech.kotlin.china.restful.database.use
import tech.kotlin.china.restful.model.Account
import tech.kotlin.china.restful.model.AccountForm
import tech.kotlin.china.restful.utils.*

@RestController
class AccountController : _Rest() {

    val ACCOUNT_PAGE_SIZE = 20 //账号列表的分页大小
    val PASSWORD_SECRET: String by lazy { config["password_secret"] }

    @Doc("会员账号登录")
    @RequestMapping("/account/login", method = arrayOf(POST))
    fun login(@RequestBody form: AccountForm) = form.check {
        it.name.require("用户名不是合法的邮箱账号") { it.isEmailFormat() }
        it.name.forbid("用户名过长") { it.length > 100 }
        it.password.require("不合法法的密码长度") { it.length >= 6 && it.length < 100 }
    }.session {
        val account = it[AccountMapper::class.java].queryByName(form.name)
                .forbid("该用户不存在") { it == null }!!
                .forbid("用户密码不正确") { !form.password.encrypt(PASSWORD_SECRET).equals(it.password) }
        val token = createToken(uid = account.uid, admin = account.rank == 1, username = form.name)
        @Return Maps.p("token", token).p("uid", account.uid)
    }

    @Doc("会员账号注册")
    @RequestMapping("/account/register", method = arrayOf(POST))
    fun register(@RequestBody @Doc("注册请求") form: AccountForm) = form.check {
        it.name.require("用户名不是合法的邮箱账号") { it.isEmailFormat() }
        it.name.forbid("用户名过长") { it.length > 100 }
        it.password.require("不合法的密码长度") { it.length >= 6 && it.length < 100 }
    }.session(transaction = true) {
        it.use(AccountMapper::class.java) {
            it.queryByName(form.name).forbid("该用户已存在") { it != null }
            it.registerAccount(Maps.p("name", form.name).p("password", form.password.encrypt(PASSWORD_SECRET)))
            val account = it.queryByName(form.name).forbid("注册失败") { it == null }!!
            @Return Maps.p("uid", account.uid)
        }
    }

    @Doc("获得当前注册用户总数")
    @RequestMapping("/account/count", method = arrayOf(GET))
    fun getUserCount() = session { @Return it[AccountMapper::class.java].getUserCount() }

    @Doc("查看用户列表")
    @RequestMapping("/account/list/{page}", method = arrayOf(GET))
    fun userList(@PathVariable("page") @Doc("分页") page: Int, @RequestParam("category", defaultValue = "all")
    @Doc("筛选用户类别(all/admin/disable)") category: String) = check {
        page.require("不合法的页数") { it > 0 }
        category.require("错误的用户类型") { it.equals("all") || it.equals("admin") || it.equals("disable") }
    }.authorized(admin = true).session {
        it.use(AccountMapper::class.java) {
            PageHelper.startPage<Account>((page - 1) * ACCOUNT_PAGE_SIZE + 1, page * ACCOUNT_PAGE_SIZE)
            @Return when (category) {
                "all" -> it.queryUserList()
                "admin" -> it.queryAdminList()
                "disable" -> it.queryDisabledList()
                else -> null
            }!!.map {
                it.expose("uid", "username")
                        .p("rank", if (it.rank == 1) "admin" else "normal")
                        .p("forbidden", if (it.forbidden) "被封禁" else "未封禁")
            }
        }
    }

    @Doc("封禁账号")
    @RequestMapping("/account/{uid}/disable", method = arrayOf(POST))
    fun disableAccount(@PathVariable("uid") @Doc("用户id") uid: Long) = check {
        uid.forbid("不合法的用户id") { uid <= 0 }
    }.authorized(admin = true).session(transaction = true) {
        it.use(AccountMapper::class.java) {
            it.queryByUID(uid).forbid("该用户不存在") { it == null }
            it.enableAccount(Maps.p("uid", uid).p("forbidden", true))
        }
    }

    @Doc("解封账号")
    @RequestMapping("/account/{uid}/enable", method = arrayOf(POST))
    fun enableAccount(@PathVariable("uid") @Doc("用户id") uid: Long) = check {
        uid.forbid("不合法的用户id") { uid <= 0 }
    }.authorized(admin = true).session(transaction = true) {
        it.use(AccountMapper::class.java) {
            it.queryByUID(uid).forbid("该用户不存在") { it == null }
            it.enableAccount(Maps.p("uid", uid).p("forbidden", false))
        }
    }
}
