---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by 10847.
--- DateTime: 2024/11/7 18:19
---
---这个是unlocK的lua脚本，首先判断是否是当前key的值是否等于当前线程值，然后再进行删除
if (redis.call('get',KEYS[1])==ARGV[1]) then
    return redis.call('del',KEYS[1])
end
return 0