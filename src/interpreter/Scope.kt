package interpreter

// Representation of the current scope
// Scopes keep track of their parent scope, allow shadowing of variables defined there,
// and only allow access to variables defined in their parent env at their creation
// We assume variables can't be undefined
data class Scope(val local : MutableMap<String, Value> = mutableMapOf(), val parent : Scope? = null)
    : MutableMap<String, Value> {
    private val bound : Set<String> = parent?.keys ?: emptySet()
    override val entries : MutableSet<MutableMap.MutableEntry<String, Value>>
        get() {
            val entrySet = mutableSetOf<MutableMap.MutableEntry<String, Value>>()
            entrySet.addAll(local.entries)
            parent?.entries?.forEach { if(it.key in bound) entrySet.add(it) }
            return entrySet
        }
    override val keys : MutableSet<String>
        get() {
            val keySet = mutableSetOf<String>()
            keySet.addAll(local.keys)
            keySet.addAll(bound)
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
    override fun containsValue(value : Value) = local.containsValue(value) || bound.any { id -> parent!![id] == value }
    override fun get(key : String) : Value? = local[key] ?: parent?.get(key)?.takeIf { key in bound }
    override fun isEmpty() = local.isEmpty() && bound.isEmpty()
    override fun clear() {
        local.clear()
        parent?.clear()
    }

    override fun put(key : String, value : Value) : Value? {
        val oldValue = this[key]
        if(key in local) {
            local[key] = value
        } else if (parent != null && key in bound) {
            parent[key] = value
        } else {
            local[key] = value
        }
        return oldValue
    }

    override fun remove(key : String) : Value? {
        val oldValue = this[key]
        local.remove(key)
        parent?.remove(key)
        return oldValue
    }

    override fun putAll(from : Map<out String, Value>) = from.forEach { id, value -> this.put(id, value) }

    override fun toString() : String {
        val parentEntries = parent?.entries ?: mutableSetOf()
        parentEntries.removeIf { (id, _) -> id !in bound || id in local.keys }
        parentEntries.addAll(this.local.entries)
        return parentEntries.joinToString(prefix = "{", postfix = "}") { (id, value) ->
            "$id -> $value"
        }
    }
}