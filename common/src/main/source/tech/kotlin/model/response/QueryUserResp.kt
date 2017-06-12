package tech.kotlin.model.response

import com.baidu.bjf.remoting.protobuf.FieldType
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf
import com.fasterxml.jackson.annotation.JsonProperty
import tech.kotlin.model.domain.Account
import tech.kotlin.model.domain.UserInfo

class QueryUserResp {

    @Protobuf(order = 1, required = true, fieldType = FieldType.MAP, description = "查询结果")
    @JsonProperty("account")
    var account: Map<Long, Account> = HashMap()

    @Protobuf(order = 2, required = true, fieldType = FieldType.MAP, description = "查询结果")
    @JsonProperty("info")
    var info: Map<Long, UserInfo> = HashMap()

}