package tech.kotlin.model.request

import com.baidu.bjf.remoting.protobuf.FieldType
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf

class QueryReplyCountByArticleReq {
    @Protobuf(order = 1, required = true, fieldType = FieldType.UINT64)
    var id: List<Long> = ArrayList()
}