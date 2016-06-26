package tech.kotlin.china.controller.rest

import com.github.pagehelper.PageHelper
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import tech.kotlin.china.framework.of
import tech.kotlin.china.mapper.ArticleMapper
import tech.kotlin.china.mapper.FlowerMapper
import tech.kotlin.china.mapper.MessageMapper
import tech.kotlin.china.model.Article
import tech.kotlin.china.model.ArticleForm
import utils.dataflow.Doc
import utils.dataflow.forbid
import utils.dataflow.next
import utils.date.format
import utils.map.get
import utils.map.p

@RestController
class ArticleController : _Rest() {

    val ARTICLE_PAGE_SIZE = 10 //账号列表的分页大小

    @Doc("文章内容")
    @RequestMapping("/article/{aid}", method = arrayOf(GET))
    fun article(@PathVariable("aid") @Doc("文章id") aid: Long) = session(transaction = true) {
        val article = it.of<ArticleMapper>().queryByAID(aid)
                .forbid("文章不存在", 404) { it == null }!!
        article[Article::aid, Article::title, Article::description, Article::content,
                Article::author, Article::comment, Article::flower]
                .p("create_time", article.create_time.format())
    }

    @Doc("查看当前文章总数")
    @RequestMapping("/article/count", method = arrayOf(GET))
    fun getArticleCount() = session { it.of<ArticleMapper>().getArticleCount() }


    @Doc("查看文章列表")
    @RequestMapping("/article/list/{page}", method = arrayOf(GET))
    fun articleList(@PathVariable("page") @Doc("分页") page: Int) = session {
        PageHelper.startPage<Article>((page - 1) * ARTICLE_PAGE_SIZE + 1, page * ARTICLE_PAGE_SIZE)
        it.of<ArticleMapper>().queryArticleList().map {
            it[Article::aid, Article::title, Article::description,
                    Article::author, Article::comment, Article::flower]
                    .p("create_time", it.create_time.format())
        }
    }

    @Doc("查看我的文章列表")
    @RequestMapping("/article/mine/{page}", method = arrayOf(GET))
    fun myArticles(@PathVariable("page") @Doc("文章分页") page: Int) = authorized().session {
        PageHelper.startPage<Article>((page - 1) * ARTICLE_PAGE_SIZE + 1, page * ARTICLE_PAGE_SIZE)
        it.of<ArticleMapper>().queryByUID(getUID()!!).map {
            it[Article::aid, Article::title, Article::description,
                    Article::author, Article::comment, Article::flower]
                    .p("create_time", it.create_time.format())
                    .p("status", if (it.forbidden) "管理员锁定" else "正常")
        }
    }

    @Doc("发布一篇文章")
    @RequestMapping("/article/publish", method = arrayOf(POST))
    fun publishArticle(@RequestBody @Doc("发布文章请求") form: ArticleForm) = form.check {
        it.title.forbid("标题长度不合法") { it.length > 128 || it.length == 0 }
        it.content.forbid("请输入文章内容") { it.length == 0 }
        it.description.forbid("请输入文章描述") { it.length == 0 }
    }.authorized().session(transaction = true) {
        it.of<ArticleMapper>().publishArticle(
                form[ArticleForm::title, ArticleForm::content, ArticleForm::description].p("uid", getUID()!!))
    }

    @Doc("修改我的文章")
    @RequestMapping("/article/{aid}/update", method = arrayOf(POST))
    fun updateArticle(@PathVariable("aid") @Doc("文章id") aid: Long,
                      @RequestBody @Doc("文章内容") form: ArticleForm) = form.check {
        it.title.forbid("标题长度不合法") { it.length > 100 || it.length == 0 }
        it.content.forbid("请输入文章内容") { it.length == 0 }
        it.description.forbid("请输入文章描述") { it.length == 0 }
    }.authorized {
        it.of<ArticleMapper>().queryByAID(aid)
                .forbid("没有这篇文章嘞!", 404) { it == null }!!
                .forbid("您没有修改这篇文章的权利!", 403) { it.author != getUID()!! }
    }.session(transaction = true) {
        it.of<ArticleMapper>().updateArticle(
                form[ArticleForm::title, ArticleForm::content, ArticleForm::description].p("aid", aid))
    }

    @Doc("点赞文章")
    @RequestMapping("/article/{aid}/flower", method = arrayOf(POST))
    fun praiseArticle(@PathVariable("aid") @Doc("文章id") aid: Long) = authorized(strict = true) {
        it.of<FlowerMapper>().queryActor(p("mode", 0).p("oid", aid).p("actor", getUID()!!))
                .forbid("你已经点过赞了!") { it != 0 }
    }.session(transaction = true) {
        val args = p("mode", 0).p("oid", aid).p("actor", getUID()!!)
        val articleMapper = it.of<ArticleMapper>()
        val article = articleMapper.queryByAID(aid).forbid("该文章不存在", 404) { it == null }!!

        //更新点赞数量
        val count = (it.of<FlowerMapper>() next { it.addFlower(args) }).objectPraisedCount(args)
        articleMapper.updateFlowerCount(p("flower", count).p("aid", aid))

        //向作者发送点赞的信息
        it.of<MessageMapper>().addMessage(p("title", "有人称赞了你的文章")
                .p("content", "${getToken()!!.username} 称赞了你的文章 <${ article.title }> ")
                .p("from", null)
                .p("to", article.author))
    }

    @Doc("取消文章点赞")
    @RequestMapping("/article/{aid}/flower/cancel", method = arrayOf(POST))
    fun cancelPraiseArticle(@PathVariable("aid") @Doc("文章id") aid: Long) = authorized(strict = true) {
        it.of<FlowerMapper>().queryActor(p("mode", 0).p("oid", aid).p("actor", getUID()!!))
                .forbid("你还没点过赞呢!") { it == 0 }
    }.session(transaction = true) {
        val args = p("mode", 0).p("oid", aid).p("actor", getUID()!!)
        it.of<FlowerMapper>().cancelFlower(args)
        val count = it.of<FlowerMapper>().objectPraisedCount(args)
        it.of<ArticleMapper>().updateFlowerCount(p("flower", count).p("aid", aid))
    }

    @Doc("锁定文章")
    @RequestMapping("/article/{aid}/disable", method = arrayOf(POST))
    fun disableArticle(@PathVariable("aid") @Doc("文章id") aid: Long) = authorized(admin = true)
            .session(transaction = true) {
                val mapper = it.of<ArticleMapper>()
                mapper.queryByAID(aid).forbid("该文章不存在", 404) { it == null }
                mapper.enableArticle(p("aid", aid).p("disable", true))
            }

    @Doc("解锁文章")
    @RequestMapping("/article/{aid}/enable", method = arrayOf(POST))
    fun enableArticle(@PathVariable("aid") @Doc("文章id") aid: Long) = authorized(admin = true)
            .session(transaction = true) {
                val mapper = it.of<ArticleMapper>()
                mapper.queryByAID(aid).forbid("该文章不存在", 404) { it == null }
                mapper.enableArticle(p("aid", aid).p("disable", false))
            }
}