package org.cimsbioko.search

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import org.cimsbioko.App.*
import java.io.IOException
import java.util.*

enum class EntityType(val entityId: String) {
    HIERARCHY(HierarchyItems.COLUMN_HIERARCHY_UUID),
    LOCATION(Locations.COLUMN_LOCATION_UUID),
    INDIVIDUAL(Individuals.COLUMN_INDIVIDUAL_UUID);

    val configName = name.lowercase(Locale.getDefault())
}

class IndexingService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        with(Indexer.instance) {
            if (intent.hasExtra(ENTITY_UUID)) {
                val type: EntityType? = intent.getStringExtra(ENTITY_TYPE)?.let { EntityType.valueOf(it) }
                val uuid: String? = intent.getStringExtra(ENTITY_UUID)
                if (type != null && uuid != null) {
                    try {
                        when (type) {
                            EntityType.HIERARCHY -> reindexHierarchy(uuid)
                            EntityType.LOCATION -> reindexLocation(uuid)
                            EntityType.INDIVIDUAL -> reindexIndividual(uuid)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "failed during reindex", e)
                    }
                } else {
                    Log.w(TAG, "ignored indexing request, missing entity type or uuid")
                }
            } else {
                reindexAll()
            }
        }
    }

    companion object {

        private val TAG = IndexingService::class.java.simpleName
        private const val ENTITY_TYPE = "entityType"
        private const val ENTITY_UUID = "entityUuid"
        private const val JOB_ID = 0xFB

        private fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, IndexingService::class.java, JOB_ID, intent)
        }

        fun queueFullReindex(ctx: Context) {
            Log.i(TAG, "queuing full reindexing")
            enqueueWork(ctx.applicationContext, Intent(ctx, IndexingService::class.java))
        }

        fun queueReindex(ctx: Context, type: EntityType, uuid: String) {
            Log.i(TAG, "queuing entity reindexing: type=$type, uuid=$uuid")
            enqueueWork(ctx.applicationContext, Intent(ctx, IndexingService::class.java).apply {
                putExtra(ENTITY_TYPE, type.toString())
                putExtra(ENTITY_UUID, uuid)
            })
        }
    }
}