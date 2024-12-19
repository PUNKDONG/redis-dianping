local vourcherID=ARGV[1];
local UserID=ARGV[2];

local stockKey='seckill:stock'..vourcherID;
local orderKey='sekill:order'.. vourcherID;
if(tonumber(redis.call('get',stockKey))<=0)
then
    return 1
end

if(redis.call('sismember',orderKey,stockKey)==1)then
    return 2
end

redis.call('incrby',stockKey,-1)
redis.call('sadd',orderKey,UserID)

return 0;