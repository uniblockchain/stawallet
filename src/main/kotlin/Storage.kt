import redis.clients.jedis.Jedis

abstract class RedisStorage(val name: String) {

    val jedis: Jedis = Jedis(config.getString("storage.redis.server"))

}