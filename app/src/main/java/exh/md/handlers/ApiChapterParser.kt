package exh.md.handlers

import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.Page
import exh.md.handlers.serializers.ChapterResponse
import exh.md.utils.MdUtil
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response

class ApiChapterParser {
    fun pageListParse(response: Response, host: String, dataSaver: Boolean): List<Page> {
        val networkApiChapter = response.parseAs<ChapterResponse>(MdUtil.jsonParser)

        val pages = mutableListOf<Page>()

        val atHomeRequestUrl = response.request.url.toUrl().toString()

        val hash = networkApiChapter.data.attributes.hash
        val pageArray = if (dataSaver) {
            networkApiChapter.data.attributes.dataSaver.map { "/data-saver/$hash/$it" }
        } else {
            networkApiChapter.data.attributes.data.map { "/data/$hash/$it" }
        }
        val now = System.currentTimeMillis()
        pageArray.forEach { imgUrl ->
            val mdAtHomeUrl = "$host,$atHomeRequestUrl,$now"
            pages += Page(pages.size, mdAtHomeUrl, imgUrl)
        }

        return pages
    }

    fun externalParse(response: Response): String {
        val json = response.parseAs<JsonObject>()
        val external = json["data"]!!.jsonObject["pages"]!!.jsonPrimitive.content
        return external.substringAfterLast("/")
    }
}
