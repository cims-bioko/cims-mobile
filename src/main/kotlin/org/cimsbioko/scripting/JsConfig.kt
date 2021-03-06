package org.cimsbioko.scripting

import android.util.Log
import org.cimsbioko.model.Individual
import org.cimsbioko.model.Location
import org.cimsbioko.model.LocationHierarchy
import org.cimsbioko.navconfig.*
import org.cimsbioko.search.*
import org.cimsbioko.utilities.DateUtils
import org.cimsbioko.utilities.FormUtils
import org.cimsbioko.utilities.IdHelper
import org.cimsbioko.utilities.StringUtils
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaClass
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.Closeable
import java.io.IOException
import java.net.*
import java.util.*
import java.util.ResourceBundle.Control

class JsConfig(private val loader: ClassLoader = JsConfig::class.java.classLoader!!) : Closeable {

    var hierarchy: Hierarchy = StubHierarchy()
        private set
    var navigatorModules: Array<NavigatorModule> = emptyArray()
        private set
    var searchSources: Map<String, SearchSource> = emptyMap()
        private set
    var searchQueryBuilder: SearchQueryBuilder = StubQueryBuilder()
        private set
    var adminSecret: String? = null
        private set

    @Throws(URISyntaxException::class)
    fun load(): JsConfig {
        val ctx = Context.enter()
        return try {
            val scope = buildScope(ctx)
            installConstants(scope)
            val require = enableJsModules(ctx, scope)
            Log.i(TAG, "loading init module")
            val init = require.requireMain(ctx, MOBILE_INIT_MODULE)
            hierarchy = init.getTypedProperty("hierarchy") ?: StubHierarchy()
            navigatorModules = init.getTypedProperty("navmods") ?: emptyArray()
            adminSecret = init.getTypedProperty("adminSecret")
            searchSources = init.getTypedProperty("searchSources") ?: emptyMap()
            searchQueryBuilder = init.getTypedProperty("searchQueryBuilder") ?: StubQueryBuilder()
            this
        } finally {
            Context.exit()
        }
    }

    private fun installConstants(scope: ScriptableObject) {
        installTranslationService(scope)
        installDbService(scope)
        installInterfaces(scope)
        installDomainClasses(scope)
        installUtilityObjects(scope)
    }

    private fun installDbService(scope: ScriptableObject) {
        putConst(scope, DB_NAME, Gateways)
    }

    private fun installTranslationService(scope: ScriptableObject) {
        putConst(scope, MSG_NAME, MessagesScriptable(this))
    }

    fun getString(key: String?): String = with(bundle) {
        return key?.let { if (containsKey(it)) getString(it) else "{$it}" } ?: "{null}"
    }

    private val bundle: ResourceBundle
        get() = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), loader, NonCachingResourceBundleControl.INSTANCE)

    @Throws(URISyntaxException::class)
    private fun enableJsModules(ctx: Context, scope: ScriptableObject): Require = RequireBuilder()
        .setSandboxed(true)
        .setModuleScriptProvider(
            SoftCachingModuleScriptProvider(
                NonCachingModuleSourceProvider(jsModulePath)
            )
        )
        .createRequire(ctx, scope)
        .apply { install(scope) }

    @get:Throws(URISyntaxException::class)
    private val jsModulePath: List<URI?>
        get() = when (loader) {
            is URLClassLoader -> loader.urLs.map { it.toURI() }
            else -> loader.getResource("$MOBILE_INIT_MODULE.js")?.let { listOf(it.toURI()) } ?: emptyList<URI>()
        }

    override fun close() {
        ResourceBundle.clearCache(loader)
    }

    private class NonCachingModuleSourceProvider(privilegedUris: Iterable<URI?>?) : UrlModuleSourceProvider(privilegedUris, null) {
        @Throws(IOException::class)
        override fun openUrlConnection(url: URL): URLConnection = super.openUrlConnection(url).apply { useCaches = false }
    }

    private class NonCachingResourceBundleControl : Control() {

        @Throws(IOException::class, IllegalAccessException::class, InstantiationException::class)
        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? =
            super.newBundle(baseName, locale, format, loader, true)

        companion object {
            val INSTANCE: Control = NonCachingResourceBundleControl()
        }
    }

    companion object {

        private val TAG = JsConfig::class.java.simpleName
        private const val MOBILE_INIT_MODULE = "init"
        private const val BUNDLE_NAME = "strings"
        private const val DB_NAME = "\$db"
        private const val MSG_NAME = "\$msg"

        private fun buildScope(ctx: Context): ScriptableObject {
            return ctx.initSafeStandardObjects()
        }

        private fun putConst(scope: ScriptableObject, name: String, `object`: Any) {
            scope.putConst(name, scope, `object`)
        }

        private fun installUtilityObjects(scope: ScriptableObject) {
            putObjects(scope, DateUtils, IdHelper, FormUtils, StringUtils, SearchUtils)
        }

        private fun installDomainClasses(scope: ScriptableObject) {
            putClasses(scope, LocationHierarchy::class.java, Location::class.java, Individual::class.java)
        }

        private fun installInterfaces(scope: ScriptableObject) {
            putClasses(
                scope, Hierarchy::class.java, NavigatorModule::class.java, FormBuilder::class.java,
                FormConsumer::class.java, Binding::class.java, Launcher::class.java, FormFormatter::class.java,
                FormDisplay::class.java, ItemFormatter::class.java, ItemDetails::class.java,
                DetailsSection::class.java, HierFormatter::class.java, HierItemDisplay::class.java,
                SearchSource::class.java, SearchField::class.java, BooleanClause::class.java, BooleanQuery::class.java,
                TermQuery::class.java, FuzzyQuery::class.java, WildcardQuery::class.java, RegexpQuery::class.java,
                SearchQueryBuilder::class.java, BooleanClause.Occurs::class.java
            )
        }

        private fun putClasses(scope: ScriptableObject, vararg classes: Class<*>) {
            for (c in classes) {
                putClass(scope, c)
            }
        }

        private fun putClass(scope: ScriptableObject, clazz: Class<*>) {
            scope.putConst(clazz.simpleName, scope, NativeJavaClass(scope, clazz))
        }

        private fun putObjects(scope: ScriptableObject, vararg objects: Any) {
            for (o in objects) {
                putConst(scope, o.javaClass.simpleName, o)
            }
        }
    }
}

private inline fun <reified T : Any> Scriptable.getTypedProperty(name: String): T? =
    ScriptableObject.getTypedProperty(this, name, T::class.java)