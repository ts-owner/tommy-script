package interpreter

data class Scope(private val local : MutableMap<String, Value> = mutableMapOf(), val parent : Scope? = null)
    : MutableMap<String, Value> {
    private val bound : Set<String> = parent?.keys ?: emptySet()
    override val entries : MutableSet<MutableMap.MutableEntry<String, Value>>
        get() {
            val entrySet = mutableSetOf<MutableMap.MutableEntry<String, Value>>()
            entrySet.addAll(local.entries)
            if(parent != null) entrySet.addAll(parent.entries)
            return entrySet
        }
    override val keys : MutableSet<String>
        get() {
            val keySet = mutableSetOf<String>()
            keySet.addAll(local.keys)
            if(parent != null) keySet.addAll(parent.keys)
            return keySet
        }
    override val size
        get() = local.size + bound.size
    override val values : MutableList<Value>
        get() {
            val valueList = mutableListOf<Value>()
            valueList.addAll(local.values)
            for(ident in bound) parent?.let { parent -> valueList.add(parent[ident]!!) }
            return valueList
        }
    override fun containsKey(key : String) = key in local || key in bound
    override fun containsValue(value : Value) = local.containsValue(value) ||
                                                bound.any { id -> parent!![id] == value }
    override fun get(key : String) : Value? = local[key] ?: parent?.get(key)?.takeIf { key in bound }
    override fun isEmpty() = local.isEmpty() && bound.isEmpty()
    override fun clear() = local.clear()
    override fun put(key : String, value : Value) : Value? {
        if(key in local) {
            val oldValue = local[key]!!
            local[key] = value
            return oldValue
        } else if(parent != null && key in parent) {
            val oldValue = parent[key]!!
            parent[key] = value
            return oldValue
        } else {
            local[key] = value
            return null
        }
    }

    override fun remove(key : String) : Value? {
        val oldValue = this[key]
        local.remove(key)
        parent?.remove(key)
        return oldValue
    }

    override fun putAll(from : Map<out String, Value>) = from.forEach { id, value -> this.put(id, value) }
}