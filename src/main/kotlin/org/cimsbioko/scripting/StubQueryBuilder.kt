package org.cimsbioko.scripting

import android.util.Log
import org.cimsbioko.search.*

private class StubBooleanQuery(
        override val clauses: List<BooleanClause> = emptyList()
) : BooleanQuery

private class StubBooleanClause(
        override val query: Query,
        override val occurs: BooleanClause.Occurs = BooleanClause.Occurs.SHOULD
) : BooleanClause

private class StubFuzzyQuery(
        override val field: String,
        override val text: String,
        override val maxEdits: Int = 2
) : FuzzyQuery

class StubQueryBuilder : SearchQueryBuilder {

    companion object {
        private const val TAG = "QueryBuilder"
    }

    override fun build(query: String): Query {
        Log.i(TAG, "using fallback search query builder")
        return query
                .split(Regex("\\s+"))
                .map { StubBooleanClause(StubFuzzyQuery(field = "name", text = it)) }
                .let { StubBooleanQuery(it) }
    }
}