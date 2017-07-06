package tech.kotlin.dao

import org.apache.ibatis.annotations.*
import org.apache.ibatis.session.SqlSession
import tech.kotlin.common.redis.Redis
import tech.kotlin.common.serialize.Json
import tech.kotlin.service.domain.Account
import tech.kotlin.common.mysql.Mysql
import tech.kotlin.common.mysql.get

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object AccountDao {

    init {
        Mysql.register(AccountMapper::class.java)
    }

    fun getById(session: SqlSession, id: Long, useCache: Boolean): Account? {
        if (useCache) {
            val cached = Cache.getById(id)
            if (cached != null) return cached
            val result = session[AccountMapper::class].getById(id)
            if (result != null && useCache) Cache.update(result)
            return result
        } else {
            return session[AccountMapper::class].getById(id)
        }
    }

    fun saveOrUpdate(session: SqlSession, account: Account) {
        val mapper = session[AccountMapper::class]
        val mayBeNull = mapper.getById(account.id)
        if (mayBeNull == null) {
            mapper.insert(account)
        } else {
            Cache.invalid(account.id)
            mapper.update(account)
        }
    }

    fun update(session: SqlSession, uid: Long, args: HashMap<String, String>) {
        val mapper = session[AccountMapper::class]
        Cache.invalid(uid)
        mapper.updateWithArgs(args = args.apply { this["id"] = "$uid" })
    }

    internal object Cache {

        fun key(uid: Long) = "account:$uid"

        fun getById(uid: Long): Account? {
            val userMap = Redis { it.hgetAll(key(uid)) }
            return if (!userMap.isEmpty()) Json.rawConvert<Account>(userMap) else null
        }

        fun update(account: Account) {
            val key = key(account.id)
            Redis {
                val map = HashMap<String, String>()
                Json.reflect(account) { obj, name, field -> map[name] = "${field.get(obj)}" }
                it.hmset(key, map)
            }
        }

        fun invalid(uid: Long) {
            Redis { it.del(key(uid)) }
        }
    }

    interface AccountMapper {

        @Select("""
        SELECT  * FROM account
        WHERE id = #{id}
        LIMIT 1
        """)
        @Results(
                Result(property = "lastLogin", column = "last_login"),
                Result(property = "createTime", column = "create_time")
                )
        fun getById(id: Long): Account?

        @Insert("""
        INSERT INTO account
        VALUES
        (#{id}, #{password}, #{lastLogin}, #{state}, #{role}, #{createTime})
        """)
        fun insert(account: Account)

        @Update("""
        UPDATE account SET
        password = #{password},
        last_login = #{lastLogin},
        state = #{state},
        role = #{role},
        create_time = #{createTime}
        WHERE
        id = #{id}
        """)
        fun update(account: Account)

        @UpdateProvider(type = SQLGen::class, method = "updateWithArgs")
        fun updateWithArgs(args: Map<String, Any>)

        class SQLGen {

            fun updateWithArgs(args: Map<String, Any>): String {
                return """
                UPDATE account SET
                ${StringBuilder().apply {
                    args.forEach { k, _ -> append("$k = #{$k}, ") }
                    setLength(length - ", ".length)
                }}
                WHERE id = #{id}
                """
            }
        }
    }
}

