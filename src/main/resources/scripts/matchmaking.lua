local timeControl = KEYS[1]
local userId = ARGV[1]
local elo = tonumber(ARGV[2])

local candidates = redis.call('ZRANGEBYSCORE', timeControl, elo - 200, elo + 200, 'WITHSCORES')

local best
local bestDiff = 201

for i = 1, #candidates, 2 do
    local id = candidates[i]
    local score = tonumber(candidates[i + 1])
    if id ~= userId and math.abs(score - elo) < bestDiff then
        best = id
        bestDiff = math.abs(score - elo)
    end
end

if best then
    redis.call('ZREM', timeControl, best)
    return best
end

redis.call('ZADD', timeControl, elo, userId)
return nil