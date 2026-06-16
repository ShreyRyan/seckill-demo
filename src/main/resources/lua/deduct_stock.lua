local key = KEYS[1]
local quantity = tonumber(ARGV[1])
local stock = redis.call('get', key)
if stock and tonumber(stock) >= quantity then
    redis.call('decrby', key, quantity)
    return 1
else
    return 0
end
