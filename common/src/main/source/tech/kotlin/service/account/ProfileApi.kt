package tech.kotlin.service.account

import tech.kotlin.common.rpc.annotations.RpcInterface
import tech.kotlin.service.TypeDef
import tech.kotlin.service.account.req.QueryUserReq
import tech.kotlin.service.account.req.UpdateUserReq
import tech.kotlin.service.article.resp.QueryUserResp
import tech.kotlin.service.domain.EmptyResp

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
interface ProfileApi {

    @RpcInterface(TypeDef.User.QUERY_BY_ID)
    fun queryById(req: QueryUserReq): QueryUserResp

    @RpcInterface(TypeDef.User.UPDATE_BY_ID)
    fun updateById(req: UpdateUserReq): EmptyResp

}