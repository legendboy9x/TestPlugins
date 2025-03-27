package com.lagradost.cloudstream3.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import org.jsoup.nodes.Element

class BluPhimProvider : MainAPI() {
    override var name = "BluPhim"
    override var mainUrl = "https://bluphim3.com"
    override var lang = "vi" // Ngôn ngữ: Tiếng Việt
    override val hasMainPage = true
    override val hasSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // Trang chủ: Lấy danh sách phim mới
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page").document
        val items = document.select("div.film-item").map { toSearchResponse(it) }
        return HomePageResponse(listOf(HomePageList("Phim Mới Nhất", items)))
    }

    // Tìm kiếm phim
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/tim-kiem?keyword=$query").document
        return document.select("div.film-item").map { toSearchResponse(it) }
    }

    // Chuyển đổi thẻ HTML thành SearchResponse
    private fun toSearchResponse(element: Element): SearchResponse {
        val title = element.selectFirst("h3.film-name")?.text() ?: "Unknown"
        val href = element.selectFirst("a")?.attr("href") ?: ""
        val poster = element.selectFirst("img")?.attr("src") ?: ""
        return MovieSearchResponse(title, href, this.name, TvType.Movie, poster)
    }

    // Tải chi tiết phim và link stream
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.film-title")?.text() ?: "Unknown"
        val description = document.selectFirst("div.film-desc")?.text()
        val poster = document.selectFirst("img.film-poster")?.attr("src")

        // Lấy link stream từ iframe
        val iframe = document.selectFirst("iframe")?.attr("src")
            ?: throw ErrorLoadingException("Không tìm thấy link stream")

        // Nếu iframe là từ bên thứ ba, cần xử lý thêm (ở đây giả định link stream trực tiếp)
        val streamUrl = app.get(iframe, referer = url).url

        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = TvType.Movie,
            dataUrl = streamUrl,
            posterUrl = poster,
            plot = description
        )
    }

    // Tải link stream (nếu cần xử lý thêm)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback(
            ExtractorLink(
                this.name,
                this.name,
                data,
                "$mainUrl/",
                Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8")
            )
        )
        return true
    }
}
