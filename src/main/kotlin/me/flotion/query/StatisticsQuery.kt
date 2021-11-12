package me.flotion.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import me.flotion.config.MODULE_SELECT_KEY
import me.flotion.config.NotionSingleton
import me.flotion.config.ResponseMessages
import me.flotion.config.UNDERSTANDING_SELECT_KEY
import me.flotion.context.NotionContext
import me.flotion.model.FlashcardFactory
import me.flotion.model.NotionUser
import me.flotion.model.Understanding
import me.flotion.services.StatisticsService
import org.jraf.klibnotion.model.database.query.DatabaseQuery
import org.jraf.klibnotion.model.database.query.filter.DatabaseQueryPredicate
import org.jraf.klibnotion.model.database.query.filter.DatabaseQueryPropertyFilter
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.pagination.Pagination
import org.jraf.klibnotion.model.pagination.ResultPage
import org.jraf.klibnotion.model.property.sort.PropertySort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StatisticsQuery @Autowired constructor(private val statsService: StatisticsService) : Query {
	class StatData(val name: String, val module: String, val amount: Int)
	class StatsResponse(
		val response: Int = 200, val message: String = ResponseMessages.SUCCESS.message,
		var modules: List<String>? = null,
		var overall: StatData? = null,
		var moduleRed: List<StatData>? = null,
		var moduleYellow: List<StatData>? = null,
		var moduleGreen: List<StatData>? = null,
		var overallRed: Int? = null, var overallYellow: Int? = null, var overallGreen: Int? = null,
		var moduleCount: Int? = null
	)

	@GraphQLDescription("gets statistics on user's flashcards")
	suspend fun getStats(hiddenModules: List<String>, context: NotionContext): StatsResponse {
		try {
			if (context.user == null) return StatsResponse(401, ResponseMessages.NOT_LOGGED_IN.message)

			val hiddenSet = setOf(*hiddenModules.toTypedArray())
			val modules = context.user.getAllModules().filter { it !in hiddenSet }

			val allCards = statsService.getFilteredStatsCards(hiddenModules, context.user)

			val (redModuleCounts, yellowModuleCounts, greenModuleCounts) = statsService.getModuleUnderstandingCounts(allCards, context.user)

			return statsService.buildResponseObject(
				redModuleCounts,
				yellowModuleCounts,
				greenModuleCounts,
				modules
			)
		} catch(e: Exception) {
			return StatsResponse(500, ResponseMessages.SERVER_ERROR.message)
		}
	}
}
