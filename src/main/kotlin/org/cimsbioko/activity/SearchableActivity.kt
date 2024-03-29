package org.cimsbioko.activity

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.Group
import androidx.core.graphics.drawable.DrawableCompat
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.flexible.core.QueryNodeException
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser
import org.apache.lucene.search.*
import org.cimsbioko.R
import org.cimsbioko.data.DataWrapper
import org.cimsbioko.databinding.SearchResultBinding
import org.cimsbioko.databinding.SearchResultsBinding
import org.cimsbioko.navconfig.*
import org.cimsbioko.navconfig.DefaultQueryHelper.getParent
import org.cimsbioko.search.SearchJob
import org.cimsbioko.search.SearchQueue
import org.cimsbioko.search.translate
import org.cimsbioko.utilities.ConfigUtils.getActiveModules
import org.cimsbioko.utilities.MessageUtils.showLongToast
import org.cimsbioko.utilities.MessageUtils.showShortToast
import java.io.IOException
import java.util.*

class SearchableActivity : AppCompatActivity() {

    private lateinit var searchQueue: SearchQueue
    private lateinit var handler: Handler
    private lateinit var progressGroup: Group
    private lateinit var listView: ListView
    private lateinit var basicQuery: EditText
    private lateinit var advancedQuery: EditText
    private lateinit var searchButton: Button
    private lateinit var hierarchyToggle: ToggleButton
    private lateinit var locationToggle: ToggleButton
    private lateinit var individualToggle: ToggleButton
    private lateinit var enabledLevels: Set<String>

    private var advancedSelected = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = SearchResultsBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setSupportActionBar(searchToolbar)
        }

        setTitle(R.string.search_label)

        basicQuery = binding.basicQueryText
        advancedQuery = binding.advancedQueryText
        searchButton = binding.searchButton
        listView = binding.list
        progressGroup = binding.progressGroup

        basicQuery.addTextChangedListener(BasicQueryTranslator())

        val config = NavigatorConfig.instance
        val intent = intent
        enabledLevels = HashSet(intent.getStringExtra(ENABLED_LEVELS_KEY)?.split("[|]".toRegex()) ?: config.levels)

        listView.onItemClickListener = ItemClickListener(listView, intent.getStringExtra(FieldWorkerActivity.ACTIVITY_MODULE_EXTRA))
        searchButton.setOnClickListener(SearchOnClickHandler())
        SearchOnEnterKeyHandler().also { listOf(basicQuery, advancedQuery).forEach { q -> q.setOnKeyListener(it) } }

        hierarchyToggle = binding.hierarchyToggle.apply { setToggleImage(R.drawable.ic_hierarchy_sm) }
        locationToggle = binding.locationToggle.apply { setToggleImage(R.drawable.ic_household_sm) }
        individualToggle = binding.individualToggle.apply { setToggleImage(R.drawable.ic_individual_sm) }

        EntityToggleHandler().let {
            listOf(hierarchyToggle, locationToggle, individualToggle).forEach { t -> t.setOnCheckedChangeListener(it) }
        }

        val enabledAdminLevels: MutableSet<String> = HashSet(enabledLevels).apply { retainAll(config.adminLevels) }
        val androidSearch = Intent.ACTION_SEARCH == intent.action
        val allowToggle = androidSearch || "true" == intent.getStringExtra(ALLOW_TOGGLE_KEY)

        enabledAdminLevels.isNotEmpty().let {
            hierarchyToggle.isChecked = it
            hierarchyToggle.isEnabled = allowToggle && it
        }

        enabledLevels.contains(Hierarchy.HOUSEHOLD).let {
            locationToggle.isChecked = it
            locationToggle.isEnabled = allowToggle && it
        }

        enabledLevels.contains(Hierarchy.INDIVIDUAL).let {
            individualToggle.isChecked = it
            individualToggle.isEnabled = allowToggle && it
        }

        searchQueue = SearchQueue()
        handler = Handler()

        savedInstanceState?.let { advancedSelected = it.getBoolean(ADVANCED_SET_KEY) }

        if (androidSearch) {
            intent.getStringExtra(SearchManager.QUERY)?.lowercase(Locale.getDefault())?.let {
                basicQuery.setText(it)
                doSearch()
            }
        }
    }

    private fun ToggleButton.setToggleImage(drawableId: Int) {
        AppCompatResources.getDrawable(context, drawableId)
                ?.let { DrawableCompat.wrap(it) }
                ?.mutate()
                ?.also { DrawableCompat.setTint(it, R.color.Black) }
                ?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }
                ?.let { ImageSpan(it) }
                ?.let { SpannableString("X").apply { setSpan(it, 0, 1, 0) } }
                ?.also {
                    text = it; textOn = it; textOff = it
                }
    }

    private val selectedLevels: Set<String>
        get() = HashSet<String>().apply {
            if (hierarchyToggle.isChecked) {
                addAll(NavigatorConfig.instance.adminLevels)
            }
            if (locationToggle.isChecked) {
                add(Hierarchy.HOUSEHOLD)
            }
            if (individualToggle.isChecked) {
                add(Hierarchy.INDIVIDUAL)
            }
            retainAll(enabledLevels)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ADVANCED_SET_KEY, advancedSelected)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        menu.findItem(R.id.search_type).let { it.actionView as Spinner }.apply {
            onItemSelectedListener = SearchTypeSelectionHandler()
            setSelection(if (advancedSelected) ADVANCED_POS else BASIC_POS)
        }
        return true
    }

    override fun onDestroy() {
        searchQueue.shutdown()
        super.onDestroy()
    }

    private fun doSearch() {
        try {
            advancedQuery.text.toString()
                    .takeIf { it.isNotBlank() }
                    ?.also { executeQuery(addLevelClause(parseLuceneQuery(it))) }
        } catch (e: QueryNodeException) {
            Log.e(TAG, "bad query", e)
            listView.adapter = ResultsAdapter(this, emptyList())
        }
    }

    private inner class EntityToggleHandler : CompoundButton.OnCheckedChangeListener {

        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            if (!isChecked) ensureOneChecked(buttonView)
        }

        private fun ensureOneChecked(uncheckedButton: CompoundButton) {
            listOf(hierarchyToggle, locationToggle, individualToggle).filter { it.isChecked }.count().let {
                uncheckedButton.isChecked = it <= 0
            }
        }
    }

    private inner class SearchTypeSelectionHandler : OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            advancedSelected = position == ADVANCED_POS
            updateQueryViews()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
        private fun updateQueryViews() {
            basicQuery.visibility = if (advancedSelected) View.GONE else View.VISIBLE
            advancedQuery.visibility = if (advancedSelected) View.VISIBLE else View.GONE
        }
    }

    private inner class SearchOnClickHandler : View.OnClickListener {
        override fun onClick(v: View) {
            doSearch()
        }
    }

    private inner class SearchOnEnterKeyHandler : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    searchButton.performClick()
                    return true
                }
            }
            return false
        }
    }

    private inner class BasicQueryTranslator : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            advancedQuery.setText(buildLuceneQuery(basicQuery.text.toString()).toString())
        }
    }

    private inner class ItemClickListener(private val listView: ListView) : OnItemClickListener {

        private var fromModule: String? = null

        constructor(listView: ListView, fromModule: String?) : this(listView) {
            this.fromModule = fromModule
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            (listView.getItemAtPosition(position) as DataWrapper).also {
                when (intent.action) {
                    Intent.ACTION_SEARCH -> viewInHierarchy(it)
                    ACTION_ENTITY_LOOKUP -> finishWithResult(it)
                }
            }
        }

        private fun finishWithResult(selected: DataWrapper) {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("value", selected.uuid)
                putExtra("name", selected.name)
                putExtra("uuid", selected.uuid)
                putExtra("category", selected.category)
                putExtra("extId", selected.extId)
                putExtra("hierarchyId", selected.hierarchyId)
            }).also { finish() }
        }

        private fun viewInHierarchy(clickedItem: DataWrapper) {
            // construct path by starting at item and traversing parents
            val reversePath = Stack<DataWrapper>().apply {
                push(clickedItem)
                var item = getParent(clickedItem)
                while (item != null) {
                    push(item)
                    item = getParent(item)
                }
            }

            val rootLevel = NavigatorConfig.instance.levels[0]
            if (!reversePath.isEmpty() && reversePath.peek().category == rootLevel) {
                val path = HierarchyPath().apply {
                    while (!reversePath.isEmpty()) reversePath.pop().let { down(it.category, it) }
                }
                val activeModules = getActiveModules(this@SearchableActivity)
                val moduleToLaunch = fromModule ?: if (!activeModules.isEmpty()) {
                    activeModules.iterator().next().name
                } else {
                    showLongToast(this@SearchableActivity, R.string.no_active_modules)
                    return
                }
                Intent(this@SearchableActivity, HierarchyNavigatorActivity::class.java).apply {
                    putExtra(FieldWorkerActivity.ACTIVITY_MODULE_EXTRA, moduleToLaunch)
                    putExtra(HierarchyNavigatorActivity.HIERARCHY_PATH_KEY, path)
                    startActivity(this)
                }
            } else {
                showShortToast(this@SearchableActivity, R.string.result_invalid_path)
            }
        }

        private fun getParent(item: DataWrapper): DataWrapper? = getParent(item.category, item.uuid)?.firstWrapper

    }

    private fun buildLuceneQuery(query: String): Query = NavigatorConfig.instance.searchQueryBuilder.build(query).translate()

    private fun addLevelClause(orig: Query): Query = BooleanQuery().also { q ->
        q.add(orig, BooleanClause.Occur.MUST)
        selectedLevels
                .map { TermQuery(Term("level", it)) }
                .fold(BooleanQuery()) { tq, term -> tq.apply { add(term, BooleanClause.Occur.SHOULD) } }
                .takeIf { it.clauses.isNotEmpty() }
                ?.also { q.add(it, BooleanClause.Occur.MUST) }
    }

    @Throws(QueryNodeException::class)
    private fun parseLuceneQuery(query: String): Query {
        val parser = StandardQueryParser()
        parser.allowLeadingWildcard = true
        return parser.parse(query, "name")
    }

    private fun showLoading(loading: Boolean) {
        progressGroup.visibility = if (loading) View.VISIBLE else View.GONE
        listView.visibility = if (loading) View.GONE else View.VISIBLE
    }

    private fun executeQuery(query: Query) {
        showLoading(true)
        searchQueue.queue(BoundedSearch(query, 100))
    }

    private fun handleSearchResults(results: List<DataWrapper>) {
        listView.adapter = ResultsAdapter(this, results)
        showLoading(false)
        (if (advancedSelected) advancedQuery else basicQuery).requestFocus()
    }

    private inner class BoundedSearch(private val query: Query, private val limit: Int) : SearchJob() {

        private val items: MutableList<DataWrapper> = ArrayList()

        @Throws(IOException::class)
        override fun IndexSearcher.performSearch() {
            Log.i(TAG, "searching: $query")
            items.clear()
            val helper: QueryHelper = DefaultQueryHelper
            search(query, limit).scoreDocs
                    .map { doc(it.doc) }
                    .mapNotNull { helper[it["level"], it["uuid"]]?.firstWrapper }
                    .forEach { items.add(it) }
        }

        override fun postResult() {
            handler.post { handleSearchResults(items) }
        }

        override fun handleException(e: Exception) {
            handler.post { showLoading(false) }
            super.handleException(e)
        }
    }

    private class ResultsAdapter(context: Context, objects: List<DataWrapper>) : ArrayAdapter<DataWrapper>(context, -1, objects) {
        private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return (convertView?.tag as? SearchResultBinding
                    ?: SearchResultBinding.inflate(inflater).apply { root.tag = this })
                    .also { vh ->
                        getItem(position)?.apply {
                            vh.icon.apply {
                                when (category) {
                                    Hierarchy.HOUSEHOLD -> setImageResource(R.drawable.ic_household)
                                    Hierarchy.INDIVIDUAL -> setImageResource(R.drawable.ic_individual)
                                    else -> setImageResource(R.drawable.ic_hierarchy)
                                }
                            }
                            vh.text1.text = name
                            vh.text2.text = extId
                        }
                    }.root
        }
    }

    companion object {
        private val TAG = SearchableActivity::class.java.simpleName
        private const val ACTION_ENTITY_LOOKUP = "org.cimsbioko.ENTITY_LOOKUP"
        private const val ADVANCED_SET_KEY = "advanced_set"
        private const val ALLOW_TOGGLE_KEY = "allow_toggle"
        private const val ENABLED_LEVELS_KEY = "levels"
        const val ADVANCED_POS = 1
        const val BASIC_POS = 0
    }
}