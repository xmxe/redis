-- 获取redis键
local key = KEYS[1]
-- 获取第一个参数（次数）
local count = tonumber(ARGV[1])
-- 获取第二个参数（时间）
local time = tonumber(ARGV[2])
-- 获取当前流量
local current = redis.call('get', key)
-- 如果current值存在，且值大于规定的次数，则拒绝放行（直接返回当前流量）
if current and tonumber(current) > count then
    return tonumber(current)
end
-- 如果值小于规定次数，或值不存在，则允许放行，当前流量数+1  (值不存在情况下，可以自增变为1)
current = redis.call('incr', key)
-- 如果是第一次进来，那么开始设置键的过期时间。
if tonumber(current) == 1 then
    redis.call('expire', key, time)
end
-- 返回当前流量
return tonumber(current)

-- 首先获取到传进来的key以及限流的count和时间time。
-- 通过get获取到这个key对应的值，这个值就是当前时间窗内这个接口可以访问多少次。
-- 如果是第一次访问，此时拿到的结果为nil，否则拿到的结果应该是一个数字，所以接下来就判断，如果拿到的结果是一个数字，并且这个数字还大于count，那就说明已经超过流量限制了，那么直接返回查询的结果即可。
-- 如果拿到的结果为nil，说明是第一次访问，此时就给当前key自增 1，然后设置一个过期时间。
-- 最后把自增1后的值返回就可以了。