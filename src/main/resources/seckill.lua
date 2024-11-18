--- 生成于 EmmyLua(https://github.com/EmmyLua)
--- 创建者: 10847
--- 日期时间: 2024/11/7 18:19
---

-- 获取参数
local voucherID = ARGV[1]
local userID = ARGV[2]
local orderID = ARGV[3]

-- 定义键
local stockKey = 'Voucher:stock:' .. voucherID
local orderKey = 'Voucher:ordermanList:' .. voucherID
local streamKey = 'StreamOrder'

-- 检查库存
local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) <= 0 then
    return 1 -- 库存不足
end

-- 检查用户是否已购买
if redis.call('SISMEMBER', orderKey, userID) == 1 then
    return 2 -- 用户已购买
end

-- 减少库存
redis.call('INCRBY', stockKey, -1)

-- 添加用户到购买列表
redis.call('SADD', orderKey, userID)

-- 向流中添加订单信息
redis.call('XADD', streamKey, '*', 'voucherId', voucherID, 'userId', userID, 'id', orderID)

return 0 -- 成功