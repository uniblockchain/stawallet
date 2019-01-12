import redis.clients.jedis.Jedis

abstract class RedisStorage(val name: String) {
    init {
        val jedis = Jedis(config.getString("storage.redis.server"))
    }
}