package tech.kotlin.model.request

import com.baidu.bjf.remoting.protobuf.FieldType
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf
import com.fasterxml.jackson.annotation.JsonProperty
import spark.Request
import tech.kotlin.model.domain.Device
import tech.kotlin.utils.exceptions.Err
import tech.kotlin.utils.exceptions.abort

class CheckTokenReq() {

    @Protobuf(order = 1, required = true, fieldType = FieldType.OBJECT, description = "设备信息")
    @JsonProperty("device")
    var device: Device = Device()

    @Protobuf(order = 2, required = true, fieldType = FieldType.STRING, description = "JWT")
    @JsonProperty("token")
    var token: String = ""

    constructor(req: Request) : this() {
        device = Device(req)
        token = req.headers("X-App-Token") ?: req.cookie("X-App-Token") ?: abort(Err.TOKEN_FAIL, "缺失登录信息")
    }
}